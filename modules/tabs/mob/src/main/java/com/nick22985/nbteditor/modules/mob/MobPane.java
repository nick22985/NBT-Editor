package com.nick22985.nbteditor.modules.mob;

import com.nick22985.nbteditor.data.DropChanceSlot;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.MobFlag;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.NumberField;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.MobUtil;
import com.nick22985.nbteditor.util.NumberUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class MobPane extends FormPane {
	private static final int LABEL_WIDTH = 64;
	private static final int FIELD_WIDTH = 64;
	private static final int BUTTON_WIDTH = 52;

	private final ModuleContext ctx;
	private final List<NumberField> numbers = new ArrayList<>();

	public MobPane(EditorSession session, ModuleContext ctx) {
		super(session);
		this.ctx = ctx;
	}

	private GameVersion version() {
		return this.session.version();
	}

	private boolean item() {
		return this.session.isItem();
	}

	@Override
	protected void rows(List<Row> rows) {
		this.numbers.clear();
		CompoundTag data = MobUtil.read(this.session.root(), this.item(), this.version());
		if (this.item()) {
			rows.add(this.note(this.ctx.tr("egg.note"), Theme.FAINT));
		}
		rows.add(this.header(this.ctx.tr("drops")));
		for (DropChanceSlot slot : MobUtil.slots(this.version())) {
			rows.add(this.dropRow(data, slot));
		}
		rows.add(this.header(this.ctx.tr("behavior")));
		rows.add(this.healthRow(data));
		MobFlag[] flags = MobFlag.values();
		for (int i = 0; i < flags.length; i += 2) {
			rows.add(this.flagRow(data, flags[i], i + 1 < flags.length ? flags[i + 1] : null));
		}
	}

	private Row dropRow(CompoundTag data, DropChanceSlot slot) {
		GameVersion version = this.version();
		boolean sparse = this.item();
		NumberField field = new NumberField(false, 0);
		field.range(0, MobUtil.MAX_DROP_CHANCE);
		field.step(0.05);
		field.value(NumberUtil.formatFloat(MobUtil.dropChance(data, slot, version)));
		field.editable(this.session.writable());
		field.onValue(value -> this.update("history.drop_chance", "mob.drop." + slot.id(), mob -> MobUtil.setDropChance(mob, slot, version, (float) value, sparse)));
		this.numbers.add(field);
		Row row = this.labelled(this.ctx.tr("slot." + slot.id()), LABEL_WIDTH);
		row.place(field, LABEL_WIDTH, 1, FIELD_WIDTH, ROW_HEIGHT - 2);
		Button reset = new Button(this.ctx.tr("reset"), () -> this.updateAndRefresh("history.drop_chance", mob -> MobUtil.setDropChance(mob, slot, version, MobUtil.DEFAULT_DROP_CHANCE, sparse)));
		reset.enabled(this.session.writable());
		reset.tooltip(this.ctx.tr("reset.hint", NumberUtil.formatFloat(MobUtil.DEFAULT_DROP_CHANCE)));
		row.placeRight(reset, 2, 1, BUTTON_WIDTH, ROW_HEIGHT - 2);
		Button always = new Button(this.ctx.tr("always"), () -> this.updateAndRefresh("history.drop_chance", mob -> MobUtil.setDropChance(mob, slot, version, MobUtil.PRESERVED_DROP_CHANCE, sparse)));
		always.enabled(this.session.writable());
		always.tooltip(this.ctx.tr("always.hint"));
		row.placeRight(always, 2 + BUTTON_WIDTH + Theme.GAP, 1, BUTTON_WIDTH, ROW_HEIGHT - 2);
		int readoutX = LABEL_WIDTH + FIELD_WIDTH + Theme.GAP;
		row.decorate((canvas, self) -> {
			Double chance = field.number();
			if (chance == null) {
				return;
			}
			int width = self.width() - readoutX - (BUTTON_WIDTH + Theme.GAP) * 2 - 4;
			DrawUtil.textClipped(canvas, this.describe(chance), self.x() + readoutX, self.y() + 5, width, this.readoutColor(chance));
		});
		row.tooltip(() -> {
			Double chance = field.number();
			return chance == null ? List.of(this.ctx.tr("drops.hint")) : List.of(this.ctx.tr("drops.hint"), this.describe(chance));
		});
		return row;
	}

	private Text describe(double chance) {
		if (chance <= 0) {
			return this.ctx.tr("chance.never");
		}
		if (chance > 1) {
			return this.ctx.tr("chance.preserved");
		}
		return Text.literal(NumberUtil.formatFloat((float) chance * 100F) + "%");
	}

	private int readoutColor(double chance) {
		if (chance <= 0) {
			return Theme.FAINT;
		}
		return chance > 1 ? Theme.GREEN : Theme.LABEL;
	}

	private Row healthRow(CompoundTag data) {
		Float health = MobUtil.health(data);
		NumberField field = new NumberField(false, 0);
		field.range(0, MobUtil.MAX_HEALTH);
		field.step(1);
		field.value(health == null ? "" : NumberUtil.formatFloat(health));
		field.placeholder(this.ctx.tr("health.default"));
		field.editable(this.session.writable());
		field.onValue(value -> this.update("history.health", "mob.health", mob -> MobUtil.setHealth(mob, (float) value)));
		field.optional(() -> this.update("history.health", "mob.health", mob -> MobUtil.setHealth(mob, null)));
		this.numbers.add(field);
		Row row = this.labelled(this.ctx.tr("health"), LABEL_WIDTH);
		row.place(field, LABEL_WIDTH, 1, FIELD_WIDTH, ROW_HEIGHT - 2);
		row.tooltip(this.ctx.tr("health.hint"));
		return row;
	}

	private Row flagRow(CompoundTag data, MobFlag left, MobFlag right) {
		Row row = new Row(ROW_HEIGHT);
		int half = (this.rowWidth() - 4) / 2;
		int dy = (ROW_HEIGHT - Theme.BUTTON_HEIGHT) / 2;
		row.place(this.flagBox(data, left), 4, dy, half - Theme.GAP, Theme.BUTTON_HEIGHT);
		if (right != null) {
			row.place(this.flagBox(data, right), 4 + half, dy, half - Theme.GAP, Theme.BUTTON_HEIGHT);
		}
		return row;
	}

	private Checkbox flagBox(CompoundTag data, MobFlag flag) {
		boolean sparse = this.item();
		Checkbox checkbox = new Checkbox(this.ctx.tr("flag." + flag.id()), MobUtil.flag(data, flag), checked -> this.update("history.flag", null, mob -> MobUtil.setFlag(mob, flag, checked, sparse)));
		checkbox.enabled(this.session.writable());
		checkbox.tooltip(this.ctx.tr("flag." + flag.id() + ".hint"));
		return checkbox;
	}

	private void update(String label, String coalesce, Consumer<CompoundTag> change) {
		boolean item = this.item();
		GameVersion version = this.version();
		this.edit(this.ctx.key(label), coalesce, root -> {
			CompoundTag data = MobUtil.read(root, item, version);
			change.accept(data);
			MobUtil.write(root, item, version, data);
		});
	}

	private void updateAndRefresh(String label, Consumer<CompoundTag> change) {
		this.update(label, null, change);
		this.populate();
	}

	@Override
	protected void refresh() {
		for (NumberField field : this.numbers) {
			if (field.focused()) {
				return;
			}
		}
		this.populate();
	}
}
