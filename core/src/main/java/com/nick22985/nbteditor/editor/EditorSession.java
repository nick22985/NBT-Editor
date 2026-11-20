package com.nick22985.nbteditor.editor;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.doc.NbtDocument;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.ClientPlatform;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.Platform;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.TextStyleUtil;
import com.nick22985.nbteditor.ui.Theme;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EditorSession {
	private final Target target;
	private final Platform platform;
	private final GameVersion version;
	private final SnbtDialect dialect;
	private final NbtDocument document;
	private GameItem previewItem;
	private List<Text> previewLines;
	private boolean previewStale = true;
	private boolean saving;
	private boolean saveFailed;

	public EditorSession(Target target, CompoundTag tag, Platform platform) {
		this.target = target;
		this.platform = platform;
		GameVersion dataVersion = target.dataVersion();
		this.version = dataVersion == null ? platform.game().version() : dataVersion;
		this.dialect = this.version.has(Feature.MODERN_SNBT) ? SnbtDialect.MODERN : SnbtDialect.LEGACY;
		this.document = new NbtDocument(tag);
		this.document.addListener(event -> this.previewStale = true);
	}

	public Target target() {
		return this.target;
	}

	public Platform platform() {
		return this.platform;
	}

	public ClientPlatform client() {
		return this.platform.client();
	}

	public GameVersion version() {
		return this.version;
	}

	public SnbtDialect dialect() {
		return this.dialect;
	}

	public NbtDocument document() {
		return this.document;
	}

	public CompoundTag root() {
		Tag root = this.document.root();
		return root instanceof CompoundTag ? (CompoundTag) root : new CompoundTag();
	}

	public SubjectKind kind() {
		return this.target.kind();
	}

	public boolean isItem() {
		return this.target.kind() == SubjectKind.ITEM;
	}

	public boolean writable() {
		return this.target.can(TargetCapability.WRITE);
	}

	public NbtPath dataPath() {
		return this.target.dataPath(this.version);
	}

	public void edit(String label, Consumer<CompoundTag> mutator) {
		this.edit(label, null, mutator);
	}

	public void edit(String label, String coalesceKey, Consumer<CompoundTag> mutator) {
		CompoundTag before = this.root();
		CompoundTag copy = before.copy();
		mutator.accept(copy);
		if (!copy.equals(before)) {
			this.document.batch(label, coalesceKey, () -> this.document.set(NbtPath.ROOT, copy));
		}
	}

	public boolean dirty() {
		return this.saveFailed || this.document.dirty();
	}

	public boolean saving() {
		return this.saving;
	}

	public GameItem previewItem() {
		this.refreshPreview();
		return this.previewItem;
	}

	public List<Text> previewLines() {
		this.refreshPreview();
		return this.previewLines;
	}

	private void refreshPreview() {
		if (!this.previewStale) {
			return;
		}
		this.previewStale = false;
		this.previewItem = null;
		List<Text> lines = new ArrayList<>();
		if (this.target.can(TargetCapability.PREVIEW_ITEM)) {
			try {
				this.previewItem = this.target.previewItem(this.root());
				if (this.previewItem != null) {
					lines.addAll(this.previewItem.tooltip(false));
					for (String error : this.previewItem.errors()) {
						lines.add(TextStyleUtil.colored(error, Theme.RED));
					}
				}
			} catch (RuntimeException e) {
				lines.add(TextStyleUtil.colored(String.valueOf(e.getMessage()), Theme.RED));
			}
		}
		if (lines.isEmpty()) {
			lines.add(this.target.displayName());
			Text source = this.target.source();
			if (source != null && !source.isEmpty()) {
				lines.add(TextStyleUtil.colored(source, Theme.FAINT));
			}
		}
		this.previewLines = lines;
	}

	public CompletableFuture<Void> save() {
		CompoundTag snapshot = this.root().copy();
		this.saving = true;
		this.document.markSaved();
		CompletableFuture<Void> result = new CompletableFuture<>();
		CompletableFuture<Void> pending;
		try {
			pending = this.target.save(snapshot);
		} catch (RuntimeException e) {
			pending = CompletableFuture.failedFuture(e);
		}
		pending.whenComplete((ignored, error) -> this.platform.scheduler().client(() -> {
			this.saving = false;
			this.saveFailed = error != null;
			if (error == null) {
				result.complete(null);
			} else {
				result.completeExceptionally(error);
			}
		}));
		return result;
	}

	public CompletableFuture<Void> reload() {
		CompletableFuture<Void> result = new CompletableFuture<>();
		this.target.load().whenComplete((tag, error) -> this.platform.scheduler().client(() -> {
			if (error == null && tag != null) {
				this.saveFailed = false;
				this.document.reset(tag);
				result.complete(null);
			} else {
				result.completeExceptionally(error == null ? new IllegalStateException("empty") : error);
			}
		}));
		return result;
	}
}
