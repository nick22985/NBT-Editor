package com.nick22985.nbteditor.modules.display;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.data.ModelOption;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.IntTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.StringTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.LegacyTextSyntax;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextSyntax;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.NumberField;
import com.nick22985.nbteditor.ui.widget.RichTextField;
import com.nick22985.nbteditor.ui.widget.RichToolbar;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.RowList;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ItemModelUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.RichTextUtil;
import com.nick22985.nbteditor.util.WidgetSizeUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class DisplayPane extends FormPane {
	private static final int LABEL_WIDTH = 64;
	private static final int MIN_LORE_HEIGHT = 40;
	private static final int MODEL_DATA_RANGE = 1000;
	private static final Style NAME_DEFAULTS = Style.EMPTY.withItalic(true);
	private static final Style LORE_DEFAULTS = Style.EMPTY.withColor(Formatting.DARK_PURPLE).withItalic(true);

	private final ModuleContext ctx;
	private RichToolbar toolbar;
	private RichTextField name;
	private RichTextField lore;
	private Checkbox unbreakable;
	private NumberField modelData;
	private TextField itemModel;
	private NumberField stackSize;
	private boolean itemName;

	public DisplayPane(EditorSession session, ModuleContext ctx) {
		super(session);
		this.ctx = ctx;
	}

	private GameVersion version() {
		return this.session.version();
	}

	private TextSyntax syntax() {
		return this.ctx.services().find(TextSyntax.class).orElse(LegacyTextSyntax.INSTANCE);
	}

	@Override
	protected int headerHeight() {
		return Theme.BUTTON_HEIGHT + Theme.GAP;
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		List<RichTextAction> actions = this.ctx.registry(RichTextAction.REGISTRY).values();
		this.name = this.richField(false);
		this.lore = this.richField(true);
		this.toolbar = frame.add(new RichToolbar(this.name, actions));
		this.toolbar.bounds(frame.bodyX(), frame.bodyY(), frame.mainWidth(), Theme.BUTTON_HEIGHT);
		if (Feature.ITEM_COMPONENTS.in(this.version())) {
			Button toggle = frame.footerButton(this.switchLabel(), null);
			toggle.onPress(pressed -> {
				this.itemName = !this.itemName;
				pressed.label(this.switchLabel());
				this.populate();
			});
		}
	}

	private Text switchLabel() {
		return this.ctx.tr(this.itemName ? "switch.custom_name" : "switch.item_name");
	}

	private RichTextField richField(boolean multiline) {
		RichTextField field = new RichTextField(multiline);
		TextSyntax syntax = this.syntax();
		field.pasteParser(input -> syntax.recognizes(input) ? syntax.parse(input) : null);
		field.copyFormatter(syntax::stringify);
		field.editable(this.session.writable());
		field.onCaret(() -> {
			if (this.toolbar != null && field.focused()) {
				this.toolbar.target(field);
			}
		});
		return field;
	}

	@Override
	protected void rows(List<Row> rows) {
		CompoundTag root = this.session.root();
		GameVersion version = this.version();
		boolean components = Feature.ITEM_COMPONENTS.in(version);
		Text nameValue = (this.itemName ? ItemFields.ITEM_NAME : ItemFields.CUSTOM_NAME).get(root, version);
		this.name.defaults(this.itemName ? Style.EMPTY : NAME_DEFAULTS);
		this.name.placeholder(this.ctx.tr(this.itemName ? "item_name.placeholder" : "name.placeholder"));
		this.name.value(nameValue == null ? Text.EMPTY : nameValue);
		this.name.onChange(this::nameChanged);
		rows.add(this.field(this.ctx.tr(this.itemName ? "item_name" : "name"), LABEL_WIDTH, this.name));
		List<Text> loreValue = ItemFields.LORE.get(root, version);
		this.lore.defaults(LORE_DEFAULTS);
		this.lore.placeholder(this.ctx.tr("lore.placeholder"));
		this.lore.lines(loreValue == null ? List.of() : loreValue);
		this.lore.onChange(this::loreChanged);
		boolean itemModels = Feature.ITEM_MODEL_COMPONENT.in(version);
		int options = (components ? 3 : 2) + (itemModels ? 1 : 0);
		int loreHeight = Math.max(MIN_LORE_HEIGHT, this.list.height() - this.list.padding() * 2 - (ROW_HEIGHT + RowList.ROW_GAP) * (options + 1));
		Row loreRow = new Row(loreHeight);
		Text loreLabel = this.ctx.tr("lore");
		loreRow.decorate((canvas, self) -> DrawUtil.textClipped(canvas, loreLabel, self.x() + 4, self.y() + 5, LABEL_WIDTH - 8, Theme.TEXT));
		loreRow.place(this.lore, LABEL_WIDTH, 0, this.rowWidth() - LABEL_WIDTH - 2, loreHeight);
		rows.add(loreRow);
		Boolean unbreakableValue = ItemFields.UNBREAKABLE.get(root, version);
		this.unbreakable = new Checkbox(this.ctx.tr("unbreakable"), unbreakableValue != null && unbreakableValue, this::unbreakableChanged);
		this.unbreakable.enabled(this.session.writable());
		Row unbreakableRow = new Row(ROW_HEIGHT);
		unbreakableRow.place(this.unbreakable, LABEL_WIDTH, (ROW_HEIGHT - Theme.BUTTON_HEIGHT) / 2, this.rowWidth() - LABEL_WIDTH - 2, Theme.BUTTON_HEIGHT);
		rows.add(unbreakableRow);
		Integer modelValue = ItemFields.CUSTOM_MODEL_DATA.get(root, version);
		this.modelData = this.optionalNumber(modelValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
		this.modelData.onValue(value -> this.edit(this.ctx.key("history.model_data"), "model_data", tag -> ItemFields.CUSTOM_MODEL_DATA.write(tag, version, (int) value)));
		this.modelData.optional(() -> this.edit(this.ctx.key("history.model_data"), "model_data", tag -> ItemFields.CUSTOM_MODEL_DATA.remove(tag, version)));
		rows.add(this.browseRow(this.ctx.tr("model_data"), this.modelData, this::browseModelData));
		if (itemModels) {
			Tag model = ItemStackUtil.component(root, "item_model");
			this.itemModel = new TextField(model instanceof StringTag ? ((StringTag) model).value() : "");
			this.itemModel.placeholder(this.ctx.tr("none"));
			this.itemModel.editable(this.session.writable());
			this.itemModel.onChange(value -> this.edit(this.ctx.key("history.item_model"), "item_model", tag -> this.writeItemModel(tag, value)));
			rows.add(this.browseRow(this.ctx.tr("item_model"), this.itemModel, this::browseItemModel));
		}
		if (components) {
			Tag stack = ItemStackUtil.component(root, "max_stack_size");
			Integer stackValue = stack instanceof NumericTag ? ((NumericTag) stack).intValue() : null;
			this.stackSize = this.optionalNumber(stackValue, 1, 99);
			this.stackSize.onValue(value -> this.edit(this.ctx.key("history.stack_size"), "stack_size", tag -> ItemStackUtil.setComponent(tag, "max_stack_size", IntTag.of((int) value))));
			this.stackSize.optional(() -> this.edit(this.ctx.key("history.stack_size"), "stack_size", tag -> ItemStackUtil.setComponent(tag, "max_stack_size", null)));
			rows.add(this.field(this.ctx.tr("stack_size"), LABEL_WIDTH * 2, this.stackSize));
		}
	}

	private Row browseRow(Text label, Widget field, Runnable onBrowse) {
		Row row = this.labelled(label, LABEL_WIDTH * 2);
		Text browse = this.ctx.tr("browse");
		int buttonWidth = WidgetSizeUtil.buttonWidth(this.frame.screen().host(), browse);
		int fieldWidth = this.rowWidth() - LABEL_WIDTH * 2 - buttonWidth - Theme.GAP - 2;
		row.place(field, LABEL_WIDTH * 2, 1, fieldWidth, ROW_HEIGHT - 2);
		Button button = new Button(browse, onBrowse);
		button.enabled(this.session.writable());
		row.place(button, LABEL_WIDTH * 2 + fieldWidth + Theme.GAP, 1, buttonWidth, ROW_HEIGHT - 2);
		return row;
	}

	private void browseModelData() {
		GameVersion version = this.version();
		CompoundTag root = this.session.root();
		String itemModel = this.itemModelOf(root, version);
		List<ModelOption> found = itemModel == null ? List.of() : ItemModelUtil.customModelData(this.session.client().resources(), itemModel, version);
		boolean stripped = itemModel != null && found.isEmpty();
		if (itemModel == null || stripped) {
			found = ItemModelUtil.customModelData(this.session.client().resources(), ItemStackUtil.id(root), version);
		}
		CompoundTag base = root.copy();
		if (stripped) {
			ItemStackUtil.setComponent(base, ItemModelUtil.ITEM_MODEL, null);
		}
		boolean ranged = found.isEmpty();
		List<ModelOption> options = new ArrayList<>(found);
		for (int i = 1; ranged && i <= MODEL_DATA_RANGE; i++) {
			options.add(new ModelOption(i, ""));
		}
		ModelBrowser<ModelOption> browser = new ModelBrowser<>(this.ctx, this.session.client(), this.ctx.tr("model_data.browse.title"), options, option -> this.ctx.tr("model_data.browse.value", option.value()), ModelOption::model, option -> {
			CompoundTag preview = base.copy();
			ItemFields.CUSTOM_MODEL_DATA.write(preview, version, option.value());
			return preview;
		}, option -> this.edit(this.ctx.key("history.model_data"), tag -> ItemFields.CUSTOM_MODEL_DATA.write(tag, version, option.value())));
		if (stripped) {
			browser.note(this.ctx.tr("model_data.browse.stripped", itemModel));
		} else if (ranged) {
			browser.note(this.ctx.tr("model_data.browse.unknown", MODEL_DATA_RANGE));
		}
		browser.open(this.frame.screen());
	}

	private String itemModelOf(CompoundTag root, GameVersion version) {
		if (!Feature.ITEM_MODEL_COMPONENT.in(version)) {
			return null;
		}
		Tag model = ItemStackUtil.component(root, ItemModelUtil.ITEM_MODEL);
		return model instanceof StringTag && !((StringTag) model).value().isEmpty() ? ((StringTag) model).value() : null;
	}

	private void browseItemModel() {
		CompoundTag root = this.session.root();
		List<String> models = ItemModelUtil.itemModels(this.session.client().resources(), this.version());
		new ModelBrowser<String>(this.ctx, this.session.client(), this.ctx.tr("item_model.browse.title"), models, Text::literal, value -> null, value -> {
			CompoundTag preview = root.copy();
			this.writeItemModel(preview, value);
			return preview;
		}, value -> this.edit(this.ctx.key("history.item_model"), tag -> this.writeItemModel(tag, value))).open(this.frame.screen());
	}

	private void writeItemModel(CompoundTag tag, String value) {
		ItemStackUtil.setComponent(tag, ItemModelUtil.ITEM_MODEL, value.isEmpty() ? null : StringTag.of(value));
	}

	private NumberField optionalNumber(Integer value, int min, int max) {
		NumberField field = new NumberField(true, 0);
		field.range(min, max);
		field.value(value == null ? "" : Integer.toString(value));
		field.placeholder(this.ctx.tr("none"));
		field.editable(this.session.writable());
		return field;
	}

	private void nameChanged() {
		GameVersion version = this.version();
		Text value = this.name.buffer().length() == 0 ? null : this.name.value();
		boolean itemName = this.itemName;
		this.edit(this.ctx.key(itemName ? "history.item_name" : "history.name"), "display.name", tag -> (itemName ? ItemFields.ITEM_NAME : ItemFields.CUSTOM_NAME).write(tag, version, value));
	}

	private void loreChanged() {
		GameVersion version = this.version();
		List<Text> value = this.lore.buffer().length() == 0 ? null : this.lore.lines();
		this.edit(this.ctx.key("history.lore"), "display.lore", tag -> ItemFields.LORE.write(tag, version, value));
	}

	private void unbreakableChanged(boolean value) {
		GameVersion version = this.version();
		this.edit(this.ctx.key("history.unbreakable"), tag -> ItemFields.UNBREAKABLE.write(tag, version, value ? Boolean.TRUE : null));
	}

	@Override
	protected void refresh() {
		if (this.name == null) {
			return;
		}
		CompoundTag root = this.session.root();
		GameVersion version = this.version();
		Text nameValue = (this.itemName ? ItemFields.ITEM_NAME : ItemFields.CUSTOM_NAME).get(root, version);
		if (!this.name.focused() && !RichTextUtil.same(this.name.value(), nameValue == null ? Text.EMPTY : nameValue)) {
			this.populate();
			return;
		}
		List<Text> loreValue = ItemFields.LORE.get(root, version);
		if (!this.lore.focused() && !RichTextUtil.same(this.lore.lines(), loreValue == null ? List.of(Text.EMPTY) : loreValue)) {
			this.populate();
			return;
		}
		Boolean unbreakableValue = ItemFields.UNBREAKABLE.get(root, version);
		this.unbreakable.checked(unbreakableValue != null && unbreakableValue);
		Integer modelValue = ItemFields.CUSTOM_MODEL_DATA.get(root, version);
		if (!this.modelData.focused()) {
			String text = modelValue == null ? "" : Integer.toString(modelValue);
			if (!text.equals(this.modelData.value())) {
				this.modelData.value(text);
			}
		}
		if (this.itemModel != null && !this.itemModel.focused()) {
			Tag model = ItemStackUtil.component(root, "item_model");
			String text = model instanceof StringTag ? ((StringTag) model).value() : "";
			if (!text.equals(this.itemModel.value())) {
				this.itemModel.value(text);
			}
		}
	}

	@Override
	public void focusDefault() {
		if (this.name != null && this.name.editable()) {
			this.name.focus();
			this.name.buffer().moveTo(this.name.buffer().length(), false);
		}
	}
}
