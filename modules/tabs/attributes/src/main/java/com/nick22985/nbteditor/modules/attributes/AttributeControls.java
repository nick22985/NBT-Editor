package com.nick22985.nbteditor.modules.attributes;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.ButtonStyle;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import com.nick22985.nbteditor.util.NumberUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class AttributeControls {
	private final ModuleContext ctx;
	private final boolean writable;

	public AttributeControls(ModuleContext ctx, boolean writable) {
		this.ctx = ctx;
		this.writable = writable;
	}

	private RegistryService registries() {
		return this.ctx.client().registries();
	}

	public boolean known(String id) {
		return this.registries().contains(RegistryService.ATTRIBUTE, id);
	}

	public Text name(String id) {
		return this.known(id) ? this.registries().name(RegistryService.ATTRIBUTE, id) : Text.literal(ItemStackUtil.stripNamespace(id));
	}

	public List<String> ids() {
		List<String> ids = new ArrayList<>(this.registries().ids(RegistryService.ATTRIBUTE));
		ids.sort(Comparator.comparing(id -> this.name(id).plain()));
		return ids;
	}

	public void pick(UiScreen screen, List<String> ids, Consumer<String> onPick) {
		ModalUtil.pick(screen, this.ctx.tr("pick"), ids, this::name, ItemStackUtil::stripNamespace, onPick);
	}

	public Button picker(String current, Consumer<String> onPick) {
		Button button = new Button(this.name(current), pressed -> this.pick(pressed.screen(), this.ids(), id -> {
			pressed.label(this.name(id));
			pressed.labelColor(Theme.TEXT);
			onPick.accept(id);
		}));
		button.style(ButtonStyle.FLAT);
		button.enabled(this.writable);
		if (!this.known(current)) {
			button.labelColor(Theme.RED);
			button.tooltip(this.ctx.tr("unknown", current));
		} else {
			button.tooltip(Text.literal(current));
		}
		return button;
	}

	public TextField amount(double value, DoubleConsumer onValue) {
		TextField field = new TextField(NumberUtil.formatAmount(value));
		field.editable(this.writable);
		field.validator(text -> NumberUtil.parseAmount(text) == null ? this.ctx.tr("amount.invalid") : null);
		field.onChange(text -> {
			Double parsed = NumberUtil.parseAmount(text);
			if (parsed != null) {
				onValue.accept(parsed);
			}
		});
		field.tooltip(this.ctx.tr("amount.hint"));
		return field;
	}

	public Button infinity(TextField amount) {
		Button button = new Button(Text.literal("∞"), () -> {
			Double current = NumberUtil.parseAmount(amount.value());
			amount.value(NumberUtil.formatAmount(current != null && current == Double.POSITIVE_INFINITY ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY));
		});
		button.style(ButtonStyle.FLAT).labelColor(Theme.AQUA);
		button.enabled(this.writable);
		button.tooltip(this.ctx.tr("infinity"));
		return button;
	}

	public Button remove(Runnable onPress) {
		Button button = new Button(Text.EMPTY, onPress).style(ButtonStyle.FLAT).icon(PixelSprites.CROSS, Theme.RED);
		button.enabled(this.writable);
		button.tooltip(this.ctx.tr("remove"));
		return button;
	}
}
