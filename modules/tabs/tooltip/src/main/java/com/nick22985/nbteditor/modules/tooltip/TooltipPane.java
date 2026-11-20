package com.nick22985.nbteditor.modules.tooltip;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.Checkbox;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import com.nick22985.nbteditor.util.TooltipUtil;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class TooltipPane extends FormPane {
	private final ModuleContext ctx;
	private final Set<String> extra = new LinkedHashSet<>();

	public TooltipPane(EditorSession session, ModuleContext ctx) {
		super(session);
		this.ctx = ctx;
	}

	private GameVersion version() {
		return this.session.version();
	}

	@Override
	protected int headerHeight() {
		return Feature.TOOLTIP_DISPLAY.in(this.version()) ? Theme.BUTTON_HEIGHT + Theme.GAP : 0;
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		if (Feature.TOOLTIP_DISPLAY.in(this.version())) {
			this.headerButton(this.ctx.tr("add"), this::addComponent).enabled(this.session.writable());
			this.headerButton(this.ctx.tr("show_all"), () -> this.change("history.show_all", item -> TooltipUtil.hiddenComponents(item, new LinkedHashSet<>()))).enabled(this.session.writable());
		}
	}

	@Override
	protected void rows(List<Row> rows) {
		GameVersion version = this.version();
		CompoundTag root = this.session.root();
		if (!Feature.ITEM_COMPONENTS.in(version)) {
			this.legacyRows(rows, root, version);
		} else if (!Feature.TOOLTIP_DISPLAY.in(version)) {
			this.showInTooltipRows(rows, root);
		} else {
			this.displayRows(rows, root, version);
		}
	}

	private void legacyRows(List<Row> rows, CompoundTag root, GameVersion version) {
		rows.add(this.header(this.ctx.tr("hide")));
		int flags = TooltipUtil.hideFlags(root, version);
		for (HideFlag flag : HideFlag.values()) {
			if (!flag.supported(version)) {
				continue;
			}
			rows.add(this.checkRow(this.ctx.tr("flag." + flag.name().toLowerCase(Locale.ROOT)), (flags & flag.bit()) != 0, true, checked -> this.change("history.flag", item -> {
				int current = TooltipUtil.hideFlags(item, version);
				TooltipUtil.hideFlags(item, version, checked ? current | flag.bit() : current & ~flag.bit());
			})));
		}
	}

	private void showInTooltipRows(List<Row> rows, CompoundTag root) {
		GameVersion version = this.version();
		rows.add(this.checkRow(this.ctx.tr("hide_tooltip"), TooltipUtil.hideTooltip(root, version), true, checked -> this.change("history.hide_tooltip", item -> TooltipUtil.hideTooltip(item, version, checked))));
		rows.add(this.checkRow(this.ctx.tr("hide_additional"), TooltipUtil.marker(root, TooltipUtil.HIDE_ADDITIONAL), true, checked -> this.change("history.hide_additional", item -> TooltipUtil.marker(item, TooltipUtil.HIDE_ADDITIONAL, checked))));
		rows.add(this.header(this.ctx.tr("hide")));
		for (String component : TooltipUtil.SHOW_IN_TOOLTIP) {
			boolean present = TooltipUtil.marker(root, component);
			Row row = this.checkRow(this.componentName(component), present && !TooltipUtil.shownInTooltip(root, component), present, checked -> this.change("history.component", item -> TooltipUtil.showInTooltip(item, component, !checked)));
			if (!present) {
				row.tooltip(this.ctx.tr("missing"));
			}
			rows.add(row);
		}
	}

	private void displayRows(List<Row> rows, CompoundTag root, GameVersion version) {
		rows.add(this.checkRow(this.ctx.tr("hide_tooltip"), TooltipUtil.hideTooltip(root, version), true, checked -> this.change("history.hide_tooltip", item -> TooltipUtil.hideTooltip(item, version, checked))));
		rows.add(this.header(this.ctx.tr("hidden_components")));
		Set<String> hidden = TooltipUtil.hiddenComponents(root);
		List<String> present = TooltipUtil.presentComponents(root);
		Set<String> candidates = new LinkedHashSet<>(hidden);
		candidates.addAll(present);
		for (String common : TooltipUtil.COMMON) {
			candidates.add(ItemStackUtil.namespaced(common));
		}
		candidates.addAll(this.extra);
		for (String component : candidates) {
			Row row = this.checkRow(this.componentName(component), hidden.contains(component), true, checked -> this.change("history.component", item -> {
				Set<String> next = TooltipUtil.hiddenComponents(item);
				if (checked) {
					next.add(component);
				} else {
					next.remove(component);
				}
				TooltipUtil.hiddenComponents(item, next);
			}));
			if (present.contains(component)) {
				row.decorate((canvas, self) -> canvas.text(Text.literal("●"), self.x() + self.width() - 10, self.y() + 5, Theme.GREEN, true));
				row.tooltip(this.ctx.tr("present"));
			}
			rows.add(row);
		}
	}

	private Text componentName(String component) {
		return Text.literal(ItemStackUtil.stripNamespace(component));
	}

	private Row checkRow(Text label, boolean checked, boolean enabled, Consumer<Boolean> onToggle) {
		Checkbox checkbox = new Checkbox(label, checked, onToggle);
		checkbox.enabled(enabled && this.session.writable());
		Row row = new Row(ROW_HEIGHT);
		row.place(checkbox, 4, (ROW_HEIGHT - Theme.BUTTON_HEIGHT) / 2, this.rowWidth() - 20, Theme.BUTTON_HEIGHT);
		return row;
	}

	private void change(String label, Consumer<CompoundTag> mutator) {
		this.edit(this.ctx.key(label), mutator);
		this.populate();
	}

	private void addComponent() {
		List<String> ids = new ArrayList<>(this.ctx.client().registries().ids(RegistryService.DATA_COMPONENT_TYPE));
		Set<String> hidden = TooltipUtil.hiddenComponents(this.session.root());
		ids.removeIf(hidden::contains);
		ModalUtil.pick(this.frame.screen(), this.ctx.tr("add.title"), ids, this::componentName, null, id -> {
			this.extra.add(id);
			this.change("history.component", item -> {
				Set<String> next = TooltipUtil.hiddenComponents(item);
				next.add(id);
				TooltipUtil.hiddenComponents(item, next);
			});
		});
	}
}
