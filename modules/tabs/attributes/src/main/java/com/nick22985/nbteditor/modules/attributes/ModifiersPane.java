package com.nick22985.nbteditor.modules.attributes;

import com.nick22985.nbteditor.data.AttributeModifier;
import com.nick22985.nbteditor.data.AttributeOperation;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.widget.Dropdown;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.TextField;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ItemDataUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ModifiersPane extends FormPane {
	private static final int OPERATION_WIDTH = 52;
	private static final int AMOUNT_WIDTH = 52;
	private static final int SLOT_WIDTH = 60;
	private static final int ICON = 16;
	private static final int MIN_NAME_WIDTH = 100;

	private final ModuleContext ctx;
	private final AttributeControls controls;
	private final List<Widget> inputs = new ArrayList<>();

	public ModifiersPane(EditorSession session, ModuleContext ctx) {
		super(session);
		this.ctx = ctx;
		this.controls = new AttributeControls(ctx, session.writable());
	}

	private GameVersion version() {
		return this.session.version();
	}

	private List<AttributeModifier> modifiers() {
		List<AttributeModifier> modifiers = ItemFields.ATTRIBUTE_MODIFIERS.get(this.session.root(), this.version());
		return modifiers == null ? null : new ArrayList<>(modifiers);
	}

	@Override
	protected int headerHeight() {
		return Theme.BUTTON_HEIGHT + Theme.GAP;
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		boolean writable = this.session.writable();
		this.headerButton(this.ctx.tr("add"), this::add).enabled(writable);
		this.headerButton(this.ctx.tr("clear"), () -> this.write("history.clear", new ArrayList<>())).enabled(writable);
		this.headerButton(this.ctx.tr("reset"), () -> this.write("history.reset", null)).enabled(writable).tooltip(this.ctx.tr("reset.hint"));
	}

	@Override
	protected void rows(List<Row> rows) {
		this.inputs.clear();
		List<AttributeModifier> modifiers = this.modifiers();
		if (modifiers == null) {
			rows.add(this.note(this.ctx.tr(Feature.ITEM_COMPONENTS.in(this.version()) ? "defaults" : "empty"), Theme.FAINT));
			return;
		}
		if (modifiers.isEmpty()) {
			rows.add(this.note(this.ctx.tr("none"), Theme.FAINT));
			return;
		}
		int fields = OPERATION_WIDTH + AMOUNT_WIDTH + ICON + SLOT_WIDTH + ICON + Theme.GAP * 4 + 2;
		boolean stacked = this.rowWidth() - fields < MIN_NAME_WIDTH;
		if (!stacked) {
			rows.add(this.columns(fields));
		}
		for (int i = 0; i < modifiers.size(); i++) {
			rows.add(this.modifierRow(i, modifiers.get(i), stacked, fields));
		}
	}

	private Row columns(int fields) {
		Row row = new Row(Theme.ROW - 2);
		int width = this.rowWidth();
		Text attribute = this.ctx.tr("attribute");
		Text operation = this.ctx.tr("operation");
		Text amount = this.ctx.tr("amount");
		Text slot = this.ctx.tr("slot");
		row.decorate((canvas, self) -> {
			int x = self.x() + width - fields;
			DrawUtil.textClipped(canvas, attribute, self.x() + 4, self.y() + 1, width - fields - 8, Theme.LABEL);
			DrawUtil.textClipped(canvas, operation, x + 1, self.y() + 1, OPERATION_WIDTH, Theme.LABEL);
			x += OPERATION_WIDTH + Theme.GAP;
			DrawUtil.textClipped(canvas, amount, x + 1, self.y() + 1, AMOUNT_WIDTH, Theme.LABEL);
			x += AMOUNT_WIDTH + ICON + Theme.GAP * 2;
			DrawUtil.textClipped(canvas, slot, x + 1, self.y() + 1, SLOT_WIDTH, Theme.LABEL);
		});
		return row;
	}

	private Row modifierRow(int index, AttributeModifier modifier, boolean stacked, int fields) {
		int offset = stacked ? ROW_HEIGHT : 0;
		Row row = new Row(ROW_HEIGHT + offset);
		int width = this.rowWidth();
		int nameWidth = stacked ? width - ICON - 4 : width - fields - Theme.GAP;
		row.place(this.controls.picker(modifier.attribute(), id -> this.update(index, "attribute", m -> m.withAttribute(id))), 0, 1, nameWidth, ROW_HEIGHT - 2);
		int x = stacked ? 0 : width - fields;
		Dropdown<AttributeOperation> operation = new Dropdown<>(Arrays.asList(AttributeOperation.values()), modifier.operation(), this::operationLabel, op -> this.update(index, "operation", m -> m.withOperation(op)));
		operation.enabled(this.session.writable());
		operation.tooltip(() -> List.of(this.ctx.tr("operation." + operation.value().serializedName() + ".hint")));
		row.place(operation, x, offset + 1, OPERATION_WIDTH, ROW_HEIGHT - 2);
		x += OPERATION_WIDTH + Theme.GAP;
		TextField amount = this.controls.amount(modifier.amount(), value -> this.update(index, "amount", m -> m.withAmount(value)));
		this.inputs.add(amount);
		row.place(amount, x, offset + 1, AMOUNT_WIDTH, ROW_HEIGHT - 2);
		x += AMOUNT_WIDTH + Theme.GAP;
		row.place(this.controls.infinity(amount), x, offset + 1, ICON, ROW_HEIGHT - 2);
		x += ICON + Theme.GAP;
		List<String> slots = new ArrayList<>(ItemDataUtil.attributeSlots(this.version()));
		if (!slots.contains(modifier.slot())) {
			slots.add(modifier.slot());
		}
		Dropdown<String> slot = new Dropdown<>(slots, modifier.slot(), this::slotLabel, value -> this.update(index, "slot", m -> m.withSlot(value)));
		slot.enabled(this.session.writable());
		slot.columns(2);
		row.place(slot, x, offset + 1, SLOT_WIDTH, ROW_HEIGHT - 2);
		row.placeRight(this.controls.remove(() -> {
			List<AttributeModifier> modifiers = this.modifiers();
			if (modifiers != null && index < modifiers.size()) {
				modifiers.remove(index);
				this.write("history.remove", modifiers);
			}
		}), 2, 1, ICON, ROW_HEIGHT - 2);
		if (modifier.id() != null) {
			row.tooltip(this.ctx.tr("modifier_id", modifier.id()));
		}
		return row;
	}

	private Text operationLabel(AttributeOperation operation) {
		return this.ctx.tr("operation." + operation.serializedName());
	}

	private Text slotLabel(String slot) {
		return this.ctx.tr("slot." + slot.toLowerCase(Locale.ROOT));
	}

	private void update(int index, String part, UnaryOperator<AttributeModifier> change) {
		GameVersion version = this.version();
		this.edit(this.ctx.key("history.modify"), "attribute." + index + "." + part, item -> {
			List<AttributeModifier> modifiers = ItemFields.ATTRIBUTE_MODIFIERS.get(item, version);
			if (modifiers == null || index >= modifiers.size()) {
				return;
			}
			List<AttributeModifier> next = new ArrayList<>(modifiers);
			next.set(index, change.apply(next.get(index)));
			ItemFields.ATTRIBUTE_MODIFIERS.write(item, version, next);
		});
	}

	private void write(String label, List<AttributeModifier> modifiers) {
		GameVersion version = this.version();
		this.edit(this.ctx.key(label), item -> ItemFields.ATTRIBUTE_MODIFIERS.write(item, version, modifiers));
		this.populate();
	}

	private void add() {
		this.controls.pick(this.frame.screen(), this.controls.ids(), id -> {
			List<AttributeModifier> modifiers = this.modifiers();
			List<AttributeModifier> next = modifiers == null ? new ArrayList<>() : modifiers;
			next.add(AttributeModifier.create(id, 1, AttributeOperation.ADD_VALUE, AttributeModifier.ANY, this.version()));
			this.write("history.add", next);
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
