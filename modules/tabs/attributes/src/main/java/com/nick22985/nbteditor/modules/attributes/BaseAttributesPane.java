package com.nick22985.nbteditor.modules.attributes;

import com.nick22985.nbteditor.data.EntityFields;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.TextField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class BaseAttributesPane extends FormPane {
	private static final int AMOUNT_WIDTH = 72;
	private static final int ICON = 16;

	private final ModuleContext ctx;
	private final AttributeControls controls;
	private final List<Widget> inputs = new ArrayList<>();

	public BaseAttributesPane(EditorSession session, ModuleContext ctx) {
		super(session);
		this.ctx = ctx;
		this.controls = new AttributeControls(ctx, session.writable());
	}

	private GameVersion version() {
		return this.session.version();
	}

	private Map<String, Double> values() {
		Map<String, Double> values = EntityFields.ATTRIBUTES.get(this.session.root(), this.version());
		return values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
	}

	@Override
	protected int headerHeight() {
		return Theme.BUTTON_HEIGHT + Theme.GAP;
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		this.headerButton(this.ctx.tr("add"), this::add).enabled(this.session.writable());
	}

	@Override
	protected void rows(List<Row> rows) {
		this.inputs.clear();
		Map<String, Double> values = this.values();
		rows.add(this.header(this.ctx.tr("base")));
		if (values.isEmpty()) {
			rows.add(this.note(this.ctx.tr("none"), Theme.FAINT));
			return;
		}
		for (Map.Entry<String, Double> entry : values.entrySet()) {
			rows.add(this.attributeRow(entry.getKey(), entry.getValue()));
		}
	}

	private Row attributeRow(String id, double value) {
		Row row = new Row(ROW_HEIGHT);
		int width = this.rowWidth();
		int fields = AMOUNT_WIDTH + ICON * 2 + Theme.GAP * 2 + 2;
		row.place(this.controls.picker(id, next -> this.change(values -> {
			Map<String, Double> renamed = new LinkedHashMap<>();
			values.forEach((key, base) -> renamed.put(key.equals(id) ? next : key, base));
			values.clear();
			values.putAll(renamed);
		}, "history.rename")), 0, 1, width - fields - Theme.GAP, ROW_HEIGHT - 2);
		TextField amount = this.controls.amount(value, base -> {
			GameVersion version = this.version();
			this.edit(this.ctx.key("history.base"), "base." + id, entity -> {
				Map<String, Double> values = EntityFields.ATTRIBUTES.get(entity, version);
				Map<String, Double> next = values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
				next.put(id, base);
				EntityFields.ATTRIBUTES.write(entity, version, next);
			});
		});
		this.inputs.add(amount);
		int x = width - fields;
		row.place(amount, x, 1, AMOUNT_WIDTH, ROW_HEIGHT - 2);
		x += AMOUNT_WIDTH + Theme.GAP;
		row.place(this.controls.infinity(amount), x, 1, ICON, ROW_HEIGHT - 2);
		row.placeRight(this.controls.remove(() -> this.change(values -> values.remove(id), "history.remove")), 2, 1, ICON, ROW_HEIGHT - 2);
		return row;
	}

	private void change(Consumer<Map<String, Double>> change, String label) {
		Map<String, Double> values = this.values();
		change.accept(values);
		GameVersion version = this.version();
		this.edit(this.ctx.key(label), entity -> EntityFields.ATTRIBUTES.write(entity, version, values));
		this.populate();
	}

	private void add() {
		Map<String, Double> current = this.values();
		List<String> ids = new ArrayList<>();
		for (String id : this.controls.ids()) {
			if (!current.containsKey(id)) {
				ids.add(id);
			}
		}
		this.controls.pick(this.frame.screen(), ids, id -> {
			this.change(values -> values.put(id, 0.0), "history.add");
			this.list.scrollToBottom();
		});
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
}
