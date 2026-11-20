package com.nick22985.nbteditor.modules.editor.nbt;

import com.nick22985.nbteditor.doc.EditException;
import com.nick22985.nbteditor.doc.NbtDocument;
import com.nick22985.nbteditor.doc.Selection;
import com.nick22985.nbteditor.editor.NbtSuggester;
import com.nick22985.nbteditor.modules.editor.tree.TreeRow;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.nbt.ListTag;
import com.nick22985.nbteditor.nbt.NbtPath;
import com.nick22985.nbteditor.nbt.SnbtDialect;
import com.nick22985.nbteditor.nbt.SnbtException;
import com.nick22985.nbteditor.nbt.Tag;
import com.nick22985.nbteditor.nbt.TagType;
import com.nick22985.nbteditor.platform.Scheduler;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.UiScreen;
import com.nick22985.nbteditor.ui.suggest.SuggestionSource;
import com.nick22985.nbteditor.ui.widget.Button;
import com.nick22985.nbteditor.ui.widget.FindBar;
import com.nick22985.nbteditor.ui.widget.MenuOption;
import com.nick22985.nbteditor.ui.widget.ModalPanel;
import com.nick22985.nbteditor.ui.widget.PopoverMenu;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.ui.widget.TextArea;
import com.nick22985.nbteditor.util.ModalUtil;
import com.nick22985.nbteditor.util.NbtPathUtil;
import com.nick22985.nbteditor.util.SnbtUtil;
import com.nick22985.nbteditor.util.TagDisplayUtil;
import com.nick22985.nbteditor.util.TagEditUtil;
import com.nick22985.nbteditor.util.TextStyleUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class NbtActions {
	private static final int TEXT_HEIGHT = 190;
	private static final int ASYNC_PARSE = 100000;
	private static final String DEFAULT_KEY = "value";

	private final NbtPane pane;

	public NbtActions(NbtPane pane) {
		this.pane = pane;
	}

	private NbtDocument document() {
		return this.pane.document();
	}

	private Selection selection() {
		return this.pane.selection();
	}

	private SnbtDialect dialect() {
		return this.pane.session().dialect();
	}

	private UiScreen screen() {
		return this.pane.frame().screen();
	}

	private boolean writable() {
		return this.pane.frame() != null && this.pane.session().writable();
	}

	private boolean editable(NbtPath path) {
		return path.startsWith(this.pane.base()) && !path.equals(this.pane.base());
	}

	public boolean canRename() {
		NbtPath primary = this.selection().primary();
		if (this.selection().size() != 1 || primary == null || !this.editable(primary)) {
			return false;
		}
		return this.document().get(primary.parent()) instanceof CompoundTag;
	}

	public InsertPoint insertionPoint() {
		NbtPath primary = this.selection().primary();
		if (this.selection().size() == 1 && primary != null && primary.startsWith(this.pane.base())) {
			Tag tag = this.document().get(primary);
			if (tag != null && tag.type().isContainer() && (this.pane.model().isExpanded(primary) || TagDisplayUtil.elements(tag).isEmpty() || primary.equals(this.pane.base()))) {
				return new InsertPoint(primary, tag instanceof CompoundTag ? -1 : TagDisplayUtil.elements(tag).size());
			}
			if (tag != null && this.editable(primary)) {
				Object last = primary.last();
				return new InsertPoint(primary.parent(), last instanceof Integer ? (Integer) last + 1 : -1);
			}
		}
		return new InsertPoint(this.pane.base(), -1);
	}

	private void ensureBase() {
		this.ensure(this.pane.base());
	}

	private void ensure(NbtPath path) {
		if (path.isRoot() || this.document().get(path) != null) {
			return;
		}
		this.ensure(path.parent());
		this.document().insert(path, new CompoundTag());
	}

	private boolean run(String label, Runnable body) {
		try {
			this.document().batch(this.pane.label(label), body);
			return true;
		} catch (EditException e) {
			this.pane.status(TextStyleUtil.colored(this.pane.tr("edit_failed", e.getMessage()), Theme.RED));
			return false;
		}
	}

	private SuggestionSource keySuggestions(NbtPath container, String allowed) {
		return new NbtSuggester(this.pane.session()).keys(container, allowed);
	}

	private Function<String, Text> keyValidator(NbtPath container, String allowed) {
		return key -> {
			if (key.isEmpty()) {
				return this.pane.tr("key.empty");
			}
			Tag tag = this.document().get(container);
			if (!key.equals(allowed) && tag instanceof CompoundTag && ((CompoundTag) tag).contains(key)) {
				return this.pane.tr("key.exists");
			}
			return null;
		};
	}

	public void openAddMenu(Button anchor) {
		if (anchor == null) {
			return;
		}
		this.openAddMenu(this.insertionPoint(), anchor.x(), anchor.y(), true);
	}

	public void openAddMenu(InsertPoint point, int x, int y, boolean upwards) {
		if (!this.writable()) {
			return;
		}
		Tag container = this.document().get(point.container());
		TagType fixed = container == null ? null : TagEditUtil.fixedElementType(container, this.dialect());
		if (fixed != null) {
			this.add(point, fixed);
			return;
		}
		List<MenuOption> options = new ArrayList<>();
		for (TagType type : TagDisplayUtil.menuTypes()) {
			options.add(MenuOption.of(TagDisplayUtil.typeName(type), () -> this.add(point, type)).chip(TagDisplayUtil.chip(type), TagDisplayUtil.chipColor(type)));
		}
		this.screen().openOverlay(new PopoverMenu(x, y, upwards, options).columns(2));
	}

	private void add(InsertPoint point, TagType type) {
		Tag tag = TagDisplayUtil.defaultTag(type);
		Tag container = this.document().get(point.container());
		if (container == null || container instanceof CompoundTag) {
			ModalUtil.input(this.screen(), this.pane.tr("add.title", TagDisplayUtil.typeName(type)), "", this.keyValidator(point.container(), null), this.keySuggestions(point.container(), null), key -> this.insert(point.container().child(key), tag));
			return;
		}
		this.insert(point.container().child(point.index()), tag);
	}

	private void insert(NbtPath path, Tag tag) {
		if (!this.run("add", () -> {
			this.ensureBase();
			this.document().insert(path, tag);
		})) {
			return;
		}
		this.selection().select(path);
		this.pane.reveal(path);
		if (!tag.type().isContainer()) {
			this.pane.inspector().focusValue();
		}
	}

	public void rename() {
		if (!this.writable() || !this.canRename()) {
			return;
		}
		NbtPath path = this.selection().primary();
		String from = (String) path.last();
		ModalUtil.input(this.screen(), this.pane.tr("rename.title"), from, this.keyValidator(path.parent(), from), this.keySuggestions(path.parent(), from), to -> {
			if (to.equals(from) || !this.run("rename", () -> this.document().rename(path, to))) {
				return;
			}
			NbtPath renamed = path.withLast(to);
			if (this.pane.model().isExpanded(path)) {
				this.pane.model().collapse(path);
				this.pane.model().expand(renamed);
			}
			this.selection().select(renamed);
			this.pane.reveal(renamed);
		});
	}

	public void duplicate() {
		if (!this.writable()) {
			return;
		}
		List<NbtPath> roots = new ArrayList<>();
		for (NbtPath path : this.selection().roots()) {
			if (this.editable(path)) {
				roots.add(path);
			}
		}
		if (roots.isEmpty()) {
			return;
		}
		List<NbtPath> created = new ArrayList<>();
		boolean ok = this.run("duplicate", () -> {
			for (int i = roots.size() - 1; i >= 0; i--) {
				NbtPath path = roots.get(i);
				Tag tag = this.document().get(path);
				Tag parent = this.document().get(path.parent());
				if (tag == null || parent == null) {
					continue;
				}
				NbtPath target;
				if (parent instanceof CompoundTag) {
					target = path.withLast(TagEditUtil.uniqueKey((CompoundTag) parent, (String) path.last()));
				} else {
					target = path.withLast((Integer) path.last() + 1);
				}
				this.document().insert(target, tag);
				for (int j = 0; j < created.size(); j++) {
					created.set(j, NbtPathUtil.shiftedForInsert(created.get(j), target));
				}
				created.add(0, target);
			}
		});
		if (ok) {
			this.selectAll(created);
		}
	}

	private void selectAll(List<NbtPath> paths) {
		if (paths.isEmpty()) {
			return;
		}
		this.selection().select(paths.get(0));
		for (int i = 1; i < paths.size(); i++) {
			this.selection().toggle(paths.get(i));
		}
		this.pane.reveal(paths.get(0));
	}

	public void copy() {
		List<NbtPath> roots = this.selection().roots();
		if (roots.isEmpty()) {
			return;
		}
		List<ClipEntry> entries = new ArrayList<>();
		boolean keyed = true;
		for (NbtPath path : roots) {
			Tag tag = this.document().get(path);
			if (tag == null) {
				continue;
			}
			Object last = path.last();
			String key = last instanceof String ? (String) last : null;
			keyed &= key != null;
			entries.add(new ClipEntry(key, tag.copy()));
		}
		if (entries.isEmpty()) {
			return;
		}
		String text;
		if (entries.size() == 1) {
			text = SnbtUtil.write(entries.get(0).tag(), this.dialect());
		} else if (keyed) {
			CompoundTag compound = new CompoundTag();
			for (ClipEntry entry : entries) {
				compound.put(TagEditUtil.uniqueKey(compound, entry.key()), entry.tag());
			}
			text = SnbtUtil.write(compound, this.dialect());
		} else {
			ListTag list = new ListTag();
			for (ClipEntry entry : entries) {
				list.add(entry.tag());
			}
			text = SnbtUtil.write(list, this.dialect());
		}
		this.screen().host().setClipboard(text);
		this.pane.clipboard().set(entries, text);
		this.pane.status(this.pane.tr("copied", entries.size()));
	}

	public void cut() {
		if (!this.writable() || this.selection().isEmpty()) {
			return;
		}
		this.copy();
		this.delete();
	}

	public void paste() {
		if (!this.writable()) {
			return;
		}
		String text = this.screen().host().clipboard();
		List<ClipEntry> entries = this.pane.clipboard().matching(text);
		if (entries == null) {
			if (text == null || text.isBlank()) {
				return;
			}
			try {
				entries = List.of(new ClipEntry(null, SnbtUtil.parse(text.trim(), this.dialect())));
			} catch (SnbtException e) {
				this.pane.status(TextStyleUtil.colored(this.pane.tr("paste.invalid"), Theme.RED));
				return;
			}
		}
		InsertPoint point = this.insertionPoint();
		Tag container = this.document().get(point.container());
		if (container == null || container instanceof CompoundTag) {
			if (entries.size() == 1 && entries.get(0).key() == null) {
				Tag tag = entries.get(0).tag();
				ModalUtil.input(this.screen(), this.pane.tr("paste.title"), "", this.keyValidator(point.container(), null), this.keySuggestions(point.container(), null), key -> this.insert(point.container().child(key), tag));
				return;
			}
			this.pasteKeyed(point.container(), entries);
			return;
		}
		List<Tag> tags = new ArrayList<>();
		for (ClipEntry entry : entries) {
			Tag tag = TagEditUtil.adaptElement(container, entry.tag());
			if (!TagEditUtil.acceptsElement(container, tag, this.dialect()) || !tags.isEmpty() && this.dialect() == SnbtDialect.LEGACY && tags.get(0).type() != tag.type()) {
				this.pane.status(TextStyleUtil.colored(this.pane.tr("paste.wrong_type"), Theme.RED));
				return;
			}
			tags.add(tag);
		}
		List<NbtPath> created = new ArrayList<>();
		boolean ok = this.run("paste", () -> {
			for (int i = 0; i < tags.size(); i++) {
				NbtPath path = point.container().child(point.index() + i);
				this.document().insert(path, tags.get(i));
				created.add(path);
			}
		});
		if (ok) {
			this.selectAll(created);
		}
	}

	private void pasteKeyed(NbtPath containerPath, List<ClipEntry> entries) {
		List<NbtPath> created = new ArrayList<>();
		boolean ok = this.run("paste", () -> {
			this.ensureBase();
			for (ClipEntry entry : entries) {
				CompoundTag compound = (CompoundTag) this.document().get(containerPath);
				String key = TagEditUtil.uniqueKey(compound, entry.key() == null ? DEFAULT_KEY : entry.key());
				NbtPath path = containerPath.child(key);
				this.document().insert(path, entry.tag());
				created.add(path);
			}
		});
		if (ok) {
			this.selectAll(created);
		}
	}

	public void delete() {
		if (!this.writable()) {
			return;
		}
		List<NbtPath> order = new ArrayList<>();
		for (NbtPath path : this.selection().removalOrder()) {
			if (this.editable(path)) {
				order.add(path);
			}
		}
		if (order.isEmpty()) {
			return;
		}
		NbtPath primary = this.selection().primary();
		int index = primary == null ? -1 : this.pane.model().indexOf(primary);
		if (!this.run("delete", () -> {
			for (NbtPath path : order) {
				this.document().remove(path);
			}
		})) {
			return;
		}
		List<TreeRow> rows = this.pane.model().rows();
		if (index < 0 || rows.isEmpty()) {
			this.selection().clear();
			return;
		}
		NbtPath next = rows.get(Math.min(index, rows.size() - 1)).path();
		this.selection().select(next);
		this.pane.tree().scrollTo(next);
	}

	public void editAsText() {
		if (this.pane.frame() == null || this.selection().size() > 1) {
			return;
		}
		NbtPath primary = this.selection().primary();
		NbtPath path = primary == null ? this.pane.base() : primary;
		Tag tag = this.document().get(path);
		boolean writable = this.writable() && path.startsWith(this.pane.base());
		Text name = path.equals(this.pane.base()) ? this.pane.tr("root") : Text.literal(NbtPathUtil.formatSegment(path.last()));
		ModalPanel modal = new ModalPanel(this.pane.tr("text.title", name), Theme.MODAL_WIDE);
		Row content = modal.content(new Row(TEXT_HEIGHT), TEXT_HEIGHT);
		TextArea area = new TextArea();
		FindBar find = new FindBar(area, writable);
		content.place(area, 0, 0, 0, TEXT_HEIGHT - find.height() - FindBar.GAP);
		content.place(find, 0, TEXT_HEIGHT - find.height(), 0, find.height());
		area.value(tag == null ? "{}" : SnbtUtil.pretty(tag, this.dialect()));
		area.formatter(this.pane::highlight);
		area.editable(writable);
		area.onChange(value -> area.clearError());
		if (writable) {
			modal.closingAction(Text.translatable("gui.cancel"), null);
			AtomicBoolean parsing = new AtomicBoolean();
			modal.defaultAction(modal.action(this.pane.tr("text.apply"), () -> this.submitText(path, area, modal, parsing)));
		} else {
			modal.hint(this.pane.tr("text.read_only"));
			modal.defaultAction(modal.closingAction(Text.translatable("gui.done"), null));
		}
		this.screen().openOverlay(modal);
		this.screen().setFocus(area);
	}

	private void submitText(NbtPath path, TextArea area, ModalPanel modal, AtomicBoolean parsing) {
		if (parsing.get()) {
			return;
		}
		String text = area.value();
		SnbtDialect dialect = this.dialect();
		if (text.length() < ASYNC_PARSE) {
			try {
				this.finishText(path, area, modal, SnbtUtil.parse(text, dialect));
			} catch (SnbtException e) {
				area.error(e.cursor(), Text.literal(e.getMessage()));
			}
			return;
		}
		parsing.set(true);
		modal.hint(this.pane.tr("text.parsing"));
		Scheduler scheduler = this.pane.ctx().platform().scheduler();
		scheduler.async(() -> {
			try {
				return SnbtUtil.parse(text, dialect);
			} catch (SnbtException e) {
				throw new CompletionException(e);
			}
		}).whenComplete((parsed, error) -> scheduler.client(() -> {
			parsing.set(false);
			modal.hint(null);
			if (!modal.isOpen()) {
				return;
			}
			Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
			if (cause instanceof SnbtException) {
				area.error(((SnbtException) cause).cursor(), Text.literal(cause.getMessage()));
			} else if (cause != null) {
				area.error(0, Text.literal(String.valueOf(cause.getMessage())));
			} else {
				this.finishText(path, area, modal, parsed);
			}
		}));
	}

	private void finishText(NbtPath path, TextArea area, ModalPanel modal, Tag parsed) {
		if (this.applyText(path, area, parsed)) {
			modal.close();
		}
	}

	private boolean applyText(NbtPath path, TextArea area, Tag parsed) {
		Tag value = parsed;
		if (path.equals(this.pane.base()) || path.isRoot()) {
			if (!(parsed instanceof CompoundTag)) {
				area.error(0, this.pane.tr("text.need_compound"));
				return false;
			}
		} else {
			Tag parent = this.document().get(path.parent());
			if (parent != null && !(parent instanceof CompoundTag)) {
				value = TagEditUtil.adaptElement(parent, parsed);
				if (parent.type().isArray() && !TagEditUtil.acceptsElement(parent, value, this.dialect()) || parent instanceof ListTag && this.dialect() == SnbtDialect.LEGACY && ((ListTag) parent).size() > 1 && ((ListTag) parent).get(0).type() != value.type()) {
					area.error(0, this.pane.tr("paste.wrong_type"));
					return false;
				}
			}
		}
		Tag current = this.document().get(path);
		if (value.equals(current)) {
			return true;
		}
		Tag result = value;
		return this.run("edit_text", () -> {
			if (path.equals(this.pane.base())) {
				this.ensure(path.parent());
			} else {
				this.ensureBase();
			}
			if (this.document().get(path) == null) {
				this.document().insert(path, result);
			} else {
				this.document().set(path, result);
			}
		});
	}

	public void contextMenu(TreeRow row, int x, int y) {
		NbtPath path = row.path();
		boolean writable = this.writable();
		boolean single = this.selection().size() == 1;
		Tag tag = row.tag();
		List<MenuOption> options = new ArrayList<>();
		if (tag.type().isContainer()) {
			InsertPoint inside = new InsertPoint(path, tag instanceof CompoundTag ? -1 : TagDisplayUtil.elements(tag).size());
			options.add(MenuOption.of(this.pane.tr("menu.add_inside"), () -> this.openAddMenu(inside, x, y, false)).enabled(writable));
			boolean expanded = this.pane.model().isExpanded(path);
			options.add(MenuOption.of(expanded ? this.pane.tr("menu.collapse") : this.pane.tr("menu.expand"), () -> {
				if (expanded) {
					this.pane.model().collapse(path);
				} else {
					this.pane.model().expandUnder(this.document().root(), path);
				}
				this.pane.rebuildRows();
			}).enabled(row.expandable() && !this.pane.model().searching()));
			options.add(MenuOption.separator());
		}
		options.add(MenuOption.of(this.pane.tr("rename"), this::rename).enabled(writable && this.canRename()));
		options.add(MenuOption.of(this.pane.tr("duplicate"), this::duplicate).enabled(writable));
		options.add(MenuOption.of(this.pane.tr("copy"), this::copy));
		options.add(MenuOption.of(this.pane.tr("menu.cut"), this::cut).enabled(writable));
		options.add(MenuOption.of(this.pane.tr("paste"), this::paste).enabled(writable));
		options.add(MenuOption.of(this.pane.tr("edit_text"), this::editAsText).enabled(single));
		options.add(MenuOption.of(this.pane.tr("menu.copy_path"), () -> {
			this.screen().host().setClipboard(path.toString());
			this.pane.status(this.pane.tr("path_copied"));
		}));
		options.add(MenuOption.separator());
		options.add(MenuOption.of(TextStyleUtil.colored(this.pane.tr("delete"), Theme.RED), this::delete).enabled(writable));
		this.screen().openOverlay(new PopoverMenu(x, y, false, options).minWidth(110));
	}
}
