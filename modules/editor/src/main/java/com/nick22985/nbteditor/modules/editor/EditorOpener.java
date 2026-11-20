package com.nick22985.nbteditor.modules.editor;

import com.nick22985.nbteditor.config.Setting;
import com.nick22985.nbteditor.editor.EditorService;
import com.nick22985.nbteditor.editor.EditorSession;
import com.nick22985.nbteditor.editor.EditorTab;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.modules.editor.nbt.NbtTab;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.util.ErrorUtil;
import java.util.List;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class EditorOpener implements EditorService {
	private final ModuleContext ctx;
	private final Setting<Integer> historyLimit;

	public EditorOpener(ModuleContext ctx, Setting<Integer> historyLimit) {
		this.ctx = ctx;
		this.historyLimit = historyLimit;
	}

	@Override
	public void open(Target target) {
		this.open(target, null);
	}

	@Override
	public void open(Target target, String tab) {
		target.load().whenComplete((tag, error) -> this.ctx.platform().scheduler().client(() -> {
			if (error != null) {
				this.ctx.log().warn("Could not open " + target.id(), ErrorUtil.unwrap(error));
				this.ctx.client().screens().toast(this.ctx.tr("open.failed"), ErrorUtil.message(error));
				return;
			}
			EditorSession session = new EditorSession(target, tag, this.ctx.platform());
			session.document().setHistoryLimit(this.historyLimit.get());
			List<EditorTab> tabs = this.ctx.registry(EditorTab.REGISTRY).values();
			String selected = tab == null ? NbtTab.ID : tab;
			this.ctx.client().screens().open(new EditorScreen(session, tabs, selected, this.ctx));
		}));
	}
}
