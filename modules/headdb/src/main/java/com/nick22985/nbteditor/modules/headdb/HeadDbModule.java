package com.nick22985.nbteditor.modules.headdb;

import com.nick22985.nbteditor.data.HeadNames;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class HeadDbModule implements Module {
	private HeadDatabase database;

	@Override
	public String id() {
		return "headdb";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		this.database = ctx.services().provide(HeadDatabase.class, new HeadDatabase(ctx, new HeadDbSettings(ctx)));
		ctx.services().provide(HeadNames.class, new HeadNameIndex(this.database, new HeadItems(ctx)));
		new HeadCommands(ctx, this.database).register();
	}

	@Override
	public void shutdown(ModuleContext ctx) {
		if (this.database != null) {
			this.database.stop();
		}
	}
}
