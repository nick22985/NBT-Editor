package com.nick22985.nbteditor.modules.potion;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.data.PotionContents;
import com.nick22985.nbteditor.data.PotionEffect;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.editor.RegistryPicker;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.ButtonStyle;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.NumberField;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.ColorUtil;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import com.nick22985.nbteditor.util.PotionEffectUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class PotionPane extends FormPane {
	public static final String STEW = "minecraft:suspicious_stew";
	private static final int LABEL_WIDTH = 96;
	private static final int LEVEL_WIDTH = 44;
	private static final int DURATION_WIDTH = 60;
	private static final int ICON = 16;
	private static final int SWATCH = 14;

	private final ModuleContext ctx;
	private final RegistryPicker effects;
	private final RegistryPicker potions;
	private final List<Widget> inputs = new ArrayList<>();

	public PotionPane(EditorSession session, ModuleContext ctx) {
		super(session);
		this.ctx = ctx;
		this.effects = new RegistryPicker(ctx.client().registries(), RegistryService.MOB_EFFECT, ctx.tr("effect.title"), session.writable());
		this.potions = new RegistryPicker(ctx.client().registries(), RegistryService.POTION, ctx.tr("potion.title"), session.writable());
	}

	private GameVersion version() {
		return this.session.version();
	}

	private boolean stew() {
		return STEW.equals(ItemStackUtil.id(this.session.root()));
	}

	private PotionContents contents(CompoundTag item) {
		PotionContents contents = ItemFields.POTION_CONTENTS.get(item, this.version());
		return contents == null ? PotionContents.EMPTY : contents;
	}

	private List<PotionEffect> effectList(CompoundTag item) {
		if (this.stew()) {
			List<PotionEffect> effects = ItemFields.STEW_EFFECTS.get(item, this.version());
			return effects == null ? new ArrayList<>() : new ArrayList<>(effects);
		}
		return new ArrayList<>(this.contents(item).effects());
	}

	private void writeEffects(CompoundTag item, List<PotionEffect> effects) {
		if (this.stew()) {
			ItemFields.STEW_EFFECTS.write(item, this.version(), effects);
		} else {
			ItemFields.POTION_CONTENTS.write(item, this.version(), this.contents(item).withEffects(effects));
		}
	}

	@Override
	protected int headerHeight() {
		return Theme.BUTTON_HEIGHT + Theme.GAP;
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		boolean writable = this.session.writable();
		this.headerButton(this.ctx.tr("add"), this::add).enabled(writable);
		this.headerButton(this.ctx.tr("clear"), () -> this.change("history.clear", item -> this.writeEffects(item, new ArrayList<>()))).enabled(writable);
	}

	@Override
	protected void rows(List<Row> rows) {
		this.inputs.clear();
		CompoundTag root = this.session.root();
		boolean stew = this.stew();
		if (!stew) {
			this.potionRows(rows, this.contents(root));
		}
		rows.add(this.header(this.ctx.tr(stew ? "stew_effects" : "effects")));
		List<PotionEffect> effects = this.effectList(root);
		if (effects.isEmpty()) {
			rows.add(this.note(this.ctx.tr("none"), Theme.FAINT));
			return;
		}
		rows.add(this.columns(stew));
		for (int i = 0; i < effects.size(); i++) {
			rows.add(this.effectRow(i, effects.get(i), stew));
		}
	}

	private void potionRows(List<Row> rows, PotionContents contents) {
		rows.add(this.header(this.ctx.tr("base")));
		Row potion = this.labelled(this.ctx.tr("potion"), LABEL_WIDTH);
		potion.place(this.potions.button(contents.potion(), id -> this.updateContents("history.potion", null, current -> current.withPotion(id))), LABEL_WIDTH, 1, this.rowWidth() - LABEL_WIDTH - ICON - 6, ROW_HEIGHT - 2);
		potion.placeRight(this.iconButton(PixelSprites.CROSS, Theme.RED, this.ctx.tr("potion.clear"), () -> this.updateContents("history.potion", null, current -> current.withPotion(null))), 2, 1, ICON, ROW_HEIGHT - 2);
		rows.add(potion);
		rows.add(this.colorRow(contents.customColor()));
		if (Feature.POTION_CUSTOM_NAME.in(this.version())) {
			TextField name = new TextField(contents.customName() == null ? "" : contents.customName());
			name.placeholder(this.ctx.tr("custom_name.placeholder"));
			name.editable(this.session.writable());
			name.tooltip(this.ctx.tr("custom_name.hint"));
			name.onChange(text -> this.updateContents("history.custom_name", "custom_name", current -> current.withCustomName(text.isBlank() ? null : text.trim())));
			this.inputs.add(name);
			rows.add(this.field(this.ctx.tr("custom_name"), LABEL_WIDTH, name));
		}
	}

	private Row colorRow(Integer color) {
		Row row = this.labelled(this.ctx.tr("color"), LABEL_WIDTH);
		row.decorate((canvas, self) -> {
			if (color != null) {
				DrawUtil.swatch(canvas, self.x() + LABEL_WIDTH, self.y() + (ROW_HEIGHT - SWATCH) / 2, SWATCH, SWATCH, color, false);
			} else {
				DrawUtil.checkerboard(canvas, self.x() + LABEL_WIDTH, self.y() + (ROW_HEIGHT - SWATCH) / 2, SWATCH, SWATCH, 3);
			}
		});
		Button pick = new Button(color == null ? this.ctx.tr("color.default") : Text.literal(ColorUtil.toHex(color)), pressed -> ModalUtil.color(pressed.screen(), this.ctx.tr("color.title"), color == null ? 0x385DC6 : color, rgb -> this.updateContents("history.color", null, current -> current.withCustomColor(rgb))));
		pick.style(ButtonStyle.FLAT);
		pick.enabled(this.session.writable());
		if (color == null) {
			pick.labelColor(Theme.FAINT);
		}
		row.place(pick, LABEL_WIDTH + SWATCH + Theme.GAP, 1, this.rowWidth() - LABEL_WIDTH - SWATCH - ICON - Theme.GAP - 6, ROW_HEIGHT - 2);
		row.placeRight(this.iconButton(PixelSprites.CROSS, Theme.RED, this.ctx.tr("color.clear"), () -> this.updateContents("history.color", null, current -> current.withCustomColor(null))), 2, 1, ICON, ROW_HEIGHT - 2);
		return row;
	}

	private int fieldsWidth(boolean stew) {
		int infinity = Feature.INFINITE_EFFECTS.in(this.version()) ? ICON + Theme.GAP : 0;
		return (stew ? 0 : LEVEL_WIDTH + Theme.GAP) + DURATION_WIDTH + Theme.GAP + infinity + ICON + 2;
	}

	private Row columns(boolean stew) {
		Row row = new Row(Theme.ROW - 2);
		int width = this.rowWidth();
		int fields = this.fieldsWidth(stew);
		Text effect = this.ctx.tr("effect");
		Text level = this.ctx.tr("level");
		Text duration = this.ctx.tr("duration");
		row.decorate((canvas, self) -> {
			int x = self.x() + width - fields;
			DrawUtil.textClipped(canvas, effect, self.x() + 4, self.y() + 1, width - fields - 8, Theme.LABEL);
			if (!stew) {
				DrawUtil.textClipped(canvas, level, x + 1, self.y() + 1, LEVEL_WIDTH, Theme.LABEL);
				x += LEVEL_WIDTH + Theme.GAP;
			}
			DrawUtil.textClipped(canvas, duration, x + 1, self.y() + 1, DURATION_WIDTH, Theme.LABEL);
		});
		return row;
	}

	private Row effectRow(int index, PotionEffect effect, boolean stew) {
		Row row = new Row(stew ? ROW_HEIGHT : ROW_HEIGHT * 2);
		int width = this.rowWidth();
		int fields = this.fieldsWidth(stew);
		row.place(this.effects.button(effect.id(), id -> this.updateEffect(index, "id", e -> e.withId(id))), 0, 1, width - fields - Theme.GAP, ROW_HEIGHT - 2);
		int x = width - fields;
		if (!stew) {
			NumberField level = new NumberField(true, effect.amplifier() + 1);
			level.range(1, Feature.STRING_EFFECT_IDS.in(this.version()) ? 256 : 128);
			level.editable(this.session.writable());
			level.onValue(value -> this.updateEffect(index, "amplifier", e -> e.withAmplifier((int) value - 1)));
			this.inputs.add(level);
			row.place(level, x, 1, LEVEL_WIDTH, ROW_HEIGHT - 2);
			x += LEVEL_WIDTH + Theme.GAP;
		}
		boolean infinite = Feature.INFINITE_EFFECTS.in(this.version());
		TextField duration = new TextField(PotionEffectUtil.formatDuration(effect.duration()));
		duration.editable(this.session.writable());
		duration.validator(text -> PotionEffectUtil.parseDuration(text, infinite) == null ? this.ctx.tr("duration.invalid") : null);
		duration.tooltip(this.ctx.tr(infinite ? "duration.hint.infinite" : "duration.hint"));
		duration.onChange(text -> {
			Integer ticks = PotionEffectUtil.parseDuration(text, infinite);
			if (ticks != null) {
				this.updateEffect(index, "duration", e -> e.withDuration(ticks));
			}
		});
		this.inputs.add(duration);
		row.place(duration, x, 1, DURATION_WIDTH, ROW_HEIGHT - 2);
		x += DURATION_WIDTH + Theme.GAP;
		if (infinite) {
			Button forever = new Button(Text.literal("∞"), () -> duration.value(PotionEffectUtil.formatDuration(PotionEffect.INFINITE)));
			forever.style(ButtonStyle.FLAT).labelColor(Theme.AQUA);
			forever.enabled(this.session.writable());
			forever.tooltip(this.ctx.tr("infinite"));
			row.place(forever, x, 1, ICON, ROW_HEIGHT - 2);
		}
		row.placeRight(this.iconButton(PixelSprites.CROSS, Theme.RED, this.ctx.tr("remove"), () -> this.change("history.remove", item -> {
			List<PotionEffect> effects = this.effectList(item);
			if (index < effects.size()) {
				effects.remove(index);
				this.writeEffects(item, effects);
			}
		})), 2, 1, ICON, ROW_HEIGHT - 2);
		if (!stew) {
			int third = (width - 8) / 3;
			row.place(this.flag("ambient", effect.ambient(), index, PotionEffect::withAmbient), 4, ROW_HEIGHT + 1, third, Theme.BUTTON_HEIGHT);
			row.place(this.flag("particles", effect.showParticles(), index, PotionEffect::withShowParticles), 4 + third, ROW_HEIGHT + 1, third, Theme.BUTTON_HEIGHT);
			row.place(this.flag("icon", effect.showIcon(), index, PotionEffect::withShowIcon), 4 + third * 2, ROW_HEIGHT + 1, third, Theme.BUTTON_HEIGHT);
		}
		return row;
	}

	private Checkbox flag(String key, boolean value, int index, BiFunction<PotionEffect, Boolean, PotionEffect> setter) {
		Checkbox checkbox = new Checkbox(this.ctx.tr("flag." + key), value, checked -> this.updateEffect(index, key, e -> setter.apply(e, checked)));
		checkbox.enabled(this.session.writable());
		checkbox.tooltip(this.ctx.tr("flag." + key + ".hint"));
		return checkbox;
	}

	private void updateContents(String label, String coalesce, UnaryOperator<PotionContents> change) {
		this.edit(this.ctx.key(label), coalesce, item -> ItemFields.POTION_CONTENTS.write(item, this.version(), change.apply(this.contents(item))));
		if (coalesce == null) {
			this.populate();
		}
	}

	private void updateEffect(int index, String part, UnaryOperator<PotionEffect> change) {
		this.edit(this.ctx.key("history.modify"), "effect." + index + "." + part, item -> {
			List<PotionEffect> effects = this.effectList(item);
			if (index < effects.size()) {
				effects.set(index, change.apply(effects.get(index)));
				this.writeEffects(item, effects);
			}
		});
	}

	private void change(String label, Consumer<CompoundTag> mutator) {
		this.edit(this.ctx.key(label), mutator);
		this.populate();
	}

	private void add() {
		this.effects.pick(this.frame.screen(), this.effects.ids(), id -> {
			this.change("history.add", item -> {
				List<PotionEffect> effects = this.effectList(item);
				PotionEffect effect = PotionEffect.create(id);
				effects.add(this.stew() ? effect.withDuration(160) : effect);
				this.writeEffects(item, effects);
			});
			this.list.scrollToBottom();
		});
	}

	@Override
	protected void refresh() {
		for (Widget input : this.inputs) {
			if (input.focused()) {
				return;
			}
		}
		this.populate();
	}
}
