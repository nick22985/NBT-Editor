package com.nick22985.nbteditor.modules.items;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetSource;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class InventorySource implements TargetSource {
	private static final int SLOTS = 41;

	private final ModuleContext ctx;

	public InventorySource(ModuleContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public String id() {
		return "inventory";
	}

	@Override
	public Text name() {
		return this.ctx.tr("source.inventory");
	}

	@Override
	public SubjectKind kind() {
		return SubjectKind.ITEM;
	}

	@Override
	public boolean available() {
		return this.ctx.client().player().present();
	}

	@Override
	public List<Target> targets() {
		List<Target> targets = new ArrayList<>();
		PlayerService player = this.ctx.client().player();
		if (!player.present()) {
			return targets;
		}
		for (int slot = 0; slot < SLOTS; slot++) {
			if (!player.slot(slot).isEmpty()) {
				targets.add(new SlotItemTarget(this.ctx, slot));
			}
		}
		if (!player.cursor().isEmpty()) {
			targets.add(new CursorItemTarget(this.ctx));
		}
		return targets;
	}
}
