package com.nick22985.nbteditor.editor;

import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.ButtonStyle;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class RegistryPicker {
	private final RegistryService registries;
	private final String registry;
	private final Text title;
	private final boolean writable;

	public RegistryPicker(RegistryService registries, String registry, Text title, boolean writable) {
		this.registries = registries;
		this.registry = registry;
		this.title = title;
		this.writable = writable;
	}

	public boolean known(String id) {
		return this.registries.contains(this.registry, id);
	}

	public Text name(String id) {
		if (id == null) {
			return Text.translatable("nbtedit.ui.registry.none");
		}
		return this.known(id) ? this.registries.name(this.registry, id) : Text.literal(ItemStackUtil.stripNamespace(id));
	}

	public List<String> ids() {
		List<String> ids = new ArrayList<>(this.registries.ids(this.registry));
		ids.sort(Comparator.comparing(id -> this.name(id).plain()));
		return ids;
	}

	public void pick(UiScreen screen, List<String> ids, Consumer<String> onPick) {
		ModalUtil.pick(screen, this.title, ids, this::name, ItemStackUtil::stripNamespace, onPick);
	}

	public Button button(String current, Consumer<String> onPick) {
		Button button = new Button(this.name(current), pressed -> this.pick(pressed.screen(), this.ids(), id -> {
			pressed.label(this.name(id));
			pressed.labelColor(Theme.TEXT);
			pressed.tooltip(Text.literal(id));
			onPick.accept(id);
		}));
		button.style(ButtonStyle.FLAT).leftAligned(true);
		button.enabled(this.writable);
		if (current == null) {
			button.labelColor(Theme.FAINT);
		} else if (!this.known(current)) {
			button.labelColor(Theme.RED);
			button.tooltip(Text.translatable("nbtedit.ui.registry.unknown", current));
		} else {
			button.tooltip(Text.literal(current));
		}
		return button;
	}
}
