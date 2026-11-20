package com.nick22985.nbteditor.modules.misc;

import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.PresetItem;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.text.LegacyTextSyntax;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.text.TextSyntax;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.text.RichTextAction;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.RichTextField;
import com.nick22985.nbteditor.ui.widget.RichToolbar;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.util.BlockEntityUtil;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.ItemDataUtil;
import com.nick22985.nbteditor.util.ModalUtil;
import com.nick22985.nbteditor.util.SignatureUtil;
import com.nick22985.nbteditor.util.SkullUtil;
import com.nick22985.nbteditor.util.UuidUtil;
import java.util.List;
import java.util.UUID;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class MiscPane extends FormPane {
	public static final String UUID_KEY = "UUID";
	private static final int LABEL_WIDTH = 72;
	private static final int BUTTON_WIDTH = 64;

	private final ModuleContext ctx;
	private final Signature signature;
	private RichTextField signatureField;
	private RichToolbar toolbar;

	public MiscPane(EditorSession session, ModuleContext ctx, Signature signature) {
		super(session);
		this.ctx = ctx;
		this.signature = signature;
	}

	private GameVersion version() {
		return this.session.version();
	}

	private boolean item() {
		return this.session.isItem();
	}

	private boolean skull() {
		return SkullUtil.isPlayerHead(BlockEntityUtil.blockId(this.session.root(), this.item()));
	}

	@Override
	protected int headerHeight() {
		return this.item() ? (Theme.BUTTON_HEIGHT + Theme.GAP) * 2 : 0;
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		if (!this.item()) {
			return;
		}
		this.headerButton(this.ctx.tr("preset"), this::preset).enabled(this.ctx.client().player().present()).tooltip(this.ctx.tr("preset.hint"));
		this.signatureField = new RichTextField(false);
		TextSyntax syntax = this.ctx.services().find(TextSyntax.class).orElse(LegacyTextSyntax.INSTANCE);
		this.signatureField.pasteParser(input -> syntax.recognizes(input) ? syntax.parse(input) : null);
		this.signatureField.copyFormatter(syntax::stringify);
		this.signatureField.value(this.signature.get());
		this.signatureField.onChange(this::signatureChanged);
		List<RichTextAction> actions = this.ctx.registry(RichTextAction.REGISTRY).values();
		this.toolbar = frame.add(new RichToolbar(this.signatureField, actions));
		this.toolbar.bounds(frame.bodyX(), frame.bodyY() + Theme.BUTTON_HEIGHT + Theme.GAP, frame.mainWidth(), Theme.BUTTON_HEIGHT);
	}

	@Override
	protected void rows(List<Row> rows) {
		CompoundTag root = this.session.root();
		if (this.skull()) {
			this.skullRows(rows, root);
		}
		if (this.item()) {
			this.signatureRows(rows, root);
			this.uuidRows(rows, root);
		}
	}

	private void skullRows(List<Row> rows, CompoundTag root) {
		GameVersion version = this.version();
		Tag profile = SkullUtil.profile(root, this.item(), version);
		rows.add(this.header(this.ctx.tr("skull")));
		String name = SkullUtil.name(profile);
		UUID uuid = SkullUtil.uuid(profile);
		String texture = SkullUtil.texture(profile);
		rows.add(this.valueRow(this.ctx.tr("skull.owner"), name == null ? this.ctx.tr("none") : Text.literal(name), name == null));
		rows.add(this.valueRow(this.ctx.tr("skull.uuid"), uuid == null ? this.ctx.tr("none") : Text.literal(uuid.toString()), uuid == null));
		Row textureRow = this.valueRow(this.ctx.tr("skull.texture"), texture == null ? this.ctx.tr("none") : Text.literal(texture), texture == null);
		if (texture != null) {
			Button copy = new Button(this.ctx.tr("copy"), () -> {
				this.ctx.client().system().setClipboard(texture);
				this.frame.status(this.ctx.tr("copied"));
			});
			textureRow.placeRight(copy, 2, 1, BUTTON_WIDTH, ROW_HEIGHT - 2);
		}
		rows.add(textureRow);
		Row unbind = new Row(ROW_HEIGHT);
		Button button = new Button(this.ctx.tr("unbind"), () -> {
			boolean item = this.item();
			this.editAndRefresh(this.ctx.key("history.unbind"), target -> SkullUtil.unbind(target, item, version));
		});
		button.enabled(this.session.writable() && texture != null && !SkullUtil.UNBOUND_NAME.equals(name));
		button.tooltip(this.ctx.tr(texture == null ? "unbind.no_texture" : "unbind.hint"));
		unbind.place(button, LABEL_WIDTH, 1, Math.min(160, this.rowWidth() - LABEL_WIDTH - 2), ROW_HEIGHT - 2);
		rows.add(unbind);
	}

	private void signatureRows(List<Row> rows, CompoundTag root) {
		GameVersion version = this.version();
		rows.add(this.header(this.ctx.tr("signature")));
		Row field = this.labelled(this.ctx.tr("signature.text"), LABEL_WIDTH);
		field.place(this.signatureField, LABEL_WIDTH, 1, this.rowWidth() - LABEL_WIDTH - 2, ROW_HEIGHT - 2);
		field.tooltip(this.ctx.tr("signature.hint"));
		rows.add(field);
		boolean signed = SignatureUtil.has(root, version, this.signature.get());
		Row actions = new Row(ROW_HEIGHT);
		Button toggle = new Button(this.ctx.tr(signed ? "signature.remove" : "signature.add"), () -> this.editAndRefresh(this.ctx.key(signed ? "history.unsign" : "history.sign"), item -> {
			if (signed) {
				SignatureUtil.remove(item, version, this.signature.get());
			} else {
				SignatureUtil.add(item, version, this.signature.get());
			}
		}));
		toggle.enabled(this.session.writable());
		actions.place(toggle, LABEL_WIDTH, 1, Math.min(160, this.rowWidth() - LABEL_WIDTH - 2), ROW_HEIGHT - 2);
		rows.add(actions);
	}

	private void uuidRows(List<Row> rows, CompoundTag root) {
		GameVersion version = this.version();
		rows.add(this.header(this.ctx.tr("uuid")));
		UUID uuid = UuidUtil.fromTag(ItemDataUtil.customData(root, version, UUID_KEY));
		Row value = this.valueRow(this.ctx.tr("uuid.value"), uuid == null ? this.ctx.tr("none") : Text.literal(uuid.toString()), uuid == null);
		value.tooltip(this.ctx.tr("uuid.hint"));
		rows.add(value);
		Row actions = new Row(ROW_HEIGHT);
		int width = Math.min(120, (this.rowWidth() - LABEL_WIDTH - Theme.GAP - 2) / 2);
		Button generate = new Button(this.ctx.tr(uuid == null ? "uuid.add" : "uuid.regenerate"), () -> this.editAndRefresh(this.ctx.key("history.uuid"), item -> ItemDataUtil.putCustomData(item, version, UUID_KEY, UuidUtil.toIntArray(UUID.randomUUID()))));
		generate.enabled(this.session.writable());
		actions.place(generate, LABEL_WIDTH, 1, width, ROW_HEIGHT - 2);
		Button remove = new Button(this.ctx.tr("uuid.remove"), () -> this.editAndRefresh(this.ctx.key("history.uuid_remove"), item -> ItemDataUtil.putCustomData(item, version, UUID_KEY, null)));
		remove.enabled(this.session.writable() && uuid != null);
		actions.place(remove, LABEL_WIDTH + width + Theme.GAP, 1, width, ROW_HEIGHT - 2);
		rows.add(actions);
	}

	private Row valueRow(Text label, Text value, boolean faint) {
		Row row = this.labelled(label, LABEL_WIDTH);
		row.decorate((canvas, self) -> DrawUtil.textClipped(canvas, value, self.x() + LABEL_WIDTH, self.y() + 5, self.width() - LABEL_WIDTH - BUTTON_WIDTH - 8, faint ? Theme.FAINT : Theme.TEXT));
		return row;
	}

	private void signatureChanged() {
		Text previous = this.signature.get();
		this.signature.set(this.signatureField.value());
		Text next = this.signature.get();
		GameVersion version = this.version();
		if (SignatureUtil.has(this.session.root(), version, previous)) {
			this.edit(this.ctx.key("history.signature"), "signature", item -> SignatureUtil.replace(item, version, previous, next));
		}
	}

	private void preset() {
		ModalUtil.pick(this.frame.screen(), this.ctx.tr("preset.title"), this.ctx.registry(PresetItem.REGISTRY).values(), PresetItem::name, null, preset -> {
			boolean given = this.ctx.client().player().give(this.ctx.client().items().fromTag(preset.create(this.ctx.platform().game().version())));
			this.frame.status(this.ctx.tr(given ? "preset.given" : "preset.full"));
		});
	}

	@Override
	protected void refresh() {
		if (this.signatureField != null && this.signatureField.focused()) {
			return;
		}
		this.populate();
	}
}
