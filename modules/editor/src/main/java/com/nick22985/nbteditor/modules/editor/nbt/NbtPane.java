package com.nick22985.nbteditor.modules.editor.nbt;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.doc.DocumentEvent;
import com.nick22985.nbteditor.doc.Edit;
import com.nick22985.nbteditor.doc.NbtDocument;
import com.nick22985.nbteditor.doc.Selection;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorPane;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.modules.editor.tree.NbtTreeView;
import com.nick22985.nbteditor.modules.editor.tree.TreeModel;
import com.nick22985.nbteditor.modules.editor.tree.TreeRow;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.Breadcrumbs;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.SnbtHighlightUtil;
import com.nick22985.nbteditor.util.TextStyleUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtPane implements EditorPane {
	private static final int FOLD_WIDTH = 14;
	private static final int TOOLBAR_HEIGHT = 12;

	private final EditorSession session;
	private final ModuleContext ctx;
	private final NbtClipboard clipboard;
	private final Setting<Integer> highlightLimit;
	private final NbtPath base;
	private final TreeModel model = new TreeModel();
	private final Runnable selectionListener = this::selectionChanged;
	private final NbtInspector inspector;
	private final NbtActions actions;
	private final Map<NbtPath, List<Text>> invalid = new HashMap<>();
	private EditorFrame frame;
	private NbtTreeView tree;
	private Breadcrumbs crumbs;
	private TextField search;
	private Button addButton;
	private Button renameButton;
	private Button duplicateButton;
	private Button copyButton;
	private Button pasteButton;
	private Button deleteButton;
	private String query = "";
	private int scroll;
	private boolean dirtyRows = true;
	private boolean listening;

	public NbtPane(EditorSession session, ModuleContext ctx, NbtClipboard clipboard, Setting<Integer> highlightLimit) {
		this.session = session;
		this.ctx = ctx;
		this.clipboard = clipboard;
		this.highlightLimit = highlightLimit;
		this.base = session.dataPath();
		this.model.dialect(session.dialect());
		this.inspector = new NbtInspector(this);
		this.actions = new NbtActions(this);
	}

	public EditorSession session() {
		return this.session;
	}

	public Text highlight(String text) {
		if (text.length() > this.highlightLimit.get()) {
			return Text.literal(text);
		}
		return SnbtHighlightUtil.highlight(text, this.session.dialect());
	}

	public NbtDocument document() {
		return this.session.document();
	}

	public Selection selection() {
		return this.session.document().selection();
	}

	public ModuleContext ctx() {
		return this.ctx;
	}

	public NbtClipboard clipboard() {
		return this.clipboard;
	}

	public NbtPath base() {
		return this.base;
	}

	public TreeModel model() {
		return this.model;
	}

	public EditorFrame frame() {
		return this.frame;
	}

	public NbtTreeView tree() {
		return this.tree;
	}

	public NbtActions actions() {
		return this.actions;
	}

	public NbtInspector inspector() {
		return this.inspector;
	}

	public Text tr(String key, Object... args) {
		return this.ctx.tr("nbt." + key, args);
	}

	public String label(String key) {
		return this.ctx.key("nbt.history." + key);
	}

	public void status(Text text) {
		if (this.frame != null) {
			this.frame.status(text);
		}
	}

	@Override
	public int sideTopHeight() {
		return NbtInspector.HEIGHT;
	}

	@Override
	public void build(EditorFrame frame) {
		if (this.tree != null) {
			this.scroll = this.tree.scroll();
		}
		if (!this.listening) {
			this.selection().addListener(this.selectionListener);
			this.listening = true;
		}
		this.frame = frame;
		int x = frame.bodyX();
		int y = frame.bodyY();
		int width = frame.mainWidth();
		int searchWidth = (int) (width * 0.34);
		int searchX = x + width - searchWidth;
		int foldX = searchX - Theme.GAP - FOLD_WIDTH * 2 - 2;
		this.crumbs = frame.add(new Breadcrumbs());
		this.crumbs.onSelect(this::crumbSelected);
		this.crumbs.bounds(x, y, foldX - Theme.GAP - x, TOOLBAR_HEIGHT);
		Button expand = frame.add(new Button(Text.EMPTY, () -> this.foldAll(true)));
		expand.icon(PixelSprites.PLUS, Theme.TEXT);
		expand.tooltip(this.tr("expand_all"));
		expand.bounds(foldX, y, FOLD_WIDTH, TOOLBAR_HEIGHT);
		Button collapse = frame.add(new Button(Text.EMPTY, () -> this.foldAll(false)));
		collapse.icon(PixelSprites.MINUS, Theme.TEXT);
		collapse.tooltip(this.tr("collapse_all"));
		collapse.bounds(foldX + FOLD_WIDTH + 2, y, FOLD_WIDTH, TOOLBAR_HEIGHT);
		this.search = frame.add(new TextField(this.query));
		this.search.placeholder(this.tr("search"));
		this.search.onChange(this::searchChanged);
		this.search.bounds(searchX, y, searchWidth, TOOLBAR_HEIGHT);
		this.tree = frame.add(new NbtTreeView(this.model, this.selection(), this.session.dialect()));
		this.tree.onToggle(this::toggle);
		this.tree.onActivate(this::activate);
		this.tree.onToggleAll(this::foldAll);
		this.tree.onContext((row, input) -> this.actions.contextMenu(row, input.mouseX(), input.mouseY()));
		this.tree.invalidTooltip(this.invalid::get);
		this.tree.bounds(x, y + TOOLBAR_HEIGHT + 3, width, frame.bodyHeight() - TOOLBAR_HEIGHT - 3);
		this.inspector.build(frame);
		this.addButton = frame.footerButton(this.tr("add"), () -> this.actions.openAddMenu(this.addButton));
		this.renameButton = frame.footerButton(this.tr("rename"), this.actions::rename);
		this.duplicateButton = frame.footerButton(this.tr("duplicate"), this.actions::duplicate);
		this.copyButton = frame.footerButton(this.tr("copy"), this.actions::copy);
		this.pasteButton = frame.footerButton(this.tr("paste"), this.actions::paste);
		this.deleteButton = frame.footerButton(TextStyleUtil.colored(this.tr("delete"), Theme.RED), this.actions::delete);
		this.refreshInvalid();
		this.rebuildRows();
		this.tree.scroll(this.scroll);
		this.selectionChanged();
	}

	@Override
	public void focusDefault() {
		if (this.tree != null) {
			this.tree.focus();
		}
	}

	@Override
	public Text hint() {
		return this.tr("hint");
	}

	public void rebuildRows() {
		this.model.query(this.query);
		this.model.invalid(this.invalid.keySet());
		this.model.build(this.document().root(), this.base);
		this.dirtyRows = false;
		if (this.tree != null) {
			this.tree.emptyText(this.model.searching() ? this.tr("no_matches") : this.tr("empty"));
			this.tree.refreshed();
		}
	}

	private void refreshInvalid() {
		this.invalid.clear();
		if (!this.session.isItem()) {
			return;
		}
		GameItem preview = this.session.previewItem();
		if (preview == null) {
			return;
		}
		for (String error : preview.errors()) {
			int split = error.indexOf(": ");
			if (split <= 0) {
				continue;
			}
			NbtPath path = this.base.child(error.substring(0, split));
			this.invalid.computeIfAbsent(path, key -> new ArrayList<>()).add(TextStyleUtil.colored(error.substring(split + 2), Theme.RED));
		}
	}

	private void searchChanged(String value) {
		if (value.equals(this.query)) {
			return;
		}
		this.query = value;
		this.rebuildRows();
		this.tree.scroll(0);
	}

	private void toggle(TreeRow row) {
		if (this.model.searching()) {
			return;
		}
		this.model.toggle(row.path());
		this.rebuildRows();
	}

	private void activate(TreeRow row) {
		if (row.expandable() && !this.model.searching()) {
			this.toggle(row);
			return;
		}
		this.selection().select(row.path());
		this.inspector.focusValue();
	}

	public void foldAll(boolean expand) {
		NbtPath primary = this.selection().primary();
		if (expand) {
			if (primary != null && this.selection().size() == 1 && this.document().get(primary) != null && this.document().get(primary).type().isContainer()) {
				this.model.expandUnder(this.document().root(), primary);
			} else {
				this.model.expandAll(this.document().root(), this.base);
			}
		} else if (primary != null && this.selection().size() == 1 && this.model.isExpanded(primary)) {
			this.model.collapseUnder(primary);
		} else {
			this.model.collapseAll();
		}
		this.rebuildRows();
		if (primary != null) {
			this.tree.scrollTo(primary);
		}
	}

	public void focusSearch() {
		if (this.search != null) {
			this.search.focus();
			this.search.buffer().selectAll();
		}
	}

	public void reveal(NbtPath path) {
		this.model.reveal(this.base, path);
		this.rebuildRows();
		if (this.tree != null) {
			this.tree.scrollTo(path);
		}
	}

	private void crumbSelected(int index) {
		NbtPath primary = this.selection().primary();
		if (index == 0 || primary == null) {
			this.selection().clear();
			return;
		}
		List<Object> segments = primary.segments().subList(this.base.size(), primary.size());
		NbtPath target = this.base;
		for (int i = 0; i < index && i < segments.size(); i++) {
			Object segment = segments.get(i);
			target = segment instanceof Integer ? target.child((Integer) segment) : target.child((String) segment);
		}
		this.selection().select(target);
		this.tree.scrollTo(target);
	}

	private void selectionChanged() {
		if (this.frame == null) {
			return;
		}
		NbtPath primary = this.selection().primary();
		List<Text> parts = new ArrayList<>();
		parts.add(this.session.isItem() && !this.base.isRoot() ? Text.literal(String.valueOf(this.base.last())) : this.tr("root"));
		if (primary != null && primary.startsWith(this.base)) {
			for (Object segment : primary.segments().subList(this.base.size(), primary.size())) {
				parts.add(Text.literal(segment instanceof Integer ? "[" + segment + "]" : (String) segment));
			}
		}
		this.crumbs.crumbs(parts);
		this.inspector.refresh(true);
		this.refreshButtons();
	}

	private void refreshButtons() {
		boolean writable = this.session.writable();
		boolean any = !this.selection().isEmpty();
		this.addButton.enabled(writable);
		this.renameButton.enabled(writable && this.actions.canRename());
		this.duplicateButton.enabled(writable && any);
		this.copyButton.enabled(any);
		this.pasteButton.enabled(writable);
		this.deleteButton.enabled(writable && any);
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		Set<NbtPath> before = new HashSet<>(this.invalid.keySet());
		this.refreshInvalid();
		boolean rebuild = event.structural() || !before.equals(this.invalid.keySet());
		if (!rebuild) {
			for (Edit edit : event.edits()) {
				if (!this.model.update(this.document().root(), edit.path())) {
					Tag tag = this.document().get(edit.path());
					if (tag != null && this.model.row(edit.path()) != null) {
						rebuild = true;
					}
				}
			}
			if (this.model.searching()) {
				rebuild = true;
			}
		}
		if (rebuild || this.dirtyRows) {
			this.rebuildRows();
		}
		if (this.frame != null) {
			this.inspector.refresh(false);
			this.refreshButtons();
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (this.frame == null) {
			return false;
		}
		if (input.shortcut(Key.F)) {
			this.focusSearch();
			return true;
		}
		if (input.shortcut(Key.C)) {
			this.actions.copy();
			return true;
		}
		if (input.shortcut(Key.X)) {
			this.actions.cut();
			return true;
		}
		if (input.shortcut(Key.V)) {
			this.actions.paste();
			return true;
		}
		if (input.shortcut(Key.D)) {
			this.actions.duplicate();
			return true;
		}
		if (input.shortcut(Key.N) || input.plain(Key.INSERT)) {
			this.actions.openAddMenu(this.addButton);
			return true;
		}
		if (input.shortcut(Key.E)) {
			this.actions.editAsText();
			return true;
		}
		if (input.shortcut(Key.R) || input.plain(Key.F2)) {
			this.actions.rename();
			return true;
		}
		if (input.plain(Key.DELETE) || input.plain(Key.BACKSPACE)) {
			this.actions.delete();
			return true;
		}
		if (input.is(Key.ESCAPE) && this.search.focused() && !this.query.isEmpty()) {
			this.search.value("");
			this.tree.focus();
			return true;
		}
		if (this.tree != null && !this.tree.focused() && this.frame.screen().focusedWidget() == null && this.tree.keyPressed(input)) {
			return true;
		}
		return false;
	}

	@Override
	public void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		this.inspector.render(canvas);
	}

	@Override
	public void removed() {
		this.selection().removeListener(this.selectionListener);
		this.listening = false;
	}
}
