package com.nick22985.nbteditor.util;

import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.suggest.SuggestionSource;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.ColorPicker;
import com.nick22985.nbteditor.ui.widget.MenuOption;
import com.nick22985.nbteditor.ui.widget.ModalPanel;
import com.nick22985.nbteditor.ui.widget.PopoverMenu;
import com.nick22985.nbteditor.ui.widget.SearchList;
import com.nick22985.nbteditor.ui.widget.TextField;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModalUtil {
	private ModalUtil() {
	}

	public static ModalPanel notice(UiScreen screen, Text title, Text message) {
		ModalPanel panel = new ModalPanel(title).message(message);
		panel.defaultAction(panel.closingAction(Text.translatable("gui.done"), null));
		return screen.openOverlay(panel);
	}

	public static ModalPanel error(UiScreen screen, Text title, Text message) {
		ModalPanel panel = new ModalPanel(title).warn(Theme.RED).message(message);
		panel.defaultAction(panel.closingAction(Text.translatable("gui.done"), null));
		return screen.openOverlay(panel);
	}

	public static ModalPanel confirm(UiScreen screen, Text title, Text message, Runnable onConfirm) {
		ModalPanel panel = new ModalPanel(title).message(message);
		panel.closingAction(Text.translatable("gui.cancel"), null);
		panel.defaultAction(panel.closingAction(Text.translatable("gui.yes"), onConfirm));
		return screen.openOverlay(panel);
	}

	public static ModalPanel confirmDanger(UiScreen screen, Text title, Text message, Text action, Runnable onConfirm) {
		ModalPanel panel = new ModalPanel(title).warn(Theme.RED).message(message);
		panel.closingAction(Text.translatable("gui.cancel"), null);
		panel.defaultAction(panel.dangerAction(action, () -> {
			panel.close();
			onConfirm.run();
		}));
		return screen.openOverlay(panel);
	}

	public static ModalPanel input(UiScreen screen, Text title, String initial, Function<String, Text> validator, Consumer<String> onSubmit) {
		return input(screen, title, initial, validator, null, onSubmit);
	}

	public static ModalPanel input(UiScreen screen, Text title, String initial, Function<String, Text> validator, SuggestionSource suggestions, Consumer<String> onSubmit) {
		ModalPanel panel = new ModalPanel(title);
		TextField field = panel.content(new TextField(initial), Theme.FIELD_HEIGHT);
		if (validator != null) {
			field.validator(validator);
		}
		field.suggestions(suggestions);
		panel.closingAction(Text.translatable("gui.cancel"), null);
		Button done = panel.action(Text.translatable("gui.done"), () -> {
			if (field.valid()) {
				panel.close();
				onSubmit.accept(field.value());
			}
		});
		field.onSubmit(value -> done.press());
		panel.defaultAction(done);
		screen.openOverlay(panel);
		screen.setFocus(field);
		field.buffer().selectAll();
		return panel;
	}

	public static ModalPanel color(UiScreen screen, Text title, int initial, IntConsumer onPick) {
		ModalPanel panel = new ModalPanel(title, 220);
		ColorPicker picker = panel.content(new ColorPicker(initial, rgb -> {
		}), ColorPicker.PREFERRED_HEIGHT);
		panel.closingAction(Text.translatable("gui.cancel"), null);
		panel.defaultAction(panel.closingAction(Text.translatable("gui.done"), () -> onPick.accept(picker.rgb())));
		return screen.openOverlay(panel);
	}

	public static PopoverMenu popover(Widget anchor, List<MenuOption> options) {
		UiScreen screen = anchor.screen();
		if (screen == null) {
			return null;
		}
		double[] origin = screen.localize(anchor, 0, 0);
		int screenX = (int) Math.round(anchor.x() - origin[0]);
		int screenY = (int) Math.round(anchor.bottom() - origin[1]);
		return screen.openOverlay(new PopoverMenu(screenX, screenY, false, options));
	}

	public static <T> ModalPanel pick(UiScreen screen, Text title, List<T> values, Function<T, Text> labeler, Function<T, String> detail, Consumer<T> onPick) {
		ModalPanel panel = new ModalPanel(title, 260);
		SearchList<T> list = new SearchList<>(values, labeler, detail, value -> {
			panel.close();
			onPick.accept(value);
		});
		panel.content(list, Math.min(200, Theme.FIELD_HEIGHT + 4 + Math.max(1, values.size()) * SearchList.ROW_HEIGHT + 8));
		panel.closingAction(Text.translatable("gui.cancel"), null);
		screen.openOverlay(panel);
		screen.setFocus(list.search());
		return panel;
	}
}
