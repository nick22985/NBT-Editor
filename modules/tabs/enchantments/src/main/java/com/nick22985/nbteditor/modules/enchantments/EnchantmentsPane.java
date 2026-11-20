package com.nick22985.nbteditor.modules.enchantments;

import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.Field;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.ByteTag;
import com.nick22985.nbteditor.nbt.NumericTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.PixelSprites;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.Dropdown;
import com.nick22985.nbteditor.ui.widget.NumberField;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EnchantmentsPane extends FormPane {
	public static final String BOOK = "minecraft:enchanted_book";
	private static final String GLINT = "enchantment_glint_override";
	private static final List<String> GLINT_VALUES = List.of("default", "on", "off");
	private static final int LEVEL_WIDTH = 56;
	private static final int ICON = 16;

	private final ModuleContext ctx;
	private final boolean uncapped;
	private final List<NumberField> levelFields = new ArrayList<>();
	private Map<String, Integer> shown = Map.of();

	public EnchantmentsPane(EditorSession session, ModuleContext ctx, boolean uncapped) {
		super(session);
		this.ctx = ctx;
		this.uncapped = uncapped;
	}

	private GameVersion version() {
		return this.session.version();
	}

	private Field<Map<String, Integer>> field() {
		return BOOK.equals(ItemStackUtil.id(this.session.root())) ? ItemFields.STORED_ENCHANTMENTS : ItemFields.ENCHANTMENTS;
	}

	private int cap() {
		if (!this.uncapped) {
			return ItemDataUtil.maxLevel(this.version());
		}
		return Feature.ITEM_COMPONENTS.in(this.version()) ? Integer.MAX_VALUE : Short.MAX_VALUE;
	}

	private Map<String, Integer> levels() {
		Map<String, Integer> levels = this.field().get(this.session.root(), this.version());
		return levels == null ? new LinkedHashMap<>() : new LinkedHashMap<>(levels);
	}

	private RegistryService registries() {
		return this.ctx.client().registries();
	}

	@Override
	protected int headerHeight() {
		return Theme.BUTTON_HEIGHT + Theme.GAP;
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		this.headerButton(this.ctx.tr("add"), this::add).enabled(this.session.writable());
		this.headerButton(this.ctx.tr("add_all"), this::addAll).enabled(this.session.writable());
		this.headerButton(this.ctx.tr("max_all"), this::maxAll).enabled(this.session.writable());
		this.headerButton(this.ctx.tr("clear"), () -> this.write("history.clear", new LinkedHashMap<>())).enabled(this.session.writable());
	}

	@Override
	protected void rows(List<Row> rows) {
		this.levelFields.clear();
		Map<String, Integer> levels = this.levels();
		this.shown = levels;
		if (Feature.ITEM_COMPONENTS.in(this.version())) {
			rows.add(this.glintRow());
		}
		Text heading = this.ctx.tr(this.field() == ItemFields.STORED_ENCHANTMENTS ? "stored" : "applied");
		rows.add(this.header(heading));
		if (levels.isEmpty()) {
			rows.add(this.note(this.ctx.tr("empty"), Theme.FAINT));
			return;
		}
		for (Map.Entry<String, Integer> entry : levels.entrySet()) {
			rows.add(this.enchantmentRow(entry.getKey(), entry.getValue()));
		}
	}

	private Row glintRow() {
		Tag tag = ItemStackUtil.component(this.session.root(), GLINT);
		String value = tag instanceof NumericTag ? ((NumericTag) tag).intValue() != 0 ? "on" : "off" : "default";
		Dropdown<String> glint = new Dropdown<>(GLINT_VALUES, value, option -> this.ctx.tr("glint." + option), option -> this.edit(this.ctx.key("history.glint"), item -> ItemStackUtil.setComponent(item, GLINT, "default".equals(option) ? null : ByteTag.of("on".equals(option)))));
		glint.enabled(this.session.writable());
		Row row = this.labelled(this.ctx.tr("glint"), 96);
		row.place(glint, 96, 1, 120, ROW_HEIGHT - 2);
		return row;
	}

	private Row enchantmentRow(String id, int level) {
		boolean known = this.registries().contains(RegistryService.ENCHANTMENT, id);
		Text name = known ? this.registries().name(RegistryService.ENCHANTMENT, id) : Text.literal(id);
		Row row = new Row(ROW_HEIGHT);
		int nameWidth = this.rowWidth() - LEVEL_WIDTH - ICON * 2 - 12;
		String detail = ItemStackUtil.stripNamespace(id);
		row.decorate((canvas, self) -> {
			int used = DrawUtil.textClipped(canvas, name, self.x() + 4, self.y() + 5, nameWidth - 4, known ? Theme.TEXT : Theme.RED);
			if (known) {
				DrawUtil.textClipped(canvas, detail, self.x() + used + 10, self.y() + 5, nameWidth - used - 10, Theme.FAINT);
			}
		});
		if (!known) {
			row.tooltip(this.ctx.tr("unknown", id));
		}
		NumberField levelField = new NumberField(true, level);
		levelField.range(1, this.cap());
		levelField.editable(this.session.writable());
		levelField.onValue(value -> this.setLevel(id, (int) value));
		this.levelFields.add(levelField);
		row.placeRight(levelField, ICON * 2 + 6, 1, LEVEL_WIDTH, ROW_HEIGHT - 2);
		Button max = this.iconButton(PixelSprites.UP, Theme.GOLD, this.ctx.tr("max"), () -> {
			levelField.number(this.cap());
			this.setLevel(id, this.cap());
		});
		row.placeRight(max, ICON + 4, 1, ICON, ROW_HEIGHT - 2);
		Button remove = this.iconButton(PixelSprites.CROSS, Theme.RED, this.ctx.tr("remove"), () -> {
			Map<String, Integer> levels = this.levels();
			levels.remove(id);
			this.write("history.remove", levels);
		});
		row.placeRight(remove, 2, 1, ICON, ROW_HEIGHT - 2);
		return row;
	}

	private void setLevel(String id, int level) {
		GameVersion version = this.version();
		Field<Map<String, Integer>> field = this.field();
		this.edit(this.ctx.key("history.level"), "enchant." + id, item -> {
			Map<String, Integer> levels = field.get(item, version);
			Map<String, Integer> next = levels == null ? new LinkedHashMap<>() : new LinkedHashMap<>(levels);
			next.put(id, level);
			field.write(item, version, next);
		});
		this.shown = this.levels();
	}

	private void write(String label, Map<String, Integer> levels) {
		GameVersion version = this.version();
		Field<Map<String, Integer>> field = this.field();
		this.edit(this.ctx.key(label), item -> field.write(item, version, levels));
		this.populate();
	}

	private List<String> available() {
		Map<String, Integer> current = this.levels();
		List<String> ids = new ArrayList<>();
		for (String id : this.registries().ids(RegistryService.ENCHANTMENT)) {
			if (!current.containsKey(id)) {
				ids.add(id);
			}
		}
		ids.sort(Comparator.comparing(id -> this.registries().name(RegistryService.ENCHANTMENT, id).plain()));
		return ids;
	}

	private void add() {
		ModalUtil.pick(this.frame.screen(), this.ctx.tr("add.title"), this.available(), id -> this.registries().name(RegistryService.ENCHANTMENT, id), ItemStackUtil::stripNamespace, id -> {
			Map<String, Integer> levels = this.levels();
			levels.put(id, 1);
			this.write("history.add", levels);
		});
	}

	private void addAll() {
		Map<String, Integer> levels = this.levels();
		for (String id : this.available()) {
			levels.put(id, this.cap());
		}
		this.write("history.add_all", levels);
	}

	private void maxAll() {
		Map<String, Integer> levels = this.levels();
		levels.replaceAll((id, level) -> this.cap());
		this.write("history.max_all", levels);
	}

	@Override
	protected void refresh() {
		if (this.levelFields.stream().anyMatch(NumberField::focused) && this.levels().keySet().equals(this.shown.keySet())) {
			return;
		}
		this.populate();
	}
}
