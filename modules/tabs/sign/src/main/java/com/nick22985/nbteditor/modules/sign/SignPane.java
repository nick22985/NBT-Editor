package com.nick22985.nbteditor.modules.sign;

import com.nick22985.nbteditor.data.DyeColor;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.SignText;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.text.LegacyTextSyntax;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextSyntax;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.Dropdown;
import com.nick22985.nbteditor.ui.widget.RichTextField;
import com.nick22985.nbteditor.ui.widget.RichToolbar;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.util.BlockEntityUtil;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.SignUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class SignPane extends FormPane {
	private static final int LABEL_WIDTH = 64;
	private static final int METER_WIDTH = 48;
	private static final int PREVIEW_PADDING = 6;
	private static final int SIGN_BOARD = 0xFF8F7348;
	private static final int SIGN_EDGE = 0xFF5E4A2E;

	private final ModuleContext ctx;
	private final List<RichTextField> lines = new ArrayList<>();
	private RichToolbar toolbar;
	private boolean front = true;
	private SignText shown = SignText.EMPTY;

	public SignPane(EditorSession session, ModuleContext ctx) {
		super(session);
		this.ctx = ctx;
	}

	private GameVersion version() {
		return this.session.version();
	}

	private boolean item() {
		return this.session.isItem();
	}

	private String blockId() {
		return BlockEntityUtil.blockId(this.session.root(), this.item());
	}

	private CompoundTag blockEntity(CompoundTag root) {
		return BlockEntityUtil.read(root, this.item(), this.version());
	}

	private SignText text(CompoundTag root) {
		return SignUtil.read(this.blockEntity(root), this.front, this.version());
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
		this.lines.clear();
		for (int i = 0; i < SignText.LINES; i++) {
			this.lines.add(this.lineField());
		}
		List<RichTextAction> actions = this.ctx.registry(RichTextAction.REGISTRY).values();
		this.toolbar = frame.add(new RichToolbar(this.lines.get(0), actions));
		this.toolbar.bounds(frame.bodyX(), frame.bodyY(), frame.mainWidth(), Theme.BUTTON_HEIGHT);
		if (Feature.SIGN_TWO_SIDES.in(this.version())) {
			Button side = frame.footerButton(this.sideLabel(), null);
			side.onPress(pressed -> {
				this.front = !this.front;
				pressed.label(this.sideLabel());
				this.populate();
			});
		}
	}

	private Text sideLabel() {
		return this.ctx.tr(this.front ? "switch.back" : "switch.front");
	}

	private RichTextField lineField() {
		RichTextField field = new RichTextField(false);
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
		SignText text = this.text(root);
		this.shown = text;
		int max = SignUtil.maxWidth(this.blockId());
		rows.add(this.previewRow(max));
		rows.add(this.header(this.ctx.tr(Feature.SIGN_TWO_SIDES.in(this.version()) ? this.front ? "front" : "back" : "text")));
		for (int i = 0; i < SignText.LINES; i++) {
			int index = i;
			RichTextField field = this.lines.get(i);
			field.value(text.lines().get(i));
			field.placeholder(this.ctx.tr("line.placeholder"));
			field.onChange(() -> this.update("history.line", "sign." + this.front + "." + index, current -> current.withLine(index, field.value())));
			Row row = this.labelled(this.ctx.tr("line", i + 1), LABEL_WIDTH);
			row.place(field, LABEL_WIDTH, 1, this.rowWidth() - LABEL_WIDTH - METER_WIDTH - Theme.GAP - 2, ROW_HEIGHT - 2);
			row.decorate((canvas, self) -> {
				int width = canvas.textWidth(field.value());
				String meter = width + "/" + max;
				DrawUtil.textClipped(canvas, meter, self.x() + self.width() - METER_WIDTH, self.y() + 5, METER_WIDTH, width > max ? Theme.RED : Theme.FAINT);
			});
			row.tooltip(this.ctx.tr("width.hint", max));
			rows.add(row);
		}
		Dropdown<DyeColor> color = new Dropdown<>(Arrays.asList(DyeColor.values()), DyeColor.of(text.color()) == null ? DyeColor.BLACK : DyeColor.of(text.color()), dye -> Text.translatable(dye.translationKey()), dye -> this.update("history.color", null, current -> current.withColor(dye.id())));
		color.columns(2);
		color.enabled(this.session.writable());
		Row colorRow = this.field(this.ctx.tr("color"), LABEL_WIDTH, color);
		rows.add(colorRow);
		rows.add(this.checkRow(this.ctx.tr("glowing"), text.glowing(), checked -> this.update("history.glowing", null, current -> current.withGlowing(checked))));
		if (Feature.SIGN_TWO_SIDES.in(this.version())) {
			rows.add(this.checkRow(this.ctx.tr("waxed"), SignUtil.waxed(this.blockEntity(root)), checked -> this.change("history.waxed", blockEntity -> SignUtil.waxed(blockEntity, checked))));
		}
	}

	private Row previewRow(int max) {
		int lineHeight = Theme.LINE;
		Row row = new Row(SignText.LINES * lineHeight + PREVIEW_PADDING * 2);
		row.decorate((canvas, self) -> {
			int boardWidth = max + PREVIEW_PADDING * 4;
			int x = self.x() + (self.width() - boardWidth) / 2;
			DrawUtil.rect(canvas, x - 1, self.y() - 1, boardWidth + 2, self.height() + 2, SIGN_EDGE);
			DrawUtil.rect(canvas, x, self.y(), boardWidth, self.height(), SIGN_BOARD);
			SignText text = this.shown;
			int color = 0xFF000000 | SignUtil.textColor(text);
			int centerX = x + boardWidth / 2;
			for (int i = 0; i < SignText.LINES; i++) {
				Text line = text.lines().get(i);
				int width = canvas.textWidth(line);
				int lineY = self.y() + PREVIEW_PADDING + i * lineHeight;
				canvas.text(line, centerX - width / 2, lineY, color, false);
				if (width > max) {
					DrawUtil.rect(canvas, centerX - max / 2, lineY + lineHeight - 1, max, 1, 0xFFFF5555);
				}
			}
		});
		return row;
	}

	private Row checkRow(Text label, boolean checked, Consumer<Boolean> onToggle) {
		Checkbox checkbox = new Checkbox(label, checked, onToggle);
		checkbox.enabled(this.session.writable());
		Row row = new Row(ROW_HEIGHT);
		row.place(checkbox, LABEL_WIDTH, (ROW_HEIGHT - Theme.BUTTON_HEIGHT) / 2, this.rowWidth() - LABEL_WIDTH - 2, Theme.BUTTON_HEIGHT);
		return row;
	}

	private void update(String label, String coalesce, UnaryOperator<SignText> change) {
		boolean front = this.front;
		GameVersion version = this.version();
		this.edit(this.ctx.key(label), coalesce, root -> {
			CompoundTag blockEntity = this.blockEntity(root);
			SignUtil.write(blockEntity, front, version, change.apply(SignUtil.read(blockEntity, front, version)));
			BlockEntityUtil.write(root, this.item(), version, SignUtil.blockEntityId(this.blockId()), blockEntity);
		});
		this.shown = this.text(this.session.root());
	}

	private void change(String label, Consumer<CompoundTag> mutator) {
		GameVersion version = this.version();
		this.edit(this.ctx.key(label), root -> {
			CompoundTag blockEntity = this.blockEntity(root);
			mutator.accept(blockEntity);
			BlockEntityUtil.write(root, this.item(), version, SignUtil.blockEntityId(this.blockId()), blockEntity);
		});
	}

	@Override
	protected void refresh() {
		for (RichTextField line : this.lines) {
			if (line.focused()) {
				return;
			}
		}
		this.populate();
	}

	@Override
	public void focusDefault() {
		if (!this.lines.isEmpty() && this.lines.get(0).editable()) {
			this.lines.get(0).focus();
		}
	}
}
