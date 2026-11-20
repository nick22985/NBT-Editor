package com.nick22985.nbteditor.modules.headdb;

import com.nick22985.nbteditor.json.JsonException;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.DataFileUtil;
import com.nick22985.nbteditor.util.JsonUtil;
import com.nick22985.nbteditor.util.UuidUtil;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PlayerHeads {
	public static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
	private static final String FILE = "players.dat";
	private static final String PROFILE = "https://api.mojang.com/users/profiles/minecraft/%s";
	private static final String SESSION = "https://sessionserver.mojang.com/session/minecraft/profile/%s";
	private static final String USER_AGENT = "nbtedit-headdb";
	private static final Duration TIMEOUT = Duration.ofSeconds(15);
	private static final int MAX = 200;

	private final ModuleContext ctx;
	private final Supplier<Path> dir;
	private final Consumer<Runnable> writer;
	private final List<Head> heads = new ArrayList<>();
	private boolean read;

	public PlayerHeads(ModuleContext ctx, Supplier<Path> dir, Consumer<Runnable> writer) {
		this.ctx = ctx;
		this.dir = dir;
		this.writer = writer;
	}

	public synchronized List<Head> heads() {
		this.read();
		return List.copyOf(this.heads);
	}

	public CompletableFuture<Optional<Head>> lookup(String name) {
		if (!NAME.matcher(name).matches()) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
		HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NORMAL).build();
		return this.get(client, String.format(Locale.ROOT, PROFILE, name)).thenCompose(profile -> this.session(client, profile)).thenApply(head -> {
			head.ifPresent(this::add);
			return head;
		});
	}

	private CompletableFuture<Optional<Head>> session(HttpClient client, Map<String, Object> profile) {
		Object id = profile == null ? null : profile.get("id");
		if (!(id instanceof String)) {
			return CompletableFuture.completedFuture(Optional.empty());
		}
		return this.get(client, String.format(Locale.ROOT, SESSION, id)).thenApply(this::head);
	}

	private CompletableFuture<Map<String, Object>> get(HttpClient client, String url) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).header("User-Agent", USER_AGENT).GET().build();
		return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(response -> {
			if (response.statusCode() == 204 || response.statusCode() == 404) {
				return null;
			}
			if (response.statusCode() != 200) {
				throw new CompletionException(new IOException("Mojang returned " + response.statusCode() + " for " + url));
			}
			try {
				return JsonUtil.parseObject(response.body());
			} catch (JsonException e) {
				throw new CompletionException(e);
			}
		});
	}

	private Optional<Head> head(Map<String, Object> profile) {
		if (profile == null) {
			return Optional.empty();
		}
		Object id = profile.get("id");
		Object name = profile.get("name");
		Object properties = profile.get("properties");
		UUID uuid = id instanceof String ? UuidUtil.parse((String) id) : null;
		if (uuid == null || !(name instanceof String) || !(properties instanceof List)) {
			return Optional.empty();
		}
		for (Object property : (List<?>) properties) {
			if (property instanceof Map && "textures".equals(((Map<?, ?>) property).get("name")) && ((Map<?, ?>) property).get("value") instanceof String) {
				return Optional.of(new Head((String) name, uuid, (String) ((Map<?, ?>) property).get("value"), List.of(), HeadCategory.PLAYERS));
			}
		}
		return Optional.empty();
	}

	private synchronized void add(Head head) {
		this.read();
		this.heads.removeIf(each -> each.uuid().equals(head.uuid()));
		this.heads.add(0, head);
		while (this.heads.size() > MAX) {
			this.heads.remove(this.heads.size() - 1);
		}
		ListTag list = new ListTag();
		for (Head each : this.heads) {
			CompoundTag entry = new CompoundTag();
			entry.putString("name", each.name());
			entry.putString("uuid", each.uuid().toString());
			entry.putString("value", each.value());
			list.add(entry);
		}
		CompoundTag root = new CompoundTag();
		root.put("players", list);
		this.writer.accept(() -> {
			try {
				DataFileUtil.write(this.dir.get().resolve(FILE), root);
			} catch (IOException e) {
				this.ctx.log().warn("Could not save player heads", e);
			}
		});
	}

	private void read() {
		if (this.read) {
			return;
		}
		this.read = true;
		try {
			CompoundTag root = DataFileUtil.read(this.dir.get().resolve(FILE));
			ListTag list = root == null ? null : root.getList("players");
			if (list == null) {
				return;
			}
			for (Tag tag : list) {
				if (!(tag instanceof CompoundTag)) {
					continue;
				}
				CompoundTag entry = (CompoundTag) tag;
				UUID uuid = UuidUtil.parse(entry.getString("uuid"));
				String name = entry.getString("name");
				String value = entry.getString("value");
				if (uuid != null && name != null && value != null) {
					this.heads.add(new Head(name, uuid, value, List.of(), HeadCategory.PLAYERS));
				}
			}
		} catch (IOException | RuntimeException e) {
			this.ctx.log().warn("Could not read player heads", e);
		}
	}
}
