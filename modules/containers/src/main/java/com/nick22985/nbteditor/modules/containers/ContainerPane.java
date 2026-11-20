package com.nick22985.nbteditor.modules.containers;

import com.nick22985.nbteditor.container.ContainerFormat;
import com.nick22985.nbteditor.container.ContainerSubject;
import com.nick22985.nbteditor.editor.EditorFrame;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.FormPane;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.ui.Theme;
import com.nick22985.nbteditor.ui.widget.ItemGrid;
import com.nick22985.nbteditor.ui.widget.Row;
import com.nick22985.nbteditor.util.ErrorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class ContainerPane extends FormPane {
	private static final int SLOT = 18;
	private static final int MAX_COLUMNS = 9;

	private final ModuleContext ctx;
	private final ContainerOpener opener;

	public ContainerPane(EditorSession session, ModuleContext ctx, ContainerOpener opener) {
		super(session);
		this.ctx = ctx;
		this.opener = opener;
	}

	@Override
	protected int headerHeight() {
		return Theme.BUTTON_HEIGHT + Theme.GAP;
	}

	@Override
	protected void buildExtras(EditorFrame frame) {
		this.headerButton(this.ctx.tr("open"), this::openView);
	}

	private void openView() {
		this.session.save().whenComplete((ignored, error) -> this.ctx.platform().scheduler().client(() -> {
			if (error != null) {
				this.ctx.client().screens().toast(this.ctx.tr("save.failed"), ErrorUtil.message(error));
				return;
			}
			this.opener.open(this.session.target());
		}));
	}

	@Override
	protected void rows(List<Row> rows) {
		ContainerSubject subject = this.opener.subject(this.session.kind(), this.session.root(), this.session.version());
		Optional<ContainerFormat> format = this.opener.format(subject);
		if (format.isEmpty()) {
			rows.add(this.note(this.ctx.tr("unsupported"), Theme.FAINT));
			return;
		}
		List<GameItem> items = new ArrayList<>();
		int filled = 0;
		for (CompoundTag tag : format.get().read(subject)) {
			GameItem item = tag.isEmpty() ? this.session.client().items().empty() : this.session.client().items().fromTag(tag);
			items.add(item);
			if (!item.isEmpty()) {
				filled++;
			}
		}
		rows.add(this.header(this.ctx.tr("slots", filled, items.size())));
		int columns = Math.max(1, Math.min(MAX_COLUMNS, (this.rowWidth() - 8) / SLOT));
		ItemGrid grid = new ItemGrid(columns, items.size(), items::get);
		Row row = new Row(grid.rows() * SLOT + 4);
		row.place(grid, 4, 2, columns * SLOT, grid.rows() * SLOT);
		rows.add(row);
		if (!this.session.writable()) {
			rows.add(this.note(this.ctx.tr("status.read_only"), Theme.FAINT));
		}
	}
}
