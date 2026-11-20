package com.nick22985.nbteditor.modules.world;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class GameRulesTarget extends WorldTarget {
	private CompoundTag loaded = new CompoundTag();

	public GameRulesTarget(ModuleContext ctx) {
		super(ctx);
	}

	@Override
	public String id() {
		return "world:gamerules";
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.WORLD;
	}

	@Override
	public Text displayName() {
		return this.ctx.tr("gamerules");
	}

	@Override
	public CompletableFuture<CompoundTag> load() {
		Optional<WorldBackend> backend = this.backend(TargetCapability.READ);
		if (backend.isEmpty()) {
			return this.failure("no_backend");
		}
		return backend.get().gameRules().thenApply(rules -> {
			CompoundTag tag = new CompoundTag();
			for (Map.Entry<String, String> entry : rules.entrySet()) {
				tag.putString(entry.getKey(), entry.getValue());
			}
			this.loaded = tag.copy();
			return tag;
		});
	}

	@Override
	public CompletableFuture<Void> save(CompoundTag tag) {
		WorldBackend backend = this.writer();
		List<CompletableFuture<Void>> pending = new ArrayList<>();
		CompoundTag snapshot = new CompoundTag();
		for (Map.Entry<String, Tag> entry : tag.entries()) {
			String value = this.value(entry.getValue());
			if (value == null) {
				continue;
			}
			snapshot.putString(entry.getKey(), value);
			if (!value.equals(this.loaded.getString(entry.getKey()))) {
				pending.add(backend.setGameRule(entry.getKey(), value));
			}
		}
		return CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).thenRun(() -> this.loaded = snapshot);
	}

	private String value(Tag tag) {
		if (tag instanceof StringTag) {
			return ((StringTag) tag).value();
		}
		if (tag instanceof NumericTag) {
			return String.valueOf(((NumericTag) tag).number());
		}
		return null;
	}
}
