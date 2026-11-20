package com.nick22985.nbteditor.modules.slotunlock;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.hook.SlotRule;
import com.nick22985.nbteditor.module.ModuleContext;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SlotUnlockSettings {
	private final Setting<Boolean> enabled;
	private final Map<SlotRule, Setting<Boolean>> rules = new EnumMap<>(SlotRule.class);
	private final Setting<Integer> permission;

	public SlotUnlockSettings(ModuleContext ctx) {
		this.enabled = ctx.config().bool("enabled", false);
		for (SlotRule rule : SlotRule.values()) {
			this.rules.put(rule, ctx.config().bool(rule.name().toLowerCase(Locale.ROOT), true));
		}
		this.permission = ctx.config().integer("permission_level", 2, 0, 4);
	}

	public boolean enabled() {
		return this.enabled.get();
	}

	public boolean rule(SlotRule rule) {
		return this.enabled() && this.rules.get(rule).get();
	}

	public int mask() {
		int mask = 0;
		for (SlotRule rule : SlotRule.values()) {
			if (this.rule(rule)) {
				mask |= rule.bit();
			}
		}
		return mask;
	}

	public int permission() {
		return this.permission.get();
	}
}
