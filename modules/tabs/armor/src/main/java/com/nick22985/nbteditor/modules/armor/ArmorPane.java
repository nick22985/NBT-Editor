package com.nick22985.nbteditor.modules.armor;

import com.nick22985.nbteditor.data.ArmorTrim;
import com.nick22985.nbteditor.data.Feature;
import com.nick22985.nbteditor.data.FieldValue;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.editor.RegistryPicker;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.ItemService;
import com.nick22985.nbteditor.platform.RegistryService;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.ColorPicker;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.util.ColorUtil;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ItemStackUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ArmorPane extends FormPane {
	private static final Pattern PIECE = Pattern.compile("^(.+)_(helmet|chestplate|leggings|boots)$");
	private static final List<String> PIECES = List.of("helmet", "chestplate", "leggings", "boots");
	private static final String DEFAULT_PATTERN = "minecraft:coast";
	private static final String DEFAULT_MATERIAL = "minecraft:iron";
	private static final int LABEL_WIDTH = 96;
	private static final float PREVIEW_SCALE = 2.0F;
	private static final int PREVIEW_SIZE = 34;

	private final ModuleContext ctx;
	private final RegistryPicker materials;
	private final RegistryPicker patterns;
	private final Random random = new Random();
	private List<GameItem> preview = List.of();
	private boolean ready;

	public ArmorPane(EditorSession session, ModuleContext ctx) {
		super(session);
		this.ctx = ctx;
		this.materials = new RegistryPicker(ctx.client().registries(), RegistryService.TRIM_MATERIAL, ctx.tr("trim.material.title"), session.writable());
		this.patterns = new RegistryPicker(ctx.client().registries(), RegistryService.TRIM_PATTERN, ctx.tr("trim.pattern.title"), session.writable());
	}

	private GameVersion version() {
		return this.session.version();
	}

	private boolean trims() {
		return Feature.TRIMS.in(this.version());
	}

	@Override
	protected int headerHeight() {
		return Theme.BUTTON_HEIGHT + Theme.GAP;
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		boolean writable = this.session.writable();
		this.headerButton(this.ctx.tr("dye.clear"), () -> this.change("history.dye_clear", item -> ItemFields.DYED_COLOR.write(item, this.version(), null))).enabled(writable);
		if (this.trims()) {
			this.headerButton(this.ctx.tr("trim.random"), this::randomTrim).enabled(writable);
			this.headerButton(this.ctx.tr("trim.remove"), () -> this.change("history.trim_remove", item -> ItemFields.TRIM.write(item, this.version(), null))).enabled(writable);
		}
	}

	@Override
	protected void rows(List<Row> rows) {
		this.ready = false;
		GameVersion version = this.version();
		CompoundTag root = this.session.root();
		this.preview = this.previewItems(root);
		rows.add(this.previewRow());
		rows.add(this.header(this.ctx.tr("dye")));
		Integer color = ItemFields.DYED_COLOR.get(root, version);
		rows.add(this.dyeStatus());
		ColorPicker picker = new ColorPicker(color == null ? ColorUtil.LEATHER_COLOR : color, ColorUtil.DYE_COLORS, this::setColor);
		picker.enabled(this.session.writable());
		Row pickerRow = new Row(ColorPicker.PREFERRED_HEIGHT + 4);
		pickerRow.place(picker, 4, 2, Math.min(this.rowWidth() - 8, 260), ColorPicker.PREFERRED_HEIGHT);
		rows.add(pickerRow);
		if (this.trims()) {
			this.trimRows(rows, root, version);
		}
		this.ready = true;
	}

	private void trimRows(List<Row> rows, CompoundTag root, GameVersion version) {
		rows.add(this.header(this.ctx.tr("trim")));
		FieldValue<ArmorTrim> value = ItemFields.TRIM.read(root, version);
		if (!value.ok()) {
			rows.add(this.note(this.ctx.tr("trim.invalid"), Theme.RED));
		}
		ArmorTrim trim = value.value();
		rows.add(this.field(this.ctx.tr("trim.material"), LABEL_WIDTH, this.materials.button(trim == null ? null : trim.material(), id -> this.setTrim(current -> current.withMaterial(id)))));
		rows.add(this.field(this.ctx.tr("trim.pattern"), LABEL_WIDTH, this.patterns.button(trim == null ? null : trim.pattern(), id -> this.setTrim(current -> current.withPattern(id)))));
	}

	private Row dyeStatus() {
		Row row = new Row(Theme.LINE + 2);
		Text none = this.ctx.tr("dye.none");
		row.decorate((canvas, self) -> {
			Integer color = ItemFields.DYED_COLOR.get(this.session.root(), this.version());
			DrawUtil.textClipped(canvas, color == null ? none : this.ctx.tr("dye.current", ColorUtil.toHex(color)), self.x() + 4, self.y() + 2, self.width() - 8, color == null ? Theme.FAINT : Theme.LABEL);
		});
		return row;
	}

	private Row previewRow() {
		Row row = new Row(PREVIEW_SIZE + 4);
		row.decorate((canvas, self) -> {
			int x = self.x() + 4;
			for (GameItem item : this.preview) {
				canvas.item(item, x + 1, self.y() + 3, PREVIEW_SCALE, false);
				x += PREVIEW_SIZE + Theme.GAP;
			}
		});
		return row;
	}

	private List<GameItem> previewItems(CompoundTag root) {
		ItemService items = this.ctx.client().items();
		List<GameItem> out = new ArrayList<>();
		String id = ItemStackUtil.id(root);
		Matcher matcher = id == null ? null : PIECE.matcher(id);
		if (matcher != null && matcher.matches()) {
			for (String piece : PIECES) {
				String pieceId = matcher.group(1) + "_" + piece;
				if (items.isItem(pieceId)) {
					CompoundTag copy = root.copy();
					ItemStackUtil.setId(copy, pieceId);
					out.add(items.fromTag(copy));
				}
			}
		}
		if (out.isEmpty()) {
			out.add(items.fromTag(root.copy()));
		}
		return out;
	}

	private void setColor(int rgb) {
		if (!this.ready) {
			return;
		}
		GameVersion version = this.version();
		this.edit(this.ctx.key("history.dye"), "dye", item -> ItemFields.DYED_COLOR.write(item, version, rgb));
		this.preview = this.previewItems(this.session.root());
	}

	private void setTrim(UnaryOperator<ArmorTrim> change) {
		GameVersion version = this.version();
		this.change("history.trim", item -> {
			ArmorTrim current = ItemFields.TRIM.get(item, version);
			ItemFields.TRIM.write(item, version, change.apply(current == null ? new ArmorTrim(this.fallback(this.materials, DEFAULT_MATERIAL), this.fallback(this.patterns, DEFAULT_PATTERN)) : current));
		});
	}

	private String fallback(RegistryPicker picker, String preferred) {
		if (picker.known(preferred)) {
			return preferred;
		}
		List<String> ids = picker.ids();
		return ids.isEmpty() ? preferred : ids.get(0);
	}

	private void randomTrim() {
		List<String> materials = this.materials.ids();
		List<String> patterns = this.patterns.ids();
		if (materials.isEmpty() || patterns.isEmpty()) {
			return;
		}
		ArmorTrim trim = new ArmorTrim(materials.get(this.random.nextInt(materials.size())), patterns.get(this.random.nextInt(patterns.size())));
		this.change("history.trim", item -> ItemFields.TRIM.write(item, this.version(), trim));
	}

	private void change(String label, Consumer<CompoundTag> mutator) {
		this.edit(this.ctx.key(label), mutator);
		this.populate();
	}
}
