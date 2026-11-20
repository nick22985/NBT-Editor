package com.nick22985.nbteditor.fabric.ui;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.mixin.ChatScreenAccessor;
import com.nick22985.nbteditor.fabric.util.McClientUtil;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.text.Text;
import java.util.List;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ChatScreen;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
//import com.mojang.blaze3d.vertex.PoseStack;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McChatWarning {
	public static final int SERVER_LIMIT = 256;
	private static final int PAD = 4;
	private static final int LINE = 12;
	private static final int BOTTOM = 14;
	private static final int BACKGROUND = 0xC0000000;
	private static final int COLOR = 0xFFFFAA00;

	private final ChatScreen screen;
	private final List<Text> lines = List.of(Text.translatable("nbtedit.chat.truncated.1"), Text.translatable("nbtedit.chat.truncated.2", SERVER_LIMIT));

	public McChatWarning(ChatScreen screen) {
		this.screen = screen;
	}

	public void register() {
		//? if >=26.1 {
		ScreenEvents.afterExtract(this.screen).register((target, graphics, mouseX, mouseY, delta) -> this.render(graphics, delta));
		//?} else
		//ScreenEvents.afterRender(this.screen).register((target, graphics, mouseX, mouseY, delta) -> this.render(graphics, delta));
	}

	private boolean shown() {
		String value = ((ChatScreenAccessor) this.screen).nbtedit$input().getValue().trim();
		return value.length() > SERVER_LIMIT && !NbteRuntime.INSTANCE.clientCommand(value);
	}

	//? if >=1.20 {
	private void render(GuiGraphicsExtractor graphics, float delta) {
	//?} else
	//private void render(PoseStack graphics, float delta) {
		if (!NbteRuntime.INSTANCE.longChat() || !this.shown()) {
			return;
		}
		Font font = McClientUtil.mc().font;
		int width = 0;
		for (Text line : this.lines) {
			width = Math.max(width, font.width(McTextUtil.toMc(line)));
		}
		int right = this.screen.width - 2;
		int left = right - width - PAD * 2;
		int top = this.screen.height - BOTTOM - this.lines.size() * LINE - PAD;
		McCanvas canvas = new McCanvas(graphics, this.screen, delta);
		canvas.begin();
		try {
			canvas.nextLayer();
			canvas.fill(left, top, right, this.screen.height - BOTTOM, BACKGROUND);
			for (int i = 0; i < this.lines.size(); i++) {
				canvas.text(this.lines.get(i), left + PAD, top + PAD + i * LINE, COLOR, true);
			}
		} finally {
			canvas.end();
		}
	}
}
