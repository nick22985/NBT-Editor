package com.nick22985.nbteditor.modules.files;

import com.nick22985.nbteditor.hook.CoreHooks;
import com.nick22985.nbteditor.hook.DropScreen;
import com.nick22985.nbteditor.hook.FilesDropEvent;
import com.nick22985.nbteditor.module.Module;
import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.text.Formatting;
import com.nick22985.nbteditor.text.Style;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FilesModule implements Module {
	@Override
	public String id() {
		return "files";
	}

	@Override
	public boolean clientOnly() {
		return true;
	}

	@Override
	public void init(ModuleContext ctx) {
		FilesSettings settings = new FilesSettings(ctx);
		FileBrowser browser = new FileBrowser(ctx, settings);
		Importer importer = new Importer(ctx, browser);
		new FileCommands(ctx, browser, importer, new Exporter(ctx, browser)).register();
		ctx.hooks().on(CoreHooks.FILES_DROP, event -> this.dropped(ctx, settings, importer, event));
	}

	private void dropped(ModuleContext ctx, FilesSettings settings, Importer importer, FilesDropEvent event) {
		if (!settings.dropImport() || event.screen() != DropScreen.CONTAINER && event.screen() != DropScreen.PAUSE) {
			return;
		}
		event.cancel();
		importer.importFiles(event.paths(), importer.defaults(), message -> ctx.client().chat().message(message.error() ? message.text().withStyle(Style.EMPTY.withColor(Formatting.RED)) : message.text()));
	}
}
