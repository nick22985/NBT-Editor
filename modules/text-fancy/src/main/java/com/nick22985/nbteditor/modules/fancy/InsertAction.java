package com.nick22985.nbteditor.modules.fancy;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Content;
import com.nick22985.nbteditor.text.KeybindContent;
import com.nick22985.nbteditor.text.NbtContent;
import com.nick22985.nbteditor.text.ScoreContent;
import com.nick22985.nbteditor.text.SelectorContent;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextSyntax;
import com.nick22985.nbteditor.text.TranslatableContent;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.Dropdown;
import com.nick22985.nbteditor.ui.widget.FormGrid;
import com.nick22985.nbteditor.ui.widget.MenuOption;
import com.nick22985.nbteditor.ui.widget.ModalPanel;
import com.nick22985.nbteditor.ui.widget.RichTextField;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.ModalUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class InsertAction implements RichTextAction {
	private static final List<String> NBT_SOURCES = List.of("entity", "block", "storage");

	private final ModuleContext ctx;
	private final TextSyntax syntax;

	public InsertAction(ModuleContext ctx, TextSyntax syntax) {
		this.ctx = ctx;
		this.syntax = syntax;
	}

	@Override
	public Text label() {
		return this.ctx.tr("insert");
	}

	@Override
	public Text tooltip() {
		return this.ctx.tr("insert.tooltip");
	}

	@Override
	public void run(RichTextField field, Button button) {
		List<MenuOption> options = new ArrayList<>();
		options.add(MenuOption.of(this.ctx.tr("insert.translate"), () -> this.translate(field, button)));
		options.add(MenuOption.of(this.ctx.tr("insert.keybind"), () -> this.keybind(field, button)));
		options.add(MenuOption.of(this.ctx.tr("insert.score"), () -> this.score(field, button)));
		options.add(MenuOption.of(this.ctx.tr("insert.selector"), () -> this.selector(field, button)));
		options.add(MenuOption.of(this.ctx.tr("insert.nbt"), () -> this.nbt(field, button)));
		ModalUtil.popover(button, options);
	}

	private TextField text(FormGrid form, String key, String placeholder) {
		TextField text = form.row(this.ctx.tr(key), new TextField(""));
		text.placeholder(Text.literal(placeholder));
		return text;
	}

	private void translate(RichTextField field, Button button) {
		FormGrid form = new FormGrid();
		TextField key = this.text(form, "insert.translate.key", "item.minecraft.diamond");
		TextField args = this.text(form, "insert.translate.args", "arg one | arg two");
		this.open(field, button, "insert.translate", form, key, () -> {
			if (key.value().isBlank()) {
				return null;
			}
			List<Object> values = new ArrayList<>();
			if (!args.value().isBlank()) {
				for (String arg : args.value().split("\\|")) {
					values.add(this.syntax.parse(arg.trim()));
				}
			}
			return new TranslatableContent(key.value().trim(), null, values);
		});
	}

	private void keybind(RichTextField field, Button button) {
		FormGrid form = new FormGrid();
		TextField key = this.text(form, "insert.keybind.key", "key.jump");
		this.open(field, button, "insert.keybind", form, key, () -> key.value().isBlank() ? null : new KeybindContent(key.value().trim()));
	}

	private void score(RichTextField field, Button button) {
		FormGrid form = new FormGrid();
		TextField name = this.text(form, "insert.score.name", "@s");
		TextField objective = this.text(form, "insert.score.objective", "kills");
		this.open(field, button, "insert.score", form, name, () -> name.value().isBlank() || objective.value().isBlank() ? null : new ScoreContent(name.value().trim(), objective.value().trim()));
	}

	private void selector(RichTextField field, Button button) {
		FormGrid form = new FormGrid();
		TextField pattern = this.text(form, "insert.selector.pattern", "@p");
		this.open(field, button, "insert.selector", form, pattern, () -> pattern.value().isBlank() ? null : new SelectorContent(pattern.value().trim(), null));
	}

	private void nbt(RichTextField field, Button button) {
		FormGrid form = new FormGrid();
		Dropdown<String> kind = form.row(this.ctx.tr("insert.nbt.kind"), new Dropdown<>(NBT_SOURCES, NBT_SOURCES.get(0), id -> this.ctx.tr("insert.nbt.kind." + id), id -> {
		}));
		TextField source = this.text(form, "insert.nbt.source", "@s");
		TextField path = this.text(form, "insert.nbt.path", "SelectedItem.id");
		Checkbox interpret = form.row(null, new Checkbox(this.ctx.tr("insert.nbt.interpret"), false, value -> {
		}));
		this.open(field, button, "insert.nbt", form, source, () -> source.value().isBlank() || path.value().isBlank() ? null : new NbtContent(path.value().trim(), interpret.checked(), null, kind.value(), source.value().trim()));
	}

	private void open(RichTextField field, Button button, String title, FormGrid form, TextField focus, Supplier<Content> build) {
		UiScreen screen = button.screen();
		if (screen == null) {
			return;
		}
		ModalPanel panel = new ModalPanel(this.ctx.tr(title), 260);
		panel.content(form, form.preferredHeight());
		panel.closingAction(Text.translatable("gui.cancel"), field::focus);
		Button done = panel.action(Text.translatable("gui.done"), () -> {
			Content content = build.get();
			if (content == null) {
				return;
			}
			panel.close();
			field.focus();
			field.insert(Text.of(content, field.buffer().typingStyle(), List.of()));
		});
		panel.defaultAction(done);
		screen.openOverlay(panel);
		screen.setFocus(focus);
	}
}
