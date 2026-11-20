package com.nick22985.nbteditor.modules.clientchest;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.util.DataFileUtil;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ClientChestStore {
	public static final int SIZE = 54;
	private static final String DIR = "client_chest";
	private static final String NAMES = "names.dat";
	private static final String PREFIX = "page_";
	private static final String SUFFIX = ".dat";
	private static final int CACHE_LIMIT = 256;
	private static final long SHUTDOWN_WAIT = 10;

	private final ModuleContext ctx;
	private final ExecutorService reader = Executors.newSingleThreadExecutor(this.threads("nbtedit-client-chest-read"));
	private final ExecutorService writer = Executors.newSingleThreadExecutor(this.threads("nbtedit-client-chest-write"));
	private final Map<Integer, ClientChestPage> cache = new LinkedHashMap<>(16, 0.75F, true);
	private final Map<Integer, CompletableFuture<ClientChestPage>> loading = new ConcurrentHashMap<>();
	private final Map<Integer, CompoundTag> pending = new ConcurrentHashMap<>();
	private final AtomicReference<CompoundTag> pendingNames = new AtomicReference<>();
	private final NavigableSet<Integer> filled = new TreeSet<>();
	private final NavigableMap<Integer, String> names = new TreeMap<>();
	private final Object writeLock = new Object();
	private boolean indexed;
	private boolean stopped;

	public ClientChestStore(ModuleContext ctx) {
		this.ctx = ctx;
	}

	private ThreadFactory threads(String name) {
		return runnable -> {
			Thread thread = new Thread(runnable, name);
			thread.setDaemon(true);
			return thread;
		};
	}

	private Path dir() {
		return DataFileUtil.dataDir(this.ctx.platform().game().gameDir()).resolve(DIR);
	}

	private Path file(int index) {
		return this.dir().resolve(PREFIX + index + SUFFIX);
	}

	private int dataVersion() {
		return this.ctx.client().items().dataVersion();
	}

	private synchronized void index() {
		if (this.indexed) {
			return;
		}
		this.indexed = true;
		Path dir = this.dir();
		if (Files.isDirectory(dir)) {
			try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, PREFIX + "*" + SUFFIX)) {
				for (Path file : files) {
					String name = file.getFileName().toString();
					try {
						this.filled.add(Integer.parseInt(name.substring(PREFIX.length(), name.length() - SUFFIX.length())));
					} catch (NumberFormatException e) {
						this.ctx.log().warn("Ignoring client chest file " + name, e);
					}
				}
			} catch (IOException e) {
				this.ctx.log().warn("Could not list client chest pages", e);
			}
		}
		try {
			CompoundTag root = DataFileUtil.read(dir.resolve(NAMES));
			CompoundTag stored = root == null ? null : root.getCompound("names");
			if (stored != null) {
				for (Map.Entry<String, Tag> entry : stored.entries()) {
					if (entry.getValue() instanceof StringTag) {
						this.names.put(Integer.parseInt(entry.getKey()), ((StringTag) entry.getValue()).value());
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			this.ctx.log().warn("Could not read client chest page names", e);
		}
	}

	public synchronized ClientChestPage cached(int index) {
		return this.cache.get(index);
	}

	public CompletableFuture<ClientChestPage> load(int index) {
		synchronized (this) {
			this.index();
			ClientChestPage cached = this.cache.get(index);
			if (cached != null) {
				return CompletableFuture.completedFuture(cached);
			}
			if (!this.filled.contains(index)) {
				ClientChestPage empty = ClientChestPage.empty(index, this.dataVersion());
				this.remember(empty);
				return CompletableFuture.completedFuture(empty);
			}
		}
		CompletableFuture<ClientChestPage> created = new CompletableFuture<>();
		CompletableFuture<ClientChestPage> existing = this.loading.putIfAbsent(index, created);
		if (existing != null) {
			return existing;
		}
		try {
			this.reader.execute(() -> created.complete(this.loaded(this.read(index))));
		} catch (RejectedExecutionException e) {
			this.loading.remove(index);
			created.completeExceptionally(e);
		}
		return created;
	}

	private synchronized ClientChestPage loaded(ClientChestPage page) {
		this.loading.remove(page.index());
		ClientChestPage current = this.cache.get(page.index());
		if (current != null) {
			return current;
		}
		this.remember(page);
		return page;
	}

	public synchronized void forget(int index) {
		if (!this.pending.containsKey(index)) {
			this.cache.remove(index);
		}
	}

	private void remember(ClientChestPage page) {
		this.cache.put(page.index(), page);
		Iterator<Integer> keys = this.cache.keySet().iterator();
		while (this.cache.size() > CACHE_LIMIT && keys.hasNext()) {
			int key = keys.next();
			if (key != page.index() && !this.pending.containsKey(key)) {
				keys.remove();
			}
		}
	}

	private ClientChestPage read(int index) {
		int current = this.dataVersion();
		try {
			CompoundTag root = DataFileUtil.read(this.file(index));
			if (root == null) {
				return ClientChestPage.empty(index, current);
			}
			int version = root.getInt("DataVersion", 0);
			ListTag list = root.getList("items");
			PageState state = version > current ? PageState.NEWER : version < current ? PageState.UPGRADED : PageState.OK;
			List<CompoundTag> items = new ArrayList<>();
			for (int i = 0; i < SIZE; i++) {
				Tag element = list != null && i < list.size() ? list.get(i) : null;
				CompoundTag item = element instanceof CompoundTag ? (CompoundTag) element : new CompoundTag();
				if (state == PageState.UPGRADED && !item.isEmpty()) {
					item = this.ctx.client().items().upgrade(item, version);
				}
				items.add(item);
			}
			return new ClientChestPage(index, List.copyOf(items), state == PageState.UPGRADED ? current : version, state);
		} catch (IOException | RuntimeException e) {
			this.ctx.log().warn("Could not read client chest page " + (index + 1), e);
			return new ClientChestPage(index, ClientChestPage.empty(index, current).items(), current, PageState.BROKEN);
		}
	}

	public void save(int index, List<CompoundTag> items) {
		List<CompoundTag> copies = new ArrayList<>();
		for (int i = 0; i < SIZE; i++) {
			copies.add(i < items.size() && items.get(i) != null ? items.get(i).copy() : new CompoundTag());
		}
		ClientChestPage page = new ClientChestPage(index, List.copyOf(copies), this.dataVersion(), PageState.OK);
		CompoundTag tag = new CompoundTag();
		synchronized (this) {
			this.index();
			if (this.stopped) {
				return;
			}
			this.remember(page);
			if (page.isEmpty()) {
				this.filled.remove(index);
			} else {
				this.filled.add(index);
				tag.putInt("DataVersion", page.dataVersion());
				tag.put("items", new ListTag(copies));
			}
			this.pending.put(index, tag);
		}
		this.write(() -> this.flush(index));
	}

	private void write(Runnable task) {
		try {
			this.writer.execute(task);
		} catch (RejectedExecutionException e) {
			task.run();
		}
	}

	private void flush(int index) {
		CompoundTag tag = this.pending.get(index);
		if (tag == null) {
			return;
		}
		synchronized (this.writeLock) {
			try {
				if (tag.isEmpty()) {
					Files.deleteIfExists(this.file(index));
				} else {
					DataFileUtil.write(this.file(index), tag);
				}
			} catch (IOException e) {
				this.ctx.log().warn("Could not save client chest page " + (index + 1), e);
			}
		}
		this.pending.remove(index, tag);
	}

	public void clear(Collection<Integer> pages) {
		boolean renamed = false;
		for (int index : pages) {
			this.save(index, List.of());
			synchronized (this) {
				renamed |= this.names.remove(index) != null;
			}
		}
		if (renamed) {
			this.saveNames();
		}
	}

	public synchronized String name(int index) {
		this.index();
		return this.names.get(index);
	}

	public synchronized Integer find(String name) {
		this.index();
		String wanted = name.trim().toLowerCase(Locale.ROOT);
		for (Map.Entry<Integer, String> entry : this.names.entrySet()) {
			if (entry.getValue().toLowerCase(Locale.ROOT).equals(wanted)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public boolean rename(int index, String name) {
		String value = name == null ? "" : name.trim();
		synchronized (this) {
			Integer owner = value.isEmpty() ? null : this.find(value);
			if (owner != null && owner != index) {
				return false;
			}
			if (value.isEmpty()) {
				this.names.remove(index);
			} else {
				this.names.put(index, value);
			}
		}
		this.saveNames();
		return true;
	}

	public synchronized NavigableMap<Integer, String> names() {
		this.index();
		return new TreeMap<>(this.names);
	}

	public synchronized NavigableSet<Integer> filled() {
		this.index();
		return new TreeSet<>(this.filled);
	}

	public synchronized boolean has(int index) {
		this.index();
		return this.filled.contains(index);
	}

	public synchronized int count() {
		this.index();
		return this.filled.size();
	}

	public synchronized boolean interesting(int index) {
		this.index();
		return this.filled.contains(index) || this.names.containsKey(index);
	}

	public synchronized Integer nearby(int from, boolean forward) {
		this.index();
		Integer page = forward ? this.filled.higher(from) : this.filled.lower(from);
		Integer named = forward ? this.names.higherKey(from) : this.names.lowerKey(from);
		if (page == null) {
			return named;
		}
		if (named == null) {
			return page;
		}
		return forward ? Math.min(page, named) : Math.max(page, named);
	}

	public synchronized int last() {
		this.index();
		int last = this.filled.isEmpty() ? -1 : this.filled.last();
		return this.names.isEmpty() ? last : Math.max(last, this.names.lastKey());
	}

	private void saveNames() {
		CompoundTag stored = new CompoundTag();
		synchronized (this) {
			if (this.stopped) {
				return;
			}
			for (Map.Entry<Integer, String> entry : this.names.entrySet()) {
				stored.putString(String.valueOf(entry.getKey()), entry.getValue());
			}
		}
		CompoundTag root = new CompoundTag();
		root.put("names", stored);
		this.pendingNames.set(root);
		this.write(this::flushNames);
	}

	private void flushNames() {
		CompoundTag root = this.pendingNames.getAndSet(null);
		if (root == null) {
			return;
		}
		synchronized (this.writeLock) {
			try {
				DataFileUtil.write(this.dir().resolve(NAMES), root);
			} catch (IOException e) {
				this.ctx.log().warn("Could not save client chest page names", e);
			}
		}
	}

	public void stop() {
		synchronized (this) {
			this.stopped = true;
		}
		this.reader.shutdownNow();
		this.writer.shutdown();
		try {
			this.writer.awaitTermination(SHUTDOWN_WAIT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		for (int index : new ArrayList<>(this.pending.keySet())) {
			this.flush(index);
		}
		this.flushNames();
	}
}
