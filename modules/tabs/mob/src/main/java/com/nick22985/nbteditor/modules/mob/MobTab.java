package com.nick22985.nbteditor.modules.mob;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.EntityTrait;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.MobUtil;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class MobTab implements EditorTab {
	public static final String ID = "mob";

	private final ModuleContext ctx;

	public MobTab(ModuleContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public Text label() {
		return this.ctx.tr("tab");
	}

	@Override
	public boolean applies(EditorSession session) {
		SubjectKind kind = session.kind();
		CompoundTag root = session.root();
		GameVersion version = session.version();
		if (kind == SubjectKind.ENTITY) {
			Set<EntityTrait> traits = this.traits(MobUtil.entityId(root, false, version));
			return traits.contains(EntityTrait.MOB) || traits.isEmpty() && MobUtil.looksLikeMob(root);
		}
		if (kind == SubjectKind.ITEM && MobUtil.isSpawnEgg(ItemStackUtil.id(root))) {
			String entity = MobUtil.entityId(root, true, version);
			if (entity == null) {
				return false;
			}
			Set<EntityTrait> traits = this.traits(entity);
			return traits.isEmpty() || traits.contains(EntityTrait.MOB);
		}
		return false;
	}

	private Set<EntityTrait> traits(String entity) {
		return entity == null ? Set.of() : this.ctx.client().registries().entityTraits(entity);
	}

	@Override
	public EditorPane create(EditorSession session) {
		return new MobPane(session, this.ctx);
	}
}
