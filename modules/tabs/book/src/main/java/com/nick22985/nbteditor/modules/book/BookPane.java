package com.nick22985.nbteditor.modules.book;

import com.nick22985.nbteditor.data.BookContent;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.LegacyTextSyntax;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextSyntax;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.Dropdown;
import com.nick22985.nbteditor.ui.widget.RichTextField;
import com.nick22985.nbteditor.ui.widget.RichToolbar;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.BookUtil;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.TextSliceUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BookPane extends FormPane {
	private static final int LABEL_WIDTH = 64;
	private static final int PAGE_LINE = 9;
	private static final int PAGE_PADDING = 8;
	private static final int PAGE_PAPER = 0xFFF4EBD0;
	private static final int PAGE_EDGE = 0xFF8A7652;
	private static final int PAGE_INK = 0xFF000000;
	private static final int PAGE_NUMBER = 0xFF6B5B3E;
	private static final int PREVIEW_WIDTH = BookUtil.PAGE_WIDTH + PAGE_PADDING * 2;
	private static final int PREVIEW_HEIGHT = (BookUtil.PAGE_LINES + 2) * PAGE_LINE + PAGE_PADDING * 2;
	private static final int SIDE_BY_SIDE = 300;
	private static final List<Integer> GENERATIONS = List.of(0, 1, 2, 3);

	private final ModuleContext ctx;
	private final List<Widget> inputs = new ArrayList<>();
	private boolean written;
	private RichTextField page;
	private RichToolbar toolbar;
	private Button previous;
	private Button next;
	private Button add;
	private Button remove;
	private int index;
	private BookContent shown;

	public BookPane(EditorSession session, ModuleContext ctx) {
		super(session);
		this.ctx = ctx;
		this.shown = BookContent.empty(BookUtil.written(session.root()));
	}

	@Override
	public void build(EditorFrame frame) {
		this.written = BookUtil.written(this.session.root());
		super.build(frame);
	}

	private GameVersion version() {
		return this.session.version();
	}

	private BookContent content() {
		return BookUtil.read(this.session.root(), this.version());
	}

	private TextSyntax syntax() {
		return this.ctx.services().find(TextSyntax.class).orElse(LegacyTextSyntax.INSTANCE);
	}

	@Override
	protected int headerHeight() {
		return (Theme.BUTTON_HEIGHT + Theme.GAP) * (this.written ? 2 : 1);
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		boolean writable = this.session.writable();
		this.previous = this.headerIconButton(PixelSprites.LEFT, this.ctx.tr("previous"), () -> this.turn(-1));
		this.next = this.headerIconButton(PixelSprites.RIGHT, this.ctx.tr("next"), () -> this.turn(1));
		this.add = this.headerButton(this.ctx.tr("add"), this::addPage);
		this.add.enabled(writable);
		this.remove = this.headerButton(this.ctx.tr("remove"), this::removePage);
		this.remove.enabled(writable);
		this.headerButton(this.ctx.tr(this.written ? "unsign" : "sign"), this::convert).enabled(writable).tooltip(this.ctx.tr(this.written ? "unsign.hint" : "sign.hint"));
		this.page = new RichTextField(true);
		TextSyntax syntax = this.syntax();
		this.page.pasteParser(input -> this.written && syntax.recognizes(input) ? syntax.parse(input) : null);
		this.page.copyFormatter(syntax::stringify);
		this.page.editable(writable);
		this.page.placeholder(this.ctx.tr("page.placeholder"));
		if (!this.written && Feature.ITEM_COMPONENTS.in(this.version())) {
			this.page.maxLength(BookUtil.WRITABLE_PAGE_LENGTH);
		}
		this.page.onChange(this::pageChanged);
		if (this.written) {
			List<RichTextAction> actions = this.ctx.registry(RichTextAction.REGISTRY).values();
			this.toolbar = frame.add(new RichToolbar(this.page, actions));
			this.toolbar.bounds(frame.bodyX(), frame.bodyY() + Theme.BUTTON_HEIGHT + Theme.GAP, frame.mainWidth(), Theme.BUTTON_HEIGHT);
		}
	}

	@Override
	protected void rows(List<Row> rows) {
		this.inputs.clear();
		BookContent content = this.content();
		this.shown = content;
		int pages = content.pages().size();
		this.index = Math.max(0, Math.min(this.index, pages - 1));
		this.updateButtons(pages);
		if (this.written) {
			this.writtenRows(rows, content);
		}
		rows.add(this.header(pages == 0 ? this.ctx.tr("page.none") : this.ctx.tr("page", this.index + 1, pages)));
		this.page.value(pages == 0 ? Text.EMPTY : content.pages().get(this.index));
		this.inputs.add(this.page);
		int width = this.rowWidth();
		int editorHeight = PREVIEW_HEIGHT;
		if (width >= SIDE_BY_SIDE) {
			Row row = new Row(editorHeight + 2);
			row.place(this.page, 0, 1, width - PREVIEW_WIDTH - Theme.GAP * 2, editorHeight);
			row.decorate((canvas, self) -> this.drawPage(canvas, self.x() + self.width() - PREVIEW_WIDTH - 2, self.y() + 1));
			rows.add(row);
		} else {
			Row editor = new Row(editorHeight / 2 + 2);
			editor.place(this.page, 0, 1, width - 2, editorHeight / 2);
			rows.add(editor);
			rows.add(this.header(this.ctx.tr("preview")));
			Row preview = new Row(PREVIEW_HEIGHT + 2);
			preview.decorate((canvas, self) -> this.drawPage(canvas, self.x() + (self.width() - PREVIEW_WIDTH) / 2, self.y() + 1));
			rows.add(preview);
		}
	}

	private void writtenRows(List<Row> rows, BookContent content) {
		boolean writable = this.session.writable();
		TextField title = new TextField(content.title());
		title.editable(writable);
		title.onChange(value -> this.update("history.title", "book.title", current -> current.withTitle(value)));
		this.inputs.add(title);
		title.tooltip(this.ctx.tr("title.hint", BookContent.MAX_TITLE));
		rows.add(this.field(this.ctx.tr("title"), LABEL_WIDTH, title));
		TextField author = new TextField(content.author());
		author.editable(writable);
		author.onChange(value -> this.update("history.author", "book.author", current -> current.withAuthor(value)));
		this.inputs.add(author);
		rows.add(this.field(this.ctx.tr("author"), LABEL_WIDTH, author));
		int generation = Math.max(0, Math.min(content.generation(), BookContent.GENERATIONS - 1));
		Dropdown<Integer> generations = new Dropdown<>(GENERATIONS, generation, value -> this.ctx.tr("generation." + value), value -> this.update("history.generation", null, current -> current.withGeneration(value)));
		generations.enabled(writable);
		rows.add(this.field(this.ctx.tr("generation"), LABEL_WIDTH, generations));
		Checkbox resolved = new Checkbox(this.ctx.tr("resolved"), content.resolved(), checked -> this.update("history.resolved", null, current -> current.withResolved(checked)));
		resolved.enabled(writable);
		resolved.tooltip(this.ctx.tr("resolved.hint"));
		Row resolvedRow = new Row(ROW_HEIGHT);
		resolvedRow.place(resolved, LABEL_WIDTH, (ROW_HEIGHT - Theme.BUTTON_HEIGHT) / 2, this.rowWidth() - LABEL_WIDTH - 2, Theme.BUTTON_HEIGHT);
		rows.add(resolvedRow);
	}

	private void drawPage(Canvas canvas, int x, int y) {
		DrawUtil.rect(canvas, x - 1, y - 1, PREVIEW_WIDTH + 2, PREVIEW_HEIGHT + 2, PAGE_EDGE);
		DrawUtil.rect(canvas, x, y, PREVIEW_WIDTH, PREVIEW_HEIGHT, PAGE_PAPER);
		BookContent content = this.shown;
		int pages = content.pages().size();
		Text number = this.ctx.tr("page.number", this.index + 1, Math.max(pages, 1));
		canvas.text(number, x + PREVIEW_WIDTH - PAGE_PADDING - canvas.textWidth(number), y + PAGE_PADDING, PAGE_NUMBER, false);
		if (pages == 0) {
			return;
		}
		Text value = this.page.focused() ? this.page.value() : content.pages().get(this.index);
		List<Text> lines = new ArrayList<>();
		for (Text line : TextSliceUtil.lines(value)) {
			List<Text> wrapped = canvas.wrap(line, BookUtil.PAGE_WIDTH);
			if (wrapped.isEmpty()) {
				lines.add(Text.EMPTY);
			} else {
				lines.addAll(wrapped);
			}
		}
		int top = y + PAGE_PADDING + PAGE_LINE * 2;
		for (int i = 0; i < lines.size() && i < BookUtil.PAGE_LINES; i++) {
			canvas.text(lines.get(i), x + PAGE_PADDING, top + i * PAGE_LINE, PAGE_INK, false);
		}
		if (lines.size() > BookUtil.PAGE_LINES) {
			DrawUtil.rect(canvas, x + PAGE_PADDING, top + BookUtil.PAGE_LINES * PAGE_LINE, BookUtil.PAGE_WIDTH, 1, Theme.RED);
			DrawUtil.textClipped(canvas, this.ctx.tr("overflow", lines.size() - BookUtil.PAGE_LINES), x + PAGE_PADDING, top + BookUtil.PAGE_LINES * PAGE_LINE + 2, BookUtil.PAGE_WIDTH, Theme.RED);
		}
	}

	private void updateButtons(int pages) {
		boolean writable = this.session.writable();
		this.previous.enabled(this.index > 0);
		this.next.enabled(this.index < pages - 1);
		this.add.enabled(writable && pages < BookUtil.maxPages(this.version()));
		this.remove.enabled(writable && pages > 0);
	}

	private Text pageValue() {
		return this.written ? this.page.value() : Text.literal(this.page.value().plain());
	}

	private void pageChanged() {
		int index = this.index;
		Text value = this.pageValue();
		this.update("history.page", "book.page." + index, current -> {
			List<Text> pages = new ArrayList<>(current.pages());
			while (pages.size() <= index) {
				pages.add(Text.EMPTY);
			}
			pages.set(index, value);
			return current.withPages(pages);
		});
		this.updateButtons(this.shown.pages().size());
	}

	private void turn(int delta) {
		this.index += delta;
		this.populate();
	}

	private void addPage() {
		int at = this.shown.pages().isEmpty() ? 0 : this.index + 1;
		this.update("history.add", null, current -> {
			List<Text> pages = new ArrayList<>(current.pages());
			pages.add(Math.min(at, pages.size()), Text.EMPTY);
			return current.withPages(pages);
		});
		this.index = at;
		this.populate();
		this.page.focus();
	}

	private void removePage() {
		int at = this.index;
		this.update("history.remove", null, current -> {
			List<Text> pages = new ArrayList<>(current.pages());
			if (at < pages.size()) {
				pages.remove(at);
			}
			return current.withPages(pages);
		});
		this.populate();
	}

	private void convert() {
		GameVersion version = this.version();
		this.edit(this.ctx.key(this.written ? "history.unsign" : "history.sign"), item -> BookUtil.convert(item, version));
		this.frame.rebuild();
	}

	private void update(String label, String coalesce, UnaryOperator<BookContent> change) {
		GameVersion version = this.version();
		this.edit(this.ctx.key(label), coalesce, item -> ItemFields.BOOK.write(item, version, change.apply(BookUtil.read(item, version))));
		this.shown = this.content();
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

	@Override
	public void focusDefault() {
		if (this.page != null && this.page.editable()) {
			this.page.focus();
		}
	}
}
