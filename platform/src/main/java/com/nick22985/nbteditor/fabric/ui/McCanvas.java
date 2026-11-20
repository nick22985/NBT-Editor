package com.nick22985.nbteditor.fabric.ui;

import com.nick22985.nbteditor.fabric.item.FabricGameItem;
import com.nick22985.nbteditor.fabric.util.McIdUtil;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import com.nick22985.nbteditor.ui.Canvas;
import com.nick22985.nbteditor.ui.SlotIcon;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
//? if >=1.20 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
*///?}
//? if >=1.21.6 {
import net.minecraft.client.renderer.RenderPipelines;
//?} else if >=1.21.2
//import net.minecraft.client.renderer.RenderType;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class McCanvas extends McTextMetrics implements Canvas {
	private static final int WHITE = 0xFFFFFFFF;
	private static final int NEAR_WHITE = 0xFFFEFEFE;
	private static final Identifier CONTAINER = McIdUtil.of("minecraft", "textures/gui/container/generic_54.png");
	//? if <1.21.4 {
	/*private static final Identifier BREWING_STAND = McIdUtil.of("minecraft", "textures/gui/container/brewing_stand.png");
	*///?}
	//? if <1.20.2 {
	/*private static final Identifier HORSE = McIdUtil.of("minecraft", "textures/gui/container/horse.png");
	*///?}
	//? if >=1.20.2 {
	private static final Identifier BUTTON = McIdUtil.of("minecraft", "widget/button");
	private static final Identifier BUTTON_HIGHLIGHTED = McIdUtil.of("minecraft", "widget/button_highlighted");
	private static final Identifier BUTTON_DISABLED = McIdUtil.of("minecraft", "widget/button_disabled");
	//?} else
	//private static final Identifier WIDGETS = McIdUtil.of("minecraft", "textures/gui/widgets.png");

	//? if >=1.20 {
	private final GuiGraphicsExtractor graphics;
	//?} else
	//private final PoseStack graphics;
	private final Screen screen;
	private final float partialTick;
	private final Deque<int[]> offsets = new ArrayDeque<>();
	private final Deque<int[]> clips = new ArrayDeque<>();
	private int offsetX;
	private int offsetY;

	//? if >=1.20 {
	public McCanvas(GuiGraphicsExtractor graphics, Screen screen, float partialTick) {
	//?} else
	//public McCanvas(PoseStack graphics, Screen screen, float partialTick) {
		this.graphics = graphics;
		this.screen = screen;
		this.partialTick = partialTick;
	}

	public void begin() {
		//? if >=1.20 && <1.21.6
		//this.graphics.pose().pushPose();
		//? if <1.20
		//this.graphics.pushPose();
	}

	public void end() {
		while (!this.clips.isEmpty()) {
			this.popClip();
		}
		//? if >=1.20 && <1.21.6
		//this.graphics.pose().popPose();
		//? if <1.20
		//this.graphics.popPose();
	}

	private static int opaque(int color) {
		return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
	}

	@Override
	public int width() {
		return this.screen.width;
	}

	@Override
	public int height() {
		return this.screen.height;
	}

	@Override
	public float partialTick() {
		return this.partialTick;
	}

	@Override
	public void fill(int x1, int y1, int x2, int y2, int rawColor) {
		int color = this.fillColor(rawColor);
		int ox = this.offsetX;
		int oy = this.offsetY;
		//? if >=1.20 {
		this.graphics.fill(x1 + ox, y1 + oy, x2 + ox, y2 + oy, color);
		//?} else
		//GuiComponent.fill(this.graphics, x1 + ox, y1 + oy, x2 + ox, y2 + oy, color);
	}

	private int fillColor(int color) {
		return color == WHITE ? NEAR_WHITE : color;
	}

	@Override
	public void gradient(int x1, int y1, int x2, int y2, int rawTop, int rawBottom) {
		int colorTop = this.fillColor(rawTop);
		int colorBottom = this.fillColor(rawBottom);
		int ox = this.offsetX;
		int oy = this.offsetY;
		//? if >=1.20 {
		this.graphics.fillGradient(x1 + ox, y1 + oy, x2 + ox, y2 + oy, colorTop, colorBottom);
		//?} else {
		/*if (this.screen instanceof HostScreen host) {
			host.legacyGradient(this.graphics, x1 + ox, y1 + oy, x2 + ox, y2 + oy, colorTop, colorBottom);
		} else {
			GuiComponent.fill(this.graphics, x1 + ox, y1 + oy, x2 + ox, y2 + oy, colorTop);
		}
		*///?}
	}

	@Override
	public void text(String text, int x, int y, int color, boolean shadow) {
		int drawX = x + this.offsetX;
		int drawY = y + this.offsetY;
		int argb = opaque(color);
		//? if >=26.1 {
		this.graphics.text(this.font(), text, drawX, drawY, argb, shadow);
		//?} else if >=1.20 {
		/*this.graphics.drawString(this.font(), text, drawX, drawY, argb, shadow);
		*///?} else {
		/*if (shadow) {
			this.font().drawShadow(this.graphics, text, drawX, drawY, argb);
		} else {
			this.font().draw(this.graphics, text, drawX, drawY, argb);
		}
		*///?}
	}

	@Override
	public void text(Text text, int x, int y, int color, boolean shadow) {
		this.sequence(McTextUtil.sequence(text), x, y, color, shadow);
	}

	private void sequence(FormattedCharSequence text, int x, int y, int color, boolean shadow) {
		int drawX = x + this.offsetX;
		int drawY = y + this.offsetY;
		int argb = opaque(color);
		//? if >=26.1 {
		this.graphics.text(this.font(), text, drawX, drawY, argb, shadow);
		//?} else if >=1.20 {
		/*this.graphics.drawString(this.font(), text, drawX, drawY, argb, shadow);
		*///?} else {
		/*if (shadow) {
			this.font().drawShadow(this.graphics, text, drawX, drawY, argb);
		} else {
			this.font().draw(this.graphics, text, drawX, drawY, argb);
		}
		*///?}
	}

	@Override
	public void sprite(String id, int x, int y, int width, int height) {
		Identifier sprite = McIdUtil.tryParse(id);
		if (sprite == null) {
			return;
		}
		this.blitSprite(sprite, x, y, width, height);
	}

	private void blitSprite(Identifier sprite, int x, int y, int width, int height) {
		int drawX = x + this.offsetX;
		int drawY = y + this.offsetY;
		//? if >=1.21.6 {
		this.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, drawX, drawY, width, height);
		//?} else if >=1.21.2 {
		/*this.graphics.blitSprite(RenderType::guiTextured, sprite, drawX, drawY, width, height);
		*///?} else if >=1.20.2 {
		/*this.graphics.blitSprite(sprite, drawX, drawY, width, height);
		*///?} else {
		/*Identifier texture = McIdUtil.of(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png");
		this.blit(texture, x, y, 0, 0, width, height, width, height);
		*///?}
	}

	@Override
	public void texture(String id, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		Identifier texture = McIdUtil.tryParse(id);
		if (texture != null) {
			this.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
		}
	}

	private void blit(Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		int drawX = x + this.offsetX;
		int drawY = y + this.offsetY;
		//? if >=1.21.6 {
		this.graphics.blit(RenderPipelines.GUI_TEXTURED, texture, drawX, drawY, u, v, width, height, textureWidth, textureHeight);
		//?} else if >=1.21.2 {
		/*this.graphics.blit(RenderType::guiTextured, texture, drawX, drawY, u, v, width, height, textureWidth, textureHeight);
		*///?} else if >=1.20 {
		/*this.graphics.blit(texture, drawX, drawY, u, v, width, height, textureWidth, textureHeight);
		*///?} else {
		/*RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		GuiComponent.blit(this.graphics, drawX, drawY, u, v, width, height, textureWidth, textureHeight);
		RenderSystem.disableBlend();
		*///?}
	}

	@Override
	public void button(int x, int y, int width, int height, boolean hovered, boolean enabled) {
		//? if >=1.20.2 {
		this.blitSprite(!enabled ? BUTTON_DISABLED : hovered ? BUTTON_HIGHLIGHTED : BUTTON, x, y, width, height);
		//?} else {
		/*int v = !enabled ? 46 : hovered ? 86 : 66;
		int leftWidth = width / 2;
		int rightWidth = width - leftWidth;
		int topHeight = height / 2;
		int bottomHeight = height - topHeight;
		this.blit(WIDGETS, x, y, 0, v, leftWidth, topHeight, 256, 256);
		this.blit(WIDGETS, x + leftWidth, y, 200 - rightWidth, v, rightWidth, topHeight, 256, 256);
		this.blit(WIDGETS, x, y + topHeight, 0, v + 20 - bottomHeight, leftWidth, bottomHeight, 256, 256);
		this.blit(WIDGETS, x + leftWidth, y + topHeight, 200 - rightWidth, v + 20 - bottomHeight, rightWidth, bottomHeight, 256, 256);
		*///?}
	}

	@Override
	public void slot(int x, int y) {
		this.blit(CONTAINER, x, y, 7, 17, 18, 18, 256, 256);
	}

	@Override
	public void slotIcon(SlotIcon icon, int x, int y) {
		if (icon == null || icon == SlotIcon.NONE) {
			return;
		}
		//? if >=1.21.4 {
		this.blitSprite(McIdUtil.of("minecraft", "container/slot/" + icon.serializedName()), x + 1, y + 1, 16, 16);
		//?} else {
		/*switch (icon) {
			case HELMET, CHESTPLATE, LEGGINGS, BOOTS, SHIELD -> this.blit(McIdUtil.of("minecraft", "textures/item/empty_armor_slot_" + icon.serializedName() + ".png"), x + 1, y + 1, 0, 0, 16, 16, 16, 16);
			case SWORD -> this.swordIcon(x, y);
			case POTION -> this.blit(BREWING_STAND, x + 1, y + 1, 57, 52, 16, 16, 256, 256);
			case BREWING_FUEL -> this.blit(BREWING_STAND, x + 1, y + 1, 18, 18, 16, 16, 256, 256);
			default -> this.horseIcon(icon, x, y);
		}
		*///?}
	}
	//? if <1.21.4 {

	/*private void swordIcon(int x, int y) {
		//? if >=1.19.4
		/^this.blit(McIdUtil.of("minecraft", "textures/item/empty_slot_sword.png"), x + 1, y + 1, 0, 0, 16, 16, 16, 16);^/
	}

	private void horseIcon(SlotIcon icon, int x, int y) {
		//? if >=1.20.2 {
		/^String name = icon == SlotIcon.SADDLE ? "saddle_slot" : icon == SlotIcon.LLAMA_ARMOR ? "llama_armor_slot" : "armor_slot";
		this.blitSprite(McIdUtil.of("minecraft", "container/horse/" + name), x, y, 18, 18);
		^///?} else {
		/^int u = icon == SlotIcon.SADDLE ? 18 : icon == SlotIcon.LLAMA_ARMOR ? 36 : 0;
		this.blit(HORSE, x, y, u, 220, 18, 18, 256, 256);
		^///?}
	}
	*///?}

	@Override
	public void item(GameItem item, int x, int y, float scale, boolean decorations) {
		ItemStack stack = FabricGameItem.stackOf(item);
		if (stack.isEmpty()) {
			return;
		}
		int drawX = x + this.offsetX;
		int drawY = y + this.offsetY;
		Minecraft mc = Minecraft.getInstance();
		//? if >=1.21.6 {
		this.graphics.pose().pushMatrix();
		this.graphics.pose().translate(drawX, drawY);
		this.graphics.pose().scale(scale, scale);
		//?} else if >=1.20 {
		/*this.graphics.pose().pushPose();
		this.graphics.pose().translate(drawX, drawY, 0.0F);
		this.graphics.pose().scale(scale, scale, 1.0F);
		*///?} else if >=1.19.4 {
		/*this.graphics.pushPose();
		this.graphics.translate(drawX, drawY, 0.0F);
		this.graphics.scale(scale, scale, 1.0F);
		*///?} else {
		/*PoseStack modelView = RenderSystem.getModelViewStack();
		modelView.pushPose();
		modelView.mulPoseMatrix(this.graphics.last().pose());
		modelView.translate(drawX, drawY, 0.0);
		modelView.scale(scale, scale, 1.0F);
		RenderSystem.applyModelViewMatrix();
		*///?}
		//? if >=26.1 {
		this.graphics.item(stack, 0, 0);
		if (decorations) {
			this.graphics.itemDecorations(this.font(), stack, 0, 0);
		}
		//?} else if >=1.20 {
		/*this.graphics.renderItem(stack, 0, 0);
		if (decorations) {
			this.graphics.renderItemDecorations(this.font(), stack, 0, 0);
		}
		*///?} else if >=1.19.4 {
		/*mc.getItemRenderer().renderAndDecorateItem(this.graphics, stack, 0, 0);
		if (decorations) {
			mc.getItemRenderer().renderGuiItemDecorations(this.graphics, this.font(), stack, 0, 0);
		}
		*///?} else {
		/*mc.getItemRenderer().renderAndDecorateItem(stack, 0, 0);
		if (decorations) {
			mc.getItemRenderer().renderGuiItemDecorations(this.font(), stack, 0, 0);
		}
		*///?}
		//? if >=1.21.6 {
		this.graphics.pose().popMatrix();
		//?} else if >=1.20 {
		/*this.graphics.pose().popPose();
		*///?} else if >=1.19.4 {
		/*this.graphics.popPose();
		*///?} else {
		/*modelView.popPose();
		RenderSystem.applyModelViewMatrix();
		*///?}
	}

	@Override
	public void tooltip(List<Text> lines, int x, int y) {
		List<FormattedCharSequence> sequences = new ArrayList<>(lines.size());
		for (Text line : lines) {
			sequences.add(McTextUtil.sequence(line));
		}
		int drawX = x + this.offsetX;
		int drawY = y + this.offsetY;
		//? if >=1.21.6 {
		this.graphics.setTooltipForNextFrame(this.font(), sequences, drawX, drawY);
		//?} else if >=1.20 {
		/*this.graphics.renderTooltip(this.font(), sequences, drawX, drawY);
		*///?} else
		//this.screen.renderTooltip(this.graphics, sequences, drawX, drawY);
	}

	@Override
	public void itemTooltip(GameItem item, int x, int y) {
		ItemStack stack = FabricGameItem.stackOf(item);
		if (stack.isEmpty()) {
			return;
		}
		int drawX = x + this.offsetX;
		int drawY = y + this.offsetY;
		//? if >=1.21.6 {
		this.graphics.setTooltipForNextFrame(this.font(), stack, drawX, drawY);
		//?} else if >=1.20 {
		/*this.graphics.renderTooltip(this.font(), stack, drawX, drawY);
		*///?} else {
		/*if (this.screen instanceof HostScreen host) {
			host.legacyItemTooltip(this.graphics, stack, drawX, drawY);
		}
		*///?}
	}

	@Override
	public void pushClip(int x, int y, int width, int height) {
		int x1 = x + this.offsetX;
		int y1 = y + this.offsetY;
		int x2 = x1 + Math.max(0, width);
		int y2 = y1 + Math.max(0, height);
		int[] parent = this.clips.peek();
		if (parent != null) {
			x1 = Math.max(x1, parent[0]);
			y1 = Math.max(y1, parent[1]);
			x2 = Math.max(x1, Math.min(x2, parent[2]));
			y2 = Math.max(y1, Math.min(y2, parent[3]));
		}
		int[] clip = {x1, y1, x2, y2};
		this.clips.push(clip);
		//? if >=1.20 {
		this.graphics.enableScissor(x1, y1, x2, y2);
		//?} else
		//this.applyScissor(clip);
	}

	@Override
	public void popClip() {
		if (this.clips.isEmpty()) {
			return;
		}
		this.clips.pop();
		//? if >=1.20 {
		this.graphics.disableScissor();
		//?} else {
		/*int[] top = this.clips.peek();
		if (top == null) {
			RenderSystem.disableScissor();
		} else {
			this.applyScissor(top);
		}
		*///?}
	}

	//? if <1.20 {
	/*private void applyScissor(int[] clip) {
		double scale = Minecraft.getInstance().getWindow().getGuiScale();
		int windowHeight = Minecraft.getInstance().getWindow().getHeight();
		int width = clip[2] - clip[0];
		int height = clip[3] - clip[1];
		RenderSystem.enableScissor((int) (clip[0] * scale), (int) (windowHeight - clip[3] * scale), (int) (width * scale), (int) (height * scale));
	}
	*///?}

	@Override
	public void pushOffset(int dx, int dy) {
		this.offsets.push(new int[] {this.offsetX, this.offsetY});
		this.offsetX += dx;
		this.offsetY += dy;
	}

	@Override
	public void popOffset() {
		int[] previous = this.offsets.poll();
		if (previous != null) {
			this.offsetX = previous[0];
			this.offsetY = previous[1];
		}
	}

	@Override
	public void nextLayer() {
		//? if >=1.21.6 {
		this.graphics.nextStratum();
		//?} else if >=1.20 {
		/*this.graphics.pose().translate(0.0F, 0.0F, 200.0F);
		*///?} else
		//this.graphics.translate(0.0, 0.0, 200.0);
	}
}
