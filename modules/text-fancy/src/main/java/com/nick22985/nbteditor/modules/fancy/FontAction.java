package com.nick22985.nbteditor.modules.fancy;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.MenuOption;
import com.nick22985.nbteditor.ui.widget.RichTextField;
import com.nick22985.nbteditor.util.ModalUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FontAction implements RichTextAction {
	private static final List<String> FONTS = List.of("minecraft:uniform", "minecraft:alt", "minecraft:illageralt");

	private final ModuleContext ctx;

	public FontAction(ModuleContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public Text label() {
		return this.ctx.tr("font");
	}

	@Override
	public Text tooltip() {
		return this.ctx.tr("font.tooltip");
	}

	@Override
	public void run(RichTextField field, Button button) {
		String current = field.buffer().selectionStyle().font();
		List<MenuOption> options = new ArrayList<>();
		options.add(MenuOption.of(this.ctx.tr("font.default"), () -> this.apply(field, null)).checked(current == null));
		for (String font : FONTS) {
			options.add(MenuOption.of(this.ctx.tr("font." + font.substring(font.indexOf(':') + 1)), () -> this.apply(field, font)).checked(font.equals(current)));
		}
		boolean custom = current != null && !FONTS.contains(current);
		options.add(MenuOption.of(this.ctx.tr("font.custom"), () -> this.custom(field, button, current)).checked(custom));
		ModalUtil.popover(button, options);
	}

	private void custom(RichTextField field, Button button, String current) {
		UiScreen screen = button.screen();
		if (screen == null) {
			return;
		}
		ModalUtil.input(screen, this.ctx.tr("font.custom.title"), Objects.requireNonNullElse(current, "minecraft:"), value -> value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") ? null : this.ctx.tr("font.custom.invalid"), value -> this.apply(field, value));
	}

	private void apply(RichTextField field, String font) {
		field.focus();
		field.style(style -> style.withFont(font));
	}
}
