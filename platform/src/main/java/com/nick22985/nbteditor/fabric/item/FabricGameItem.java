package com.nick22985.nbteditor.fabric.item;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.util.McItemUtil;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
//builtin
import net.minecraft.network.chat.Component;
//? if >=1.20.5 {
import net.minecraft.world.item.Item;
//?}
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricGameItem implements GameItem {
	private final ItemStack stack;
	private final List<String> errors;
	private CompoundTag tag;

	public FabricGameItem(ItemStack stack, CompoundTag source, List<String> errors) {
		this.stack = stack;
		this.tag = source == null ? null : source.copy();
		this.errors = List.copyOf(errors);
	}

	public static FabricGameItem of(ItemStack stack) {
		return new FabricGameItem(stack == null ? ItemStack.EMPTY : stack.copy(), null, List.of());
	}

	public static FabricGameItem from(CompoundTag tag) {
		DecodedItem decoded = McItemUtil.decode(tag);
		return new FabricGameItem(decoded.stack(), decoded.errors().isEmpty() ? null : tag, decoded.errors());
	}

	public static ItemStack stackOf(GameItem item) {
		if (item == null) {
			return ItemStack.EMPTY;
		}
		if (item instanceof FabricGameItem fabric) {
			return fabric.stack.copy();
		}
		return McItemUtil.decode(item.tag()).stack();
	}

	public ItemStack stack() {
		return this.stack;
	}

	@Override
	public CompoundTag tag() {
		if (this.tag == null) {
			this.tag = McItemUtil.encode(this.stack);
		}
		return this.tag.copy();
	}

	@Override
	public String id() {
		return BuiltInRegistries.ITEM.getKey(this.stack.getItem()).toString();
	}

	@Override
	public int count() {
		return this.stack.isEmpty() ? 0 : this.stack.getCount();
	}

	@Override
	public boolean isEmpty() {
		return this.stack.isEmpty();
	}

	@Override
	public Text name() {
		return McTextUtil.fromMc(this.stack.getHoverName());
	}

	@Override
	public List<Text> tooltip(boolean advanced) {
		List<Text> lines = new ArrayList<>();
		try {
			for (Component line : this.mcTooltip(advanced)) {
				lines.add(McTextUtil.fromMc(line));
			}
		} catch (RuntimeException e) {
			lines.clear();
			lines.add(this.name());
		}
		return lines;
	}

	public List<Component> mcTooltip(boolean advanced) {
		Minecraft mc = Minecraft.getInstance();
		//? if >=1.19.3 {
		TooltipFlag flag = advanced ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
		//?} else
		//TooltipFlag flag = advanced ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
		//? if >=1.20.5 {
		Item.TooltipContext context = mc.level == null ? Item.TooltipContext.of(NbteRuntime.INSTANCE.registries().get()) : Item.TooltipContext.of(mc.level);
		return this.stack.getTooltipLines(context, mc.player, flag);
		//?} else
		//return this.stack.getTooltipLines(mc.player, flag);
	}

	@Override
	public List<String> errors() {
		return this.errors;
	}

	@Override
	public GameItem withCount(int count) {
		if (this.stack.isEmpty()) {
			return this;
		}
		ItemStack copy = this.stack.copy();
		copy.setCount(count);
		return new FabricGameItem(copy, null, List.of());
	}
}
