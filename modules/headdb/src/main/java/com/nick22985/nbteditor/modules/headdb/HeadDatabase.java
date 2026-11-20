package com.nick22985.nbteditor.modules.headdb;

import com.nick22985.nbteditor.json.JsonException;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.DataFileUtil;
import com.nick22985.nbteditor.util.JsonUtil;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HeadDatabase {
	private static final String DIR = "headdb";
	private static final String FAVORITES = "favorites.dat";
	private static final String SOURCE = "https://minecraft-heads.com/scripts/api.php?cat=%s&tags=true";
	private static final String USER_AGENT = "nbtedit-headdb";
	private static final Duration TIMEOUT = Duration.ofSeconds(15);
	private static final String TAG_PREFIX = "tag:";
	private static final int MAX_RESPONSE = 64 * 1024 * 1024;

	private final ModuleContext ctx;
	private final HeadDbSettings settings;
	private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "nbtedit-headdb");
		thread.setDaemon(true);
		return thread;
	});
	private final Set<String> favorites = new LinkedHashSet<>();
	private final PlayerHeads players;
	private volatile Map<HeadCategory, List<Head>> heads = Map.of();
	private volatile HeadDbState state = HeadDbState.IDLE;
	private final AtomicInteger version = new AtomicInteger();
	private final AtomicInteger downloaded = new AtomicInteger();
	private final AtomicBoolean offline = new AtomicBoolean();
	private volatile int downloads;
	private CompletableFuture<HeadDbState> loading;
	private boolean favoritesRead;

	public HeadDatabase(ModuleContext ctx, HeadDbSettings settings) {
		this.ctx = ctx;
		this.settings = settings;
		this.players = new PlayerHeads(ctx, this::dir, this::run);
	}

	private Path dir() {
		return DataFileUtil.dataDir(this.ctx.platform().game().gameDir()).resolve(DIR);
	}

	public HeadDbState state() {
		return this.state;
	}

	public int version() {
		return this.version.get();
	}

	public int downloaded() {
		return this.downloaded.get();
	}

	public int downloads() {
		return this.downloads;
	}

	public synchronized CompletableFuture<HeadDbState> load(boolean refresh) {
		if (this.loading != null && !this.loading.isDone()) {
			return this.loading;
		}
		if (!refresh && this.state != HeadDbState.IDLE && this.state != HeadDbState.FAILED) {
			return CompletableFuture.completedFuture(this.state);
		}
		this.state = HeadDbState.LOADING;
		try {
			this.loading = CompletableFuture.supplyAsync(() -> this.readCache(refresh), this.worker).thenCompose(this::download).exceptionally(this::failed);
		} catch (RejectedExecutionException e) {
			this.state = HeadDbState.FAILED;
			this.loading = CompletableFuture.completedFuture(this.state);
		}
		return this.loading;
	}

	private List<HeadCategory> readCache(boolean refresh) {
		Map<HeadCategory, List<Head>> loaded = new EnumMap<>(HeadCategory.class);
		List<HeadCategory> stale = new ArrayList<>();
		for (HeadCategory category : HeadCategory.values()) {
			if (!category.remote()) {
				continue;
			}
			Path file = this.file(category);
			String json = this.cached(category);
			if (json != null) {
				loaded.put(category, this.parse(json, category));
			}
			if (refresh || json == null || !this.fresh(file)) {
				stale.add(category);
			}
		}
		synchronized (this) {
			this.heads = Map.copyOf(loaded);
			this.downloads = stale.size();
			this.downloaded.set(0);
			this.offline.set(false);
		}
		this.version.incrementAndGet();
		return stale;
	}

	private boolean fresh(Path file) {
		try {
			return System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis() < this.settings.refreshMillis();
		} catch (IOException e) {
			return false;
		}
	}

	private CompletableFuture<HeadDbState> download(List<HeadCategory> stale) {
		if (stale.isEmpty()) {
			return CompletableFuture.completedFuture(this.finish());
		}
		HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build();
		List<CompletableFuture<Void>> requests = new ArrayList<>();
		for (HeadCategory category : stale) {
			HttpRequest request = HttpRequest.newBuilder(URI.create(String.format(Locale.ROOT, SOURCE, category.id()))).timeout(TIMEOUT).header("User-Agent", USER_AGENT).GET().build();
			requests.add(client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).handle((response, error) -> {
				this.received(category, response, error);
				return null;
			}));
		}
		return CompletableFuture.allOf(requests.toArray(new CompletableFuture<?>[0])).thenApply(ignored -> this.finish());
	}

	private void received(HeadCategory category, HttpResponse<String> response, Throwable error) {
		try {
			String body = response == null ? null : response.body();
			if (error != null) {
				this.ctx.log().warn("Could not download heads for " + category.id(), error);
				this.offline.set(true);
			} else if (response.statusCode() != 200 || body == null || body.length() > MAX_RESPONSE || !body.trim().startsWith("[")) {
				this.ctx.log().warn("Head database returned " + response.statusCode() + " for " + category.id());
				this.offline.set(true);
			} else {
				List<Head> parsed = this.parse(body, category);
				this.save(category, body);
				synchronized (this) {
					Map<HeadCategory, List<Head>> next = new EnumMap<>(HeadCategory.class);
					next.putAll(this.heads);
					next.put(category, parsed);
					this.heads = Map.copyOf(next);
				}
			}
		} catch (RuntimeException e) {
			this.ctx.log().warn("Could not read heads for " + category.id(), e);
			this.offline.set(true);
		} finally {
			this.downloaded.incrementAndGet();
			this.version.incrementAndGet();
		}
	}

	private void save(HeadCategory category, String body) {
		Path file = this.file(category);
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, body, StandardCharsets.UTF_8);
		} catch (IOException e) {
			this.ctx.log().warn("Could not save heads for " + category.id(), e);
		}
	}

	public void warm() {
		if (this.state != HeadDbState.IDLE) {
			return;
		}
		for (HeadCategory category : HeadCategory.values()) {
			if (category.remote() && Files.isRegularFile(this.file(category))) {
				this.load(false);
				return;
			}
		}
	}

	public boolean empty() {
		return this.heads.values().stream().allMatch(List::isEmpty);
	}

	private HeadDbState finish() {
		this.state = this.empty() ? HeadDbState.FAILED : this.offline.get() ? HeadDbState.OFFLINE : HeadDbState.READY;
		this.version.incrementAndGet();
		return this.state;
	}

	private HeadDbState failed(Throwable error) {
		this.ctx.log().warn("Could not load the head database", error);
		this.offline.set(true);
		return this.finish();
	}

	private Path file(HeadCategory category) {
		return this.dir().resolve(category.id() + ".json");
	}

	private String cached(HeadCategory category) {
		Path file = this.file(category);
		try {
			return Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8) : null;
		} catch (IOException e) {
			this.ctx.log().warn("Could not read cached heads for " + category.id(), e);
			return null;
		}
	}

	private List<Head> parse(String json, HeadCategory category) {
		List<Head> parsed = new ArrayList<>();
		Object root;
		try {
			root = JsonUtil.parse(json);
		} catch (JsonException e) {
			this.ctx.log().warn("Could not parse heads for " + category.id(), e);
			return parsed;
		}
		if (!(root instanceof List)) {
			return parsed;
		}
		for (Object entry : (List<?>) root) {
			if (!(entry instanceof Map)) {
				continue;
			}
			Map<?, ?> object = (Map<?, ?>) entry;
			Object name = object.get("name");
			Object value = object.get("value");
			if (!(name instanceof String) || !(value instanceof String)) {
				continue;
			}
			Object tags = object.get("tags");
			List<String> tagList = tags instanceof String ? this.tags((String) tags) : List.of();
			parsed.add(new Head((String) name, this.uuid(object.get("uuid"), (String) value), (String) value, tagList, category));
		}
		return List.copyOf(parsed);
	}

	private List<String> tags(String raw) {
		List<String> tags = new ArrayList<>();
		for (String tag : raw.split(",")) {
			String trimmed = tag.trim();
			if (!trimmed.isEmpty()) {
				tags.add(trimmed);
			}
		}
		return List.copyOf(tags);
	}

	private UUID uuid(Object raw, String value) {
		if (raw instanceof String) {
			try {
				return UUID.fromString((String) raw);
			} catch (IllegalArgumentException ignored) {
			}
		}
		return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
	}

	public List<Head> heads(HeadCategory category) {
		Map<HeadCategory, List<Head>> current = this.heads;
		if (category == HeadCategory.ALL) {
			List<Head> all = new ArrayList<>();
			all.addAll(this.players.heads());
			for (HeadCategory each : HeadCategory.values()) {
				all.addAll(current.getOrDefault(each, List.of()));
			}
			return all;
		}
		if (category == HeadCategory.PLAYERS) {
			return this.players.heads();
		}
		if (category == HeadCategory.FAVORITES) {
			Set<String> wanted = this.favorites();
			List<Head> found = new ArrayList<>();
			for (Head head : this.heads(HeadCategory.ALL)) {
				if (wanted.contains(head.value())) {
					found.add(head);
				}
			}
			return found;
		}
		return current.getOrDefault(category, List.of());
	}

	public int count(HeadCategory category) {
		return this.heads(category).size();
	}

	public List<Head> search(HeadCategory category, String query) {
		List<Head> heads = this.heads(category);
		String search = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		if (search.isEmpty()) {
			return heads;
		}
		boolean tagOnly = search.startsWith(TAG_PREFIX);
		String term = tagOnly ? search.substring(TAG_PREFIX.length()).trim() : search;
		List<String> words = Arrays.asList(term.split("\\s+"));
		List<Head> found = new ArrayList<>();
		for (Head head : heads) {
			if (this.matches(head, words, tagOnly)) {
				found.add(head);
			}
		}
		return found;
	}

	private boolean matches(Head head, List<String> words, boolean tagOnly) {
		String name = head.name().toLowerCase(Locale.ROOT);
		String tags = String.join(",", head.tags()).toLowerCase(Locale.ROOT);
		for (String word : words) {
			boolean found = tagOnly ? tags.contains(word) : name.contains(word) || tags.contains(word);
			if (!found) {
				return false;
			}
		}
		return true;
	}

	public synchronized Set<String> favorites() {
		this.readFavorites();
		return new LinkedHashSet<>(this.favorites);
	}

	public synchronized boolean favorite(Head head) {
		this.readFavorites();
		return this.favorites.contains(head.value());
	}

	public synchronized boolean toggleFavorite(Head head) {
		this.readFavorites();
		boolean added = this.favorites.add(head.value());
		if (!added) {
			this.favorites.remove(head.value());
		}
		CompoundTag root = new CompoundTag();
		root.put("values", ListTag.ofStrings(this.favorites));
		this.write(root);
		return added;
	}

	public CompletableFuture<Optional<Head>> player(String name) {
		return this.players.lookup(name).whenComplete((head, error) -> this.version.incrementAndGet());
	}

	private void write(CompoundTag root) {
		this.run(() -> {
			try {
				DataFileUtil.write(this.dir().resolve(FAVORITES), root);
			} catch (IOException e) {
				this.ctx.log().warn("Could not save favorite heads", e);
			}
		});
	}

	private void run(Runnable task) {
		try {
			this.worker.execute(task);
		} catch (RejectedExecutionException e) {
			task.run();
		}
	}

	private void readFavorites() {
		if (this.favoritesRead) {
			return;
		}
		this.favoritesRead = true;
		try {
			CompoundTag root = DataFileUtil.read(this.dir().resolve(FAVORITES));
			ListTag values = root == null ? null : root.getList("values");
			if (values != null) {
				for (Tag value : values) {
					if (value instanceof StringTag) {
						this.favorites.add(((StringTag) value).value());
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			this.ctx.log().warn("Could not read favorite heads", e);
		}
	}

	public void stop() {
		this.worker.shutdown();
	}
}
