package com.nick22985.nbteditor.modules.editor;

import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.doc.DocumentEvent;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.text.LegacyTextSyntax;
import com.nick22985.nbteditor.text.Style;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextSyntax;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.ItemSlot;
import com.nick22985.nbteditor.ui.widget.MenuOption;
import com.nick22985.nbteditor.ui.widget.ModalPanel;
import com.nick22985.nbteditor.ui.widget.NumberField;
import com.nick22985.nbteditor.ui.widget.PopoverMenu;
import com.nick22985.nbteditor.ui.widget.RichTextField;
import com.nick22985.nbteditor.ui.widget.TabBar;
import com.nick22985.nbteditor.ui.widget.TabSpec;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ErrorUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import com.nick22985.nbteditor.util.RichTextUtil;
import com.nick22985.nbteditor.util.TextStyleUtil;
import com.nick22985.nbteditor.util.WidgetSizeUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EditorScreen extends UiScreen implements EditorFrame {
	private static final int COUNT_WIDTH = 30;
	private static final int SIDE_GAP = 5;
	private static final long STATUS_MILLIS = 2000;

	private final EditorSession session;
	private final ModuleContext ctx;
	private final List<EditorTab> tabs;
	private final Map<String, EditorPane> panes = new HashMap<>();
	private final Consumer<DocumentEvent> listener = this::documentChanged;
	private String tab;
	private EditorPane pane;
	private int bodyX;
	private int bodyY;
	private int bodyWidth;
	private int bodyHeight;
	private int mainWidth;
	private int sideX;
	private int sideWidth;
	private int footerY;
	private int footerLeft;
	private RichTextField nameField;
	private TextField idField;
	private NumberField countField;
	private Button saveButton;
	private boolean updatingHeader;
	private Text status;
	private int statusColor;
	private long statusUntil;
	private boolean closing;

	public EditorScreen(EditorSession session, List<EditorTab> tabs, String tab, ModuleContext ctx) {
		super(session.target().displayName());
		this.session = session;
		this.ctx = ctx;
		this.tabs = tabs;
		this.tab = tab;
		session.document().addListener(this.listener);
	}

	@Override
	public EditorSession session() {
		return this.session;
	}

	@Override
	public UiScreen screen() {
		return this;
	}

	@Override
	public int bodyX() {
		return this.bodyX;
	}

	@Override
	public int bodyY() {
		return this.bodyY;
	}

	@Override
	public int bodyWidth() {
		return this.bodyWidth;
	}

	@Override
	public int bodyHeight() {
		return this.bodyHeight;
	}

	@Override
	public int mainWidth() {
		return this.mainWidth;
	}

	@Override
	public int sideX() {
		return this.sideX;
	}

	@Override
	public int sideWidth() {
		return this.sideWidth;
	}

	public String tab() {
		return this.tab;
	}

	public EditorPane pane() {
		return this.pane;
	}

	private List<EditorTab> visibleTabs() {
		List<EditorTab> out = new ArrayList<>();
		for (EditorTab candidate : this.tabs) {
			if (candidate.applies(this.session)) {
				out.add(candidate);
			}
		}
		return out;
	}

	private EditorTab currentTab(List<EditorTab> visible) {
		for (EditorTab candidate : visible) {
			if (candidate.id().equals(this.tab) && candidate.unavailable(this.session) == null) {
				return candidate;
			}
		}
		for (EditorTab candidate : visible) {
			if (candidate.unavailable(this.session) == null) {
				return candidate;
			}
		}
		return null;
	}

	@Override
	protected void build() {
		List<EditorTab> visible = this.visibleTabs();
		EditorTab current = this.currentTab(visible);
		this.tab = current == null ? null : current.id();
		this.pane = current == null ? null : this.panes.computeIfAbsent(current.id(), id -> current.create(this.session));
		int tabsY = Theme.PAD + Theme.SLOT + Theme.GAP;
		this.bodyX = Theme.PAD;
		this.bodyY = tabsY + TabBar.HEIGHT + 2 + Theme.GAP;
		this.bodyWidth = this.width - Theme.PAD * 2;
		this.footerY = this.height - Theme.PAD_BOTTOM - Theme.BUTTON_HEIGHT;
		this.bodyHeight = this.footerY - Theme.GAP - 2 - Theme.GAP - this.bodyY;
		boolean side = this.pane != null && this.pane.hasSide();
		this.sideWidth = side ? (int) (this.bodyWidth * 0.36) : 0;
		this.mainWidth = side ? this.bodyWidth - this.sideWidth - SIDE_GAP : this.bodyWidth;
		this.sideX = this.bodyX + this.mainWidth + SIDE_GAP;
		this.footerLeft = Theme.PAD;
		this.buildHeader();
		this.buildTabs(visible, tabsY);
		if (this.pane != null) {
			this.pane.build(this);
		}
		this.refreshHeader();
	}

	private void buildHeader() {
		this.nameField = null;
		this.idField = null;
		this.countField = null;
		int saveX = this.width - Theme.PAD - this.saveWidth();
		this.saveButton = null;
		if (this.session.writable()) {
			this.saveButton = this.add(new Button(this.ctx.tr("save"), () -> this.requestSave()));
			this.saveButton.bounds(saveX, Theme.PAD + 1, this.saveWidth(), Theme.BUTTON_HEIGHT);
		}
		if (this.session.target().can(TargetCapability.PREVIEW_ITEM)) {
			ItemSlot slot = this.add(new ItemSlot(this.session::previewItem));
			slot.bounds(Theme.PAD, Theme.PAD, Theme.SLOT, Theme.SLOT);
		}
		if (!this.session.isItem()) {
			return;
		}
		int fieldsX = Theme.PAD + Theme.SLOT + Theme.GAP;
		int fieldsRight = saveX - Theme.GAP - this.textWidth(this.ctx.tr("unsaved")) - Theme.GAP;
		int available = fieldsRight - fieldsX;
		int nameWidth = (int) (available * 0.54);
		int idWidth = available - nameWidth - COUNT_WIDTH - Theme.GAP * 2;
		int fieldY = Theme.PAD + 1;
		TextSyntax syntax = this.ctx.services().find(TextSyntax.class).orElse(LegacyTextSyntax.INSTANCE);
		this.nameField = this.add(new RichTextField(false));
		this.nameField.defaults(Style.EMPTY.withItalic(true));
		this.nameField.placeholder(this.ctx.tr("header.name"));
		this.nameField.pasteParser(input -> syntax.recognizes(input) ? syntax.parse(input) : null);
		this.nameField.copyFormatter(syntax::stringify);
		this.nameField.onChange(this::nameChanged);
		this.nameField.editable(this.session.writable());
		this.nameField.bounds(fieldsX, fieldY, nameWidth, Theme.FIELD_HEIGHT);
		this.idField = this.add(new TextField());
		this.idField.placeholder(this.ctx.tr("header.id"));
		this.idField.validator(value -> this.session.client().items().isItem(ItemStackUtil.namespaced(value.trim())) ? null : this.ctx.tr("header.unknown_item"));
		this.idField.onChange(this::idChanged);
		this.idField.editable(this.session.writable());
		this.idField.bounds(fieldsX + nameWidth + Theme.GAP, fieldY, idWidth, Theme.FIELD_HEIGHT);
		this.countField = this.add(new NumberField(true, ItemStackUtil.count(this.session.root())));
		this.countField.range(1, Math.max(ItemStackUtil.maxCount(this.session.version()), 127));
		this.countField.onValue(this::countChanged);
		this.countField.editable(this.session.writable());
		this.countField.bounds(fieldsRight - COUNT_WIDTH, fieldY, COUNT_WIDTH, Theme.FIELD_HEIGHT);
	}

	private void buildTabs(List<EditorTab> visible, int tabsY) {
		List<TabSpec> specs = new ArrayList<>();
		int total = 0;
		for (EditorTab candidate : visible) {
			TabSpec spec = TabSpec.of(candidate.id(), candidate.label());
			Text reason = candidate.unavailable(this.session);
			if (reason != null) {
				spec = spec.unavailable(reason);
			}
			specs.add(spec);
			total += this.textWidth(candidate.label()) + 14;
		}
		TabBar bar = this.add(new TabBar(specs, this.tab, spec -> this.switchTab(spec.id())));
		bar.bounds(Theme.PAD, tabsY, this.width - Theme.PAD * 2, TabBar.HEIGHT);
		if (total > bar.width()) {
			bar.more(this.ctx.tr("tab.more"), overflow -> this.openOverflow(overflow, bar));
		}
	}

	private void openOverflow(List<TabSpec> overflow, TabBar bar) {
		List<MenuOption> options = new ArrayList<>();
		for (TabSpec spec : overflow) {
			options.add(MenuOption.of(spec.label(), () -> this.switchTab(spec.id())).enabled(spec.available()).checked(spec.id().equals(this.tab)).tooltip(spec.unavailableReason()));
		}
		this.openOverlay(new PopoverMenu(bar.right(), bar.bottom(), false, options));
	}

	public void switchTab(String id) {
		if (id.equals(this.tab)) {
			return;
		}
		this.tab = id;
		this.rebuild();
	}

	@Override
	protected void afterBuild() {
		if (this.focusedWidget() == null && this.pane != null) {
			this.pane.focusDefault();
		}
	}

	private int textWidth(Text text) {
		return this.host() == null ? 40 : this.host().textWidth(text);
	}

	@Override
	public <T extends Widget> T add(T widget) {
		return super.add(widget);
	}

	@Override
	public Button footerButton(Text label, Runnable onPress) {
		Button button = new Button(label, onPress);
		int buttonWidth = WidgetSizeUtil.buttonWidth(this.host(), label);
		this.add(button).bounds(this.footerLeft, this.footerY, buttonWidth, Theme.BUTTON_HEIGHT);
		this.footerLeft += buttonWidth + 3;
		return button;
	}

	@Override
	public void status(Text status) {
		this.status(status, Theme.LABEL);
	}

	public void status(Text status, int color) {
		this.status = status;
		this.statusColor = color;
		this.statusUntil = this.host() == null ? 0 : this.host().millis() + STATUS_MILLIS;
	}

	private void documentChanged(DocumentEvent event) {
		this.refreshHeader();
		if (this.pane != null) {
			this.pane.documentChanged(event);
		}
	}

	private void refreshHeader() {
		if (this.saveButton != null) {
			this.saveButton.enabled(this.session.dirty() && !this.session.saving());
		}
		if (this.nameField == null) {
			return;
		}
		CompoundTag root = this.session.root();
		this.updatingHeader = true;
		try {
			if (!this.nameField.focused()) {
				Text name = ItemFields.CUSTOM_NAME.get(root, this.session.version());
				Text value = name == null ? Text.EMPTY : name;
				if (!RichTextUtil.same(value, this.nameField.value())) {
					this.nameField.value(value);
				}
			}
			GameItem preview = this.session.previewItem();
			if (preview != null) {
				this.nameField.placeholder(TextStyleUtil.colored(preview.name(), Theme.FIELD_PLACEHOLDER));
			}
			if (!this.idField.focused()) {
				String id = ItemStackUtil.id(root);
				if (!id.equals(this.idField.value())) {
					this.idField.value(id);
				}
			}
			if (!this.countField.focused()) {
				int count = ItemStackUtil.count(root);
				Double current = this.countField.number();
				if (current == null || current.intValue() != count) {
					this.countField.number(count);
				}
			}
		} finally {
			this.updatingHeader = false;
		}
	}

	private void nameChanged() {
		if (this.updatingHeader) {
			return;
		}
		Text value = this.nameField.buffer().length() == 0 ? null : this.nameField.value();
		this.session.edit(this.ctx.key("edit.name"), "header:name", root -> {
			if (value == null) {
				ItemFields.CUSTOM_NAME.remove(root, this.session.version());
			} else {
				ItemFields.CUSTOM_NAME.write(root, this.session.version(), value);
			}
		});
	}

	private void idChanged(String value) {
		if (this.updatingHeader || !this.idField.valid()) {
			return;
		}
		this.session.edit(this.ctx.key("edit.id"), "header:id", root -> ItemStackUtil.setId(root, ItemStackUtil.namespaced(value.trim())));
	}

	private void countChanged(double value) {
		if (this.updatingHeader) {
			return;
		}
		this.session.edit(this.ctx.key("edit.count"), "header:count", root -> ItemStackUtil.setCount(root, (int) value, this.session.version()));
	}

	public void requestSave() {
		this.requestSave(null);
	}

	private void requestSave(Runnable after) {
		if (!this.session.writable() || this.session.saving()) {
			return;
		}
		if (this.session.isItem()) {
			if (ItemStackUtil.isEmpty(this.session.root())) {
				ModalUtil.confirmDanger(this, this.ctx.tr("save.empty.title"), this.ctx.tr("save.empty.message"), this.ctx.tr("save"), () -> this.save(after));
				return;
			}
			GameItem preview = this.session.previewItem();
			if (preview != null && !preview.errors().isEmpty()) {
				ModalUtil.confirmDanger(this, this.ctx.tr("save.invalid.title"), this.ctx.tr("save.invalid.message", preview.errors().size()), this.ctx.tr("save"), () -> this.save(after));
				return;
			}
		}
		this.save(after);
	}

	private int saveWidth() {
		return Math.max(44, WidgetSizeUtil.buttonWidth(this.host(), this.ctx.tr("save")));
	}

	private void save(Runnable after) {
		this.session.save().whenComplete((ignored, error) -> {
			this.refreshHeader();
			if (error == null) {
				this.status(this.ctx.tr("saved"), Theme.GREEN);
				if (after != null) {
					after.run();
				}
			} else {
				ModalUtil.error(this, this.ctx.tr("save.failed"), ErrorUtil.message(error));
			}
		});
		this.refreshHeader();
	}

	private void confirmClose() {
		ModalPanel panel = new ModalPanel(this.ctx.tr("close.title")).warn(Theme.GOLD).message(this.ctx.tr("close.message"));
		panel.closingAction(Text.translatable("gui.cancel"), null);
		panel.dangerAction(this.ctx.tr("close.discard"), () -> {
			panel.close();
			this.forceClose();
		});
		if (this.session.writable()) {
			panel.defaultAction(panel.action(this.ctx.tr("close.save"), () -> {
				panel.close();
				this.requestSave(this::forceClose);
			}));
		}
		this.openOverlay(panel);
	}

	private void forceClose() {
		this.closing = true;
		this.close();
	}

	@Override
	public boolean closeOnEscape() {
		return this.closing || !this.session.dirty();
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.pane != null && this.pane.keyPressed(input)) {
			return true;
		}
		if (input.shortcut(Key.S)) {
			this.requestSave();
			return true;
		}
		if (input.shortcut(Key.Z)) {
			this.session.document().undo();
			return true;
		}
		if (input.shortcut(Key.Y) || input.shiftShortcut(Key.Z)) {
			this.session.document().redo();
			return true;
		}
		if (input.is(Key.ESCAPE) && this.topOverlay() == null && !this.closeOnEscape()) {
			this.confirmClose();
			return true;
		}
		return false;
	}

	@Override
	protected void onTick() {
		if (this.pane != null) {
			this.pane.tick();
		}
	}

	@Override
	public void onRemoved() {
		for (EditorPane open : this.panes.values()) {
			open.removed();
		}
	}

	@Override
	protected void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		int saveX = this.width - Theme.PAD - this.saveWidth();
		boolean slot = this.session.target().can(TargetCapability.PREVIEW_ITEM);
		if (!this.session.isItem()) {
			int titleX = Theme.PAD + 1 + (slot ? Theme.SLOT + Theme.GAP : 0);
			int titleWidth = DrawUtil.textClipped(canvas, this.title(), titleX, Theme.PAD + 5, saveX - titleX - 80, Theme.TEXT);
			Text source = this.session.target().source();
			if (source != null && !source.isEmpty()) {
				int sourceX = titleX + titleWidth + 8;
				DrawUtil.textClipped(canvas, source, sourceX, Theme.PAD + 5, saveX - Theme.GAP - 60 - sourceX, Theme.FAINT);
			}
		}
		Text state = null;
		int stateColor = Theme.GOLD;
		if (!this.session.writable()) {
			state = this.ctx.tr("read_only");
			stateColor = Theme.FAINT;
		} else if (this.session.saving()) {
			state = this.ctx.tr("saving");
			stateColor = Theme.FAINT;
		} else if (this.session.dirty()) {
			state = this.ctx.tr("unsaved");
		}
		if (state != null) {
			int stateRight = this.session.writable() ? saveX - Theme.GAP : this.width - Theme.PAD;
			DrawUtil.text(canvas, state, stateRight - canvas.textWidth(state), Theme.PAD + 5, stateColor);
		}
		int tabsY = Theme.PAD + Theme.SLOT + Theme.GAP;
		DrawUtil.separator(canvas, 0, tabsY + TabBar.HEIGHT, this.width);
		DrawUtil.separator(canvas, 0, this.footerY - Theme.GAP - 2, this.width);
		if (this.pane != null && this.pane.hasSide() && this.pane.showsPreview()) {
			this.renderPreview(canvas);
		}
		if (this.pane != null) {
			this.pane.renderBackground(canvas, mouseX, mouseY, delta);
		} else {
			DrawUtil.text(canvas, this.ctx.tr("no_tabs"), this.bodyX, this.bodyY + 4, Theme.FAINT);
		}
		this.renderFooterText(canvas);
	}

	private void renderPreview(Canvas canvas) {
		int top = this.pane.sideTopHeight();
		int labelY = top > 0 ? this.bodyY + top + 3 : this.bodyY;
		DrawUtil.text(canvas, this.ctx.tr("preview"), this.sideX + 1, labelY, Theme.LABEL);
		int boxY = labelY + 11;
		int maxHeight = this.bodyY + this.bodyHeight - boxY;
		if (maxHeight > 12) {
			DrawUtil.tooltipBox(canvas, this.session.previewLines(), this.sideX, boxY, this.sideWidth, maxHeight);
		}
	}

	private void renderFooterText(Canvas canvas) {
		Text text = null;
		int color = Theme.FAINT;
		if (this.status != null && this.host() != null && this.host().millis() < this.statusUntil) {
			text = this.status;
			color = this.statusColor;
		} else if (this.pane != null) {
			text = this.pane.hint();
		}
		if (text == null) {
			return;
		}
		int right = this.width - Theme.PAD;
		int maxWidth = right - this.footerLeft - Theme.GAP;
		int textWidth = Math.min(canvas.textWidth(text), maxWidth);
		DrawUtil.textClipped(canvas, text, right - textWidth, this.footerY + 4, maxWidth, color);
	}

	@Override
	protected void renderForeground(Canvas canvas, int mouseX, int mouseY, float delta) {
		if (this.pane != null) {
			this.pane.renderForeground(canvas, mouseX, mouseY, delta);
		}
	}
}
