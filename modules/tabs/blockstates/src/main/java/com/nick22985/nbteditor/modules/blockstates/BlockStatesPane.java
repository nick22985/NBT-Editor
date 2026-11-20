package com.nick22985.nbteditor.modules.blockstates;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.Dropdown;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.util.BlockEntityUtil;
import com.nick22985.nbteditor.util.BlockStateUtil;
import com.nick22985.nbteditor.util.DrawUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BlockStatesPane extends FormPane {
	private static final String UNSET = "";
	private static final int LABEL_WIDTH = 120;
	private static final int VALUE_WIDTH = 120;
	private static final int ICON = 16;
	private static final int WIDE_LIST = 8;

	private final ModuleContext ctx;

	public BlockStatesPane(EditorSession session, ModuleContext ctx) {
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

	private Map<String, List<String>> allowed() {
		return this.ctx.client().registries().blockProperties(this.blockId());
	}

	private Map<String, String> state() {
		return BlockStateUtil.read(this.session.root(), this.item(), this.version());
	}

	@Override
	protected int headerHeight() {
		return Theme.BUTTON_HEIGHT + Theme.GAP;
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		boolean writable = this.session.writable();
		this.headerButton(this.ctx.tr("reset"), this::reset).enabled(writable).tooltip(this.ctx.tr(this.item() ? "reset.item" : "reset.block"));
		if (this.item()) {
			this.headerButton(this.ctx.tr("fill"), () -> this.write("history.fill", this.ctx.client().registries().defaultBlockState(this.blockId()))).enabled(writable).tooltip(this.ctx.tr("fill.hint"));
		}
	}

	@Override
	protected void rows(List<Row> rows) {
		Map<String, List<String>> allowed = this.allowed();
		Map<String, String> state = this.state();
		rows.add(this.header(this.ctx.tr("properties")));
		if (this.item()) {
			rows.add(this.note(this.ctx.tr("item.hint"), Theme.FAINT));
		}
		for (Map.Entry<String, List<String>> entry : allowed.entrySet()) {
			rows.add(this.propertyRow(entry.getKey(), entry.getValue(), state.get(entry.getKey())));
		}
		List<String> unknown = new ArrayList<>(state.keySet());
		unknown.removeAll(allowed.keySet());
		if (!unknown.isEmpty()) {
			rows.add(this.header(this.ctx.tr("unknown")));
			for (String key : unknown) {
				rows.add(this.unknownRow(key, state.get(key)));
			}
		}
	}

	private Row propertyRow(String key, List<String> values, String current) {
		List<String> options = new ArrayList<>();
		if (this.item()) {
			options.add(UNSET);
		}
		options.addAll(values);
		String selected = current == null ? this.item() ? UNSET : values.get(0) : current;
		if (!options.contains(selected)) {
			options.add(selected);
		}
		Dropdown<String> dropdown = new Dropdown<>(options, selected, this::valueLabel, value -> this.change("history.set", state -> {
			if (UNSET.equals(value)) {
				state.remove(key);
			} else {
				state.put(key, value);
			}
		}));
		dropdown.enabled(this.session.writable());
		if (options.size() > WIDE_LIST) {
			dropdown.columns(options.size() > WIDE_LIST * 2 ? 4 : 2);
		}
		boolean invalid = current != null && !values.contains(current);
		Row row = new Row(ROW_HEIGHT);
		int labelWidth = Math.min(LABEL_WIDTH, this.rowWidth() - VALUE_WIDTH - 4);
		row.decorate((canvas, self) -> DrawUtil.textClipped(canvas, key, self.x() + 4, self.y() + 5, labelWidth - 8, invalid ? Theme.RED : Theme.LABEL));
		row.place(dropdown, labelWidth, 1, Math.min(VALUE_WIDTH, this.rowWidth() - labelWidth - 2), ROW_HEIGHT - 2);
		if (invalid) {
			row.tooltip(this.ctx.tr("invalid", current));
		}
		return row;
	}

	private Row unknownRow(String key, String value) {
		Row row = new Row(ROW_HEIGHT);
		Text text = Text.literal(key + " = " + value);
		row.decorate((canvas, self) -> DrawUtil.textClipped(canvas, text, self.x() + 4, self.y() + 5, self.width() - ICON - 12, Theme.RED));
		row.tooltip(this.ctx.tr("unknown.hint"));
		row.placeRight(this.iconButton(PixelSprites.CROSS, Theme.RED, this.ctx.tr("remove"), () -> this.change("history.remove", state -> state.remove(key))), 2, 1, ICON, ROW_HEIGHT - 2);
		return row;
	}

	private Text valueLabel(String value) {
		return UNSET.equals(value) ? this.ctx.tr("unset") : Text.literal(value);
	}

	private void reset() {
		this.write("history.reset", this.item() ? new LinkedHashMap<>() : this.ctx.client().registries().defaultBlockState(this.blockId()));
	}

	private void change(String label, Consumer<Map<String, String>> change) {
		Map<String, String> state = new LinkedHashMap<>(this.state());
		change.accept(state);
		this.write(label, state);
	}

	private void write(String label, Map<String, String> state) {
		GameVersion version = this.version();
		boolean item = this.item();
		Map<String, String> copy = new LinkedHashMap<>(state);
		this.edit(this.ctx.key(label), root -> BlockStateUtil.write(root, item, version, copy));
		this.populate();
	}
}
