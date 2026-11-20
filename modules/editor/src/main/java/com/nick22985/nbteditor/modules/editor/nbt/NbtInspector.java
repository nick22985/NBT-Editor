package com.nick22985.nbteditor.modules.editor.nbt;

import com.nick22985.nbteditor.doc.NbtDocument;
import com.nick22985.nbteditor.doc.Selection;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.NbtSuggester;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.nbt.TagType;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.MenuOption;
import com.nick22985.nbteditor.ui.widget.PopoverMenu;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.TagDisplayUtil;
import com.nick22985.nbteditor.util.TextStyleUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtInspector {
	public static final int HEIGHT = 70;
	private static final int PADDING = 5;
	private static final int MAX_FIELD_TEXT = 4096;

	private final NbtPane pane;
	private TextField value;
	private Button typeButton;
	private Button textButton;
	private NbtPath shown;
	private boolean updating;
	private int x;
	private int y;
	private int width;

	public NbtInspector(NbtPane pane) {
		this.pane = pane;
	}

	private NbtDocument document() {
		return this.pane.document();
	}

	private SnbtDialect dialect() {
		return this.pane.session().dialect();
	}

	public void build(EditorFrame frame) {
		this.x = frame.sideX();
		this.y = frame.bodyY();
		this.width = frame.sideWidth();
		int innerX = this.x + PADDING;
		int innerWidth = this.width - PADDING * 2;
		this.value = frame.add(new TextField());
		this.value.formatter(this::format);
		this.value.onChange(this::valueChanged);
		this.value.suggestions(new NbtSuggester(this.pane.session()).values(() -> this.shown));
		this.value.bounds(innerX, this.y + PADDING + 13, innerWidth, Theme.FIELD_HEIGHT);
		int half = (innerWidth - Theme.GAP) / 2;
		this.typeButton = frame.add(new Button(Text.EMPTY, this::openTypeMenu));
		this.typeButton.bounds(innerX, this.y + PADDING + 44, half, Theme.BUTTON_HEIGHT);
		this.textButton = frame.add(new Button(this.pane.tr("edit_text"), this.pane.actions()::editAsText));
		this.textButton.bounds(innerX + half + Theme.GAP, this.y + PADDING + 44, innerWidth - half - Theme.GAP, Theme.BUTTON_HEIGHT);
		this.shown = null;
	}

	public void refresh(boolean selectionChanged) {
		if (this.value == null) {
			return;
		}
		Selection selection = this.pane.selection();
		NbtPath primary = selection.primary();
		boolean single = selection.size() == 1 && primary != null;
		Tag tag = single ? this.document().get(primary) : null;
		boolean writable = this.pane.session().writable();
		this.shown = tag == null ? null : primary;
		if (selectionChanged || !this.value.focused()) {
			this.updating = true;
			try {
				this.value.error(null);
				if (tag == null) {
					this.value.value("");
					this.value.editable(false);
					this.value.placeholder(selection.size() > 1 ? this.pane.tr("inspector.multiple", selection.size()) : this.pane.tr("inspector.none"));
				} else {
					String text = TagDisplayUtil.editText(tag, this.dialect());
					if (text.length() > MAX_FIELD_TEXT || tag instanceof StringTag && text.indexOf('\n') >= 0) {
						this.value.value("");
						this.value.editable(false);
						this.value.placeholder(this.pane.tr("inspector.use_text"));
					} else {
						this.value.value(text);
						this.value.editable(writable);
						this.value.placeholder(tag instanceof StringTag ? this.pane.tr("inspector.empty_string") : Text.EMPTY);
					}
				}
			} finally {
				this.updating = false;
			}
		}
		this.typeButton.label(tag == null ? this.pane.tr("inspector.type_none") : this.pane.tr("inspector.type", TagDisplayUtil.typeName(tag.type())));
		this.typeButton.enabled(tag != null && writable && this.canChangeType(primary));
		this.textButton.enabled(selection.size() <= 1);
	}

	private boolean canChangeType(NbtPath path) {
		if (path.isRoot()) {
			return false;
		}
		Tag parent = this.document().get(path.parent());
		if (parent == null || parent.type().isArray()) {
			return false;
		}
		return !(parent instanceof ListTag && this.dialect() == SnbtDialect.LEGACY && ((ListTag) parent).size() > 1);
	}

	public void focusValue() {
		if (this.value != null && this.value.editable()) {
			this.value.focus();
			this.value.buffer().selectAll();
		}
	}

	private Text format(String text) {
		Tag tag = this.shown == null ? null : this.document().get(this.shown);
		if (tag instanceof StringTag) {
			return TextStyleUtil.colored(text, Theme.GREEN);
		}
		return this.pane.highlight(text);
	}

	private void valueChanged(String text) {
		if (this.updating || this.shown == null) {
			return;
		}
		NbtPath path = this.shown;
		Tag current = this.document().get(path);
		if (current == null) {
			return;
		}
		Tag parsed;
		try {
			parsed = TagDisplayUtil.parseEdit(text, current, this.dialect());
		} catch (SnbtException e) {
			this.value.error(Text.literal(e.getMessage()));
			return;
		}
		this.value.error(null);
		if (parsed.equals(current)) {
			return;
		}
		this.document().batch(this.pane.label("value"), "value:" + path, () -> this.document().set(path, parsed));
	}

	private void openTypeMenu() {
		NbtPath path = this.shown;
		Tag tag = path == null ? null : this.document().get(path);
		if (tag == null) {
			return;
		}
		List<MenuOption> options = new ArrayList<>();
		for (TagType type : TagDisplayUtil.menuTypes()) {
			options.add(MenuOption.of(TagDisplayUtil.typeName(type), () -> this.changeType(path, type)).chip(TagDisplayUtil.chip(type), TagDisplayUtil.chipColor(type)).checked(type == tag.type()));
		}
		this.pane.frame().screen().openOverlay(new PopoverMenu(this.typeButton.x(), this.typeButton.bottom(), false, options).columns(2));
	}

	private void changeType(NbtPath path, TagType type) {
		Tag tag = this.document().get(path);
		if (tag == null || tag.type() == type) {
			return;
		}
		Tag converted = TagDisplayUtil.convert(tag, type, this.dialect());
		this.document().batch(this.pane.label("type"), () -> this.document().set(path, converted));
	}

	public void render(Canvas canvas) {
		if (this.value == null) {
			return;
		}
		DrawUtil.well(canvas, this.x, this.y, this.width, HEIGHT);
		int innerX = this.x + PADDING;
		int innerY = this.y + PADDING;
		int innerRight = this.x + this.width - PADDING;
		Tag tag = this.shown == null ? null : this.document().get(this.shown);
		if (tag == null) {
			Selection selection = this.pane.selection();
			Text label = selection.size() > 1 ? this.pane.tr("inspector.multiple", selection.size()) : this.pane.tr("inspector.title");
			DrawUtil.textClipped(canvas, label, innerX, innerY + 1, innerRight - innerX, Theme.LABEL);
		} else {
			int chipWidth = DrawUtil.chip(canvas, TagDisplayUtil.chip(tag.type()), innerX, innerY, TagDisplayUtil.chipColor(tag.type()));
			int keyX = innerX + chipWidth + 3;
			Object last = this.shown.last();
			String key = last == null ? "" : last instanceof Integer ? "[" + last + "]" : (String) last;
			DrawUtil.textClipped(canvas, key, keyX, innerY + 1, innerRight - keyX, last instanceof Integer ? Theme.FAINT : Theme.AQUA);
			Text info = tag.type().isContainer() ? TagDisplayUtil.summary(tag) : TagDisplayUtil.typeName(tag.type());
			DrawUtil.textClipped(canvas, info, innerX, innerY + 33, innerRight - innerX, Theme.FAINT);
		}
	}
}
