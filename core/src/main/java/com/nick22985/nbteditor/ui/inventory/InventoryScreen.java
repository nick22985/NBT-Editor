package com.nick22985.nbteditor.ui.inventory;

import com.nick22985.nbteditor.container.ContainerService;
import com.nick22985.nbteditor.data.Field;
import com.nick22985.nbteditor.data.GameVersion;
import com.nick22985.nbteditor.data.ItemFields;
import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.ItemLostEvent;
import com.nick22985.nbteditor.hook.LostItemReason;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.ItemService;
import com.nick22985.nbteditor.platform.PlayerService;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.HoveredItem;
import com.nick22985.nbteditor.ui.Key;
import com.nick22985.nbteditor.ui.KeyInput;
import com.nick22985.nbteditor.ui.Modifiers;
import com.nick22985.nbteditor.ui.MouseButton;
import com.nick22985.nbteditor.ui.MouseInput;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.Widget;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.util.DrawUtil;
import com.nick22985.nbteditor.util.InventoryUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public abstract class InventoryScreen extends UiScreen {
	private static final String ENCHANTED_BOOK = "minecraft:enchanted_book";
	private static final int MIN_PANEL_WIDTH = 176;
	private static final int PANEL_PAD = 7;
	private static final int LABEL_TOP = 6;
	private static final int LABEL_HEIGHT = 11;
	private static final int SECTION_SPACING = 2;
	private static final int SIDE_WIDTH = 120;
	private static final int SIDE_MIN_WIDTH = 80;
	private static final int SIDE_SPACING = 3;
	private static final int STATUS_GAP = 6;

	protected final ModuleContext ctx;
	private final List<SlotSection> sections = new ArrayList<>();
	private final List<SectionView> views = new ArrayList<>();
	private final List<Widget> sideButtons = new ArrayList<>();
	private final List<SlotRef> dragSlots = new ArrayList<>();
	private List<Text> sideLines = List.of();
	private GameItem cursor;
	private MouseButton dragButton;
	private SlotRef dragOrigin;
	private int panelX;
	private int panelY;
	private int panelWidth;
	private int panelHeight;
	private int sideX;
	private int sideY;
	private int mouseX = Integer.MIN_VALUE / 2;
	private int mouseY = Integer.MIN_VALUE / 2;

	protected InventoryScreen(ModuleContext ctx, Text title) {
		super(title);
		this.ctx = ctx;
		this.cursor = ctx.client().items().empty();
	}

	protected ItemService items() {
		return this.ctx.client().items();
	}

	protected PlayerService player() {
		return this.ctx.client().player();
	}

	protected <T extends SlotSection> T addSection(T section) {
		this.sections.add(section);
		return section;
	}

	protected void clearSections() {
		this.sections.clear();
	}

	public List<SlotSection> sections() {
		return List.copyOf(this.sections);
	}

	public PlayerSection playerSection() {
		for (SlotSection section : this.sections) {
			if (section instanceof PlayerSection) {
				return (PlayerSection) section;
			}
		}
		return null;
	}

	public GameItem cursor() {
		return this.cursor;
	}

	protected void setCursor(GameItem cursor) {
		this.cursor = cursor == null ? this.items().empty() : cursor;
	}

	public int panelX() {
		return this.panelX;
	}

	public int panelY() {
		return this.panelY;
	}

	public int panelWidth() {
		return this.panelWidth;
	}

	public int panelHeight() {
		return this.panelHeight;
	}

	@Override
	protected final void build() {
		this.views.clear();
		this.sideButtons.clear();
		int contentWidth = 0;
		int y = LABEL_TOP;
		List<int[]> offsets = new ArrayList<>();
		for (SlotSection section : this.sections) {
			contentWidth = Math.max(contentWidth, section.layout().width());
			int labelY = y;
			if (section.label() != null) {
				y += LABEL_HEIGHT;
			} else {
				y += 1;
			}
			offsets.add(new int[] {labelY, y});
			y += section.layout().height() + SECTION_SPACING;
		}
		this.panelWidth = Math.max(MIN_PANEL_WIDTH, contentWidth + PANEL_PAD * 2);
		this.panelHeight = y - SECTION_SPACING + PANEL_PAD;
		this.buildSide();
		Text side = this.sideText();
		boolean hasSide = side != null || !this.sideButtons.isEmpty();
		int sideWidth = hasSide ? SIDE_WIDTH : 0;
		this.panelX = (this.width - this.panelWidth) / 2;
		if (hasSide) {
			int maxX = this.width - Theme.PAD - SIDE_MIN_WIDTH - Theme.PAD - this.panelWidth;
			this.panelX = Math.max(Theme.PAD, Math.min(this.panelX, maxX));
			this.sideX = this.panelX + this.panelWidth + Theme.PAD;
			sideWidth = Math.max(SIDE_MIN_WIDTH, Math.min(SIDE_WIDTH, this.width - Theme.PAD - this.sideX));
		}
		this.panelY = Math.max(Theme.PAD, (this.height - this.panelHeight) / 2);
		for (int i = 0; i < this.sections.size(); i++) {
			SlotSection section = this.sections.get(i);
			int[] offset = offsets.get(i);
			SectionView view = this.add(new SectionView(this, section));
			view.position(this.panelX + (this.panelWidth - section.layout().width()) / 2, this.panelY + offset[1]);
			view.labelY(this.panelY + offset[0]);
			this.views.add(view);
		}
		if (hasSide) {
			this.sideLines = side == null ? List.of() : this.host().wrap(side, sideWidth);
			int textHeight = this.sideLines.isEmpty() ? 0 : this.sideLines.size() * 9 + SIDE_SPACING + 1;
			int sideHeight = textHeight + this.sideButtons.size() * (Theme.BUTTON_HEIGHT + SIDE_SPACING) - SIDE_SPACING;
			this.sideY = Math.max(Theme.PAD, this.panelY + (this.panelHeight - sideHeight) / 2);
			int buttonY = this.sideY + textHeight;
			for (Widget button : this.sideButtons) {
				button.bounds(this.sideX, buttonY, sideWidth, Theme.BUTTON_HEIGHT);
				buttonY += Theme.BUTTON_HEIGHT + SIDE_SPACING;
			}
		} else {
			this.sideLines = List.of();
		}
	}

	protected void buildSide() {
	}

	protected Text sideText() {
		return null;
	}

	protected InventoryStatus status() {
		return null;
	}

	protected Button sideButton(Text label, Runnable onPress) {
		return this.sideWidget(new Button(label, onPress));
	}

	protected <T extends Widget> T sideWidget(T widget) {
		T added = this.add(widget);
		this.sideButtons.add(added);
		return added;
	}

	protected boolean slotPressed(SlotRef ref, MouseInput input) {
		return false;
	}

	@Override
	protected void renderBackground(Canvas canvas, int mouseX, int mouseY, float delta) {
		DrawUtil.containerPanel(canvas, this.panelX, this.panelY, this.panelWidth, this.panelHeight);
		int labelX = this.panelX + 8;
		int maxWidth = this.panelWidth - 16;
		for (int i = 0; i < this.views.size(); i++) {
			SectionView view = this.views.get(i);
			Text label = view.section().label();
			if (label == null) {
				continue;
			}
			int width = maxWidth;
			if (i == 0) {
				width -= this.renderStatus(canvas, view.labelY());
			}
			canvas.text(canvas.trimToWidth(label, width), labelX, view.labelY(), Theme.CONTAINER_LABEL, false);
		}
		for (int i = 0; i < this.sideLines.size(); i++) {
			canvas.text(this.sideLines.get(i), this.sideX, this.sideY + i * 9, Theme.LABEL, false);
		}
	}

	private int renderStatus(Canvas canvas, int y) {
		InventoryStatus status = this.status();
		if (status == null) {
			return 0;
		}
		int statusWidth = canvas.textWidth(status.text());
		int statusX = this.panelX + this.panelWidth - 8 - statusWidth;
		canvas.text(status.text(), statusX, y, status.color(), false);
		int used = statusWidth + STATUS_GAP;
		if (status.sprite() != null) {
			int spriteWidth = DrawUtil.spriteWidth(status.sprite());
			DrawUtil.sprite(canvas, status.sprite(), statusX - 2 - spriteWidth, y + 1, status.color());
			used += spriteWidth + 2;
		}
		return used;
	}

	@Override
	public void renderScreen(Canvas canvas, int mouseX, int mouseY, float delta) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		super.renderScreen(canvas, mouseX, mouseY, delta);
		if (this.topOverlay() != null) {
			return;
		}
		if (InventoryUtil.present(this.cursor)) {
			canvas.nextLayer();
			canvas.item(this.cursor, mouseX - 8, mouseY - 8);
			return;
		}
		SlotRef hovered = this.slotAt(mouseX, mouseY);
		if (hovered != null && InventoryUtil.present(hovered.item())) {
			List<Text> extra = this.slotTooltip(hovered);
			if (extra.isEmpty()) {
				canvas.itemTooltip(hovered.item(), mouseX, mouseY);
			} else {
				List<Text> lines = new ArrayList<>(hovered.item().tooltip(false));
				lines.addAll(extra);
				canvas.tooltip(lines, mouseX, mouseY);
			}
		}
	}

	protected List<Text> slotTooltip(SlotRef ref) {
		return List.of();
	}

	public SlotRef slotAt(double mouseX, double mouseY) {
		for (SectionView view : this.views) {
			int index = view.indexAt(mouseX, mouseY);
			if (index >= 0) {
				return new SlotRef(view.section(), index);
			}
		}
		return null;
	}

	public SlotRef hovered() {
		return this.slotAt(this.mouseX, this.mouseY);
	}

	@Override
	public HoveredItem hoveredItem() {
		SlotRef ref = this.hovered();
		if (ref == null || !InventoryUtil.present(ref.item())) {
			return null;
		}
		Target target = ref.section().target(ref.index());
		return new HoveredItem(ref.item(), target, target == null ? HoveredItem.NO_SLOT : target.inventorySlot());
	}

	public boolean dragging(SlotSection section, int index) {
		return this.dragSlots.size() > 1 && this.dragSlots.contains(new SlotRef(section, index));
	}

	void press(SlotRef ref, MouseInput input) {
		if (this.topOverlay() != null || this.slotPressed(ref, input)) {
			return;
		}
		if (input.middle()) {
			this.cloneStack(ref);
			return;
		}
		if (input.modifiers().primary() && input.left() && this.combine(ref)) {
			return;
		}
		if (input.modifiers().shift()) {
			this.quickMove(ref);
			return;
		}
		if (input.left() && input.doubleClick() && InventoryUtil.present(this.cursor)) {
			this.collect();
			return;
		}
		if (InventoryUtil.present(this.cursor) && ref.section().mode() == SlotMode.NORMAL && (input.left() || input.right())) {
			this.dragButton = input.button();
			this.dragOrigin = ref;
			this.dragSlots.clear();
			if (this.dragEligible(ref)) {
				this.dragSlots.add(ref);
			}
			return;
		}
		this.click(ref, input.right());
	}

	void dragOver(double mouseX, double mouseY) {
		if (this.dragButton == null) {
			return;
		}
		SlotRef ref = this.slotAt(mouseX, mouseY);
		if (ref != null && !this.dragSlots.contains(ref) && this.dragEligible(ref) && this.dragSlots.size() < this.cursor.count()) {
			this.dragSlots.add(ref);
		}
	}

	void release() {
		if (this.dragButton == null) {
			return;
		}
		boolean right = this.dragButton == MouseButton.RIGHT;
		SlotRef origin = this.dragOrigin;
		List<SlotRef> slots = new ArrayList<>(this.dragSlots);
		this.dragButton = null;
		this.dragOrigin = null;
		this.dragSlots.clear();
		if (slots.size() <= 1) {
			this.click(origin, right);
		} else {
			this.distribute(slots, right);
		}
	}

	private boolean dragEligible(SlotRef ref) {
		if (!ref.writable()) {
			return false;
		}
		GameItem item = ref.item();
		if (!InventoryUtil.present(item)) {
			return true;
		}
		return InventoryUtil.stackable(item, this.cursor) && item.count() < InventoryUtil.limit(this.items(), item, this.cursor);
	}

	private void distribute(List<SlotRef> slots, boolean right) {
		int remaining = this.cursor.count();
		int each = right ? 1 : Math.max(1, remaining / slots.size());
		for (SlotRef ref : slots) {
			if (remaining <= 0) {
				break;
			}
			GameItem item = ref.item();
			int existing = InventoryUtil.present(item) ? item.count() : 0;
			int space = InventoryUtil.limit(this.items(), item, this.cursor) - existing;
			int put = Math.min(Math.min(each, space), remaining);
			if (put <= 0) {
				continue;
			}
			ref.section().set(ref.index(), this.cursor.withCount(existing + put));
			remaining -= put;
		}
		this.setCursor(InventoryUtil.withCount(this.items(), this.cursor, remaining));
	}

	protected void click(SlotRef ref, boolean right) {
		SlotSection section = ref.section();
		GameItem item = ref.item();
		boolean holding = InventoryUtil.present(this.cursor);
		boolean present = InventoryUtil.present(item);
		if (section.mode() == SlotMode.READ_ONLY) {
			return;
		}
		if (section.mode() == SlotMode.SOURCE) {
			if (!present) {
				return;
			}
			if (!holding) {
				this.setCursor(item);
			} else if (InventoryUtil.stackable(this.cursor, item)) {
				int limit = InventoryUtil.limit(this.items(), this.cursor, item);
				this.setCursor(this.cursor.withCount(Math.min(limit, this.cursor.count() + (right ? 1 : item.count()))));
			} else {
				this.lose(this.cursor, LostItemReason.CURSOR);
				this.setCursor(item);
			}
			return;
		}
		if (!section.writable(ref.index())) {
			return;
		}
		ItemService items = this.items();
		if (!holding) {
			if (!present) {
				return;
			}
			int take = right ? (item.count() + 1) / 2 : item.count();
			this.setCursor(item.withCount(take));
			section.set(ref.index(), InventoryUtil.withCount(items, item, item.count() - take));
		} else if (!present) {
			int put = right ? 1 : this.cursor.count();
			section.set(ref.index(), this.cursor.withCount(put));
			this.setCursor(InventoryUtil.withCount(items, this.cursor, this.cursor.count() - put));
		} else if (InventoryUtil.stackable(item, this.cursor)) {
			int space = InventoryUtil.limit(items, item, this.cursor) - item.count();
			int put = Math.min(right ? 1 : this.cursor.count(), space);
			if (put > 0) {
				section.set(ref.index(), item.withCount(item.count() + put));
				this.setCursor(InventoryUtil.withCount(items, this.cursor, this.cursor.count() - put));
			}
		} else {
			section.set(ref.index(), this.cursor);
			this.setCursor(item);
		}
	}

	protected boolean combine(SlotRef ref) {
		GameItem item = ref.item();
		if (!ref.writable() || !InventoryUtil.present(item) || !InventoryUtil.present(this.cursor) || !ENCHANTED_BOOK.equals(this.cursor.id())) {
			return false;
		}
		GameVersion version = this.ctx.platform().game().version();
		Map<String, Integer> stored = ItemFields.STORED_ENCHANTMENTS.get(this.cursor.tag(), version);
		if (stored == null || stored.isEmpty()) {
			return false;
		}
		Field<Map<String, Integer>> field = ENCHANTED_BOOK.equals(item.id()) ? ItemFields.STORED_ENCHANTMENTS : ItemFields.ENCHANTMENTS;
		CompoundTag tag = item.tag().copy();
		Map<String, Integer> current = field.get(tag, version);
		Map<String, Integer> next = current == null ? new LinkedHashMap<>() : new LinkedHashMap<>(current);
		stored.forEach((id, level) -> next.merge(id, level, Math::max));
		field.write(tag, version, next);
		ref.section().set(ref.index(), this.items().fromTag(tag));
		if (!this.player().creative()) {
			this.setCursor(InventoryUtil.withCount(this.items(), this.cursor, this.cursor.count() - 1));
		}
		return true;
	}

	private void cloneStack(SlotRef ref) {
		GameItem item = ref.item();
		if (!this.player().creative() || InventoryUtil.present(this.cursor) || !InventoryUtil.present(item)) {
			return;
		}
		this.setCursor(item.withCount(Math.max(item.count(), this.items().maxStackSize(item))));
	}

	protected List<SlotSection> quickTargets(SlotSection from) {
		List<SlotSection> targets = new ArrayList<>();
		boolean fromPlayer = from instanceof PlayerSection;
		for (SlotSection section : this.sections) {
			if (section != from && section.mode() == SlotMode.NORMAL && (section instanceof PlayerSection) != fromPlayer) {
				targets.add(section);
			}
		}
		return targets;
	}

	private void quickMove(SlotRef ref) {
		SlotSection section = ref.section();
		GameItem item = ref.item();
		if (!InventoryUtil.present(item) || section.mode() == SlotMode.READ_ONLY || section.mode() == SlotMode.NORMAL && !ref.writable()) {
			return;
		}
		GameItem rest = item;
		for (SlotSection target : this.quickTargets(section)) {
			rest = this.insert(target, rest);
			if (!InventoryUtil.present(rest)) {
				break;
			}
		}
		if (section.mode() == SlotMode.NORMAL && (!InventoryUtil.present(rest) || rest.count() != item.count())) {
			section.set(ref.index(), InventoryUtil.present(rest) ? rest : this.items().empty());
		}
	}

	protected GameItem insert(SlotSection target, GameItem item) {
		GameItem rest = item;
		ItemService items = this.items();
		for (int i = 0; i < target.size() && InventoryUtil.present(rest); i++) {
			GameItem existing = target.get(i);
			if (!target.writable(i) || !InventoryUtil.stackable(existing, rest)) {
				continue;
			}
			int space = InventoryUtil.limit(items, existing, rest) - existing.count();
			int put = Math.min(space, rest.count());
			if (put > 0) {
				target.set(i, existing.withCount(existing.count() + put));
				rest = InventoryUtil.withCount(items, rest, rest.count() - put);
			}
		}
		for (int i = 0; i < target.size() && InventoryUtil.present(rest); i++) {
			if (target.writable(i) && !InventoryUtil.present(target.get(i))) {
				target.set(i, rest);
				rest = items.empty();
			}
		}
		return InventoryUtil.present(rest) ? rest : items.empty();
	}

	private void collect() {
		ItemService items = this.items();
		int limit = InventoryUtil.limit(items, this.cursor, this.cursor);
		for (int pass = 0; pass < 2; pass++) {
			for (SlotSection section : this.sections) {
				for (int i = 0; i < section.size() && this.cursor.count() < limit; i++) {
					GameItem item = section.get(i);
					if (!section.writable(i) || !InventoryUtil.stackable(item, this.cursor)) {
						continue;
					}
					boolean full = item.count() >= items.maxStackSize(item);
					if (pass == 0 && full) {
						continue;
					}
					int take = Math.min(item.count(), limit - this.cursor.count());
					section.set(i, InventoryUtil.withCount(items, item, item.count() - take));
					this.setCursor(this.cursor.withCount(this.cursor.count() + take));
				}
			}
		}
	}

	private boolean hotbarSwap(SlotRef ref, int hotbar) {
		PlayerSection player = this.playerSection();
		if (player == null) {
			return false;
		}
		int index = player.hotbarIndex(hotbar);
		if (ref.section() == player && ref.index() == index || !player.writable(index)) {
			return true;
		}
		GameItem item = ref.item();
		GameItem current = player.get(index);
		if (ref.section().mode() == SlotMode.SOURCE) {
			if (InventoryUtil.present(item)) {
				this.lose(current, LostItemReason.OVERWRITTEN);
				player.set(index, item);
			}
			return true;
		}
		if (!ref.writable()) {
			return true;
		}
		ref.section().set(ref.index(), current);
		player.set(index, item);
		return true;
	}

	private boolean delete(SlotRef ref) {
		GameItem item = ref.item();
		if (!ref.writable() || !InventoryUtil.present(item)) {
			return false;
		}
		this.lose(item, LostItemReason.DELETED);
		ref.section().set(ref.index(), this.items().empty());
		return true;
	}

	private boolean openSlot(SlotRef ref, Modifiers modifiers) {
		Target target = ref.section().target(ref.index());
		if (target == null || !InventoryUtil.present(ref.item())) {
			return false;
		}
		if (modifiers.primary()) {
			ContainerService containers = this.ctx.services().find(ContainerService.class).orElse(null);
			if (containers == null) {
				return false;
			}
			containers.open(target);
			return true;
		}
		EditorService editor = this.ctx.services().find(EditorService.class).orElse(null);
		if (editor == null) {
			return false;
		}
		editor.open(target);
		return true;
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		SlotRef ref = this.hovered();
		if (ref == null || this.topOverlay() != null) {
			return false;
		}
		if (input.key() >= Key.NUM_1 && input.key() <= Key.NUM_9 && input.modifiers().none()) {
			return this.hotbarSwap(ref, input.key() - Key.NUM_1);
		}
		if (input.plain(Key.DELETE)) {
			return this.delete(ref);
		}
		if (input.is(Key.SPACE)) {
			return this.openSlot(ref, input.modifiers());
		}
		return false;
	}

	protected void lose(GameItem item, LostItemReason reason) {
		if (InventoryUtil.present(item)) {
			this.ctx.hooks().fire(CoreHooks.ITEM_LOST, new ItemLostEvent(item, reason));
		}
	}

	protected void returnCursor() {
		GameItem rest = this.cursor;
		this.setCursor(null);
		if (!InventoryUtil.present(rest)) {
			return;
		}
		PlayerSection player = this.playerSection();
		if (player != null) {
			rest = this.insert(player, rest);
		}
		for (SlotSection section : this.sections) {
			if (InventoryUtil.present(rest) && section != player && section.mode() == SlotMode.NORMAL) {
				rest = this.insert(section, rest);
			}
		}
		if (InventoryUtil.present(rest) && this.player().creative() && this.player().give(rest)) {
			return;
		}
		this.lose(rest, LostItemReason.CURSOR);
	}

	@Override
	public void onRemoved() {
		this.dragButton = null;
		this.dragSlots.clear();
		this.returnCursor();
	}
}
