package com.nick22985.nbteditor.fabric.client;

import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.item.FabricGameItem;
import com.nick22985.nbteditor.fabric.util.McBlockUtil;
import com.nick22985.nbteditor.fabric.util.McEntityUtil;
import com.nick22985.nbteditor.fabric.util.McTagUtil;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import com.nick22985.nbteditor.nbt.CompoundTag;
import com.nick22985.nbteditor.platform.BlockData;
import com.nick22985.nbteditor.platform.BlockHit;
import com.nick22985.nbteditor.platform.BlockPos;
import com.nick22985.nbteditor.platform.EntityHit;
import com.nick22985.nbteditor.platform.GameItem;
import com.nick22985.nbteditor.platform.Hit;
import com.nick22985.nbteditor.platform.PickListener;
import com.nick22985.nbteditor.platform.WorldService;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricWorldService implements WorldService {
	private ClientLevel level() {
		return Minecraft.getInstance().level;
	}

	@Override
	public boolean present() {
		return this.level() != null;
	}

	@Override
	public String dimension() {
		ClientLevel level = this.level();
		return level == null ? null : level.dimension().identifier().toString();
	}

	@Override
	public Hit targeted(double reach, boolean includeFluids) {
		Minecraft mc = Minecraft.getInstance();
		Entity camera = mc.getCameraEntity();
		ClientLevel level = this.level();
		if (camera == null || level == null) {
			return null;
		}
		float partial = 1.0F;
		HitResult block = camera.pick(reach, partial, includeFluids);
		Vec3 eye = camera.getEyePosition(partial);
		double blockDistance = block == null || block.getType() == HitResult.Type.MISS ? reach * reach : block.getLocation().distanceToSqr(eye);
		Vec3 look = camera.getViewVector(partial);
		Vec3 end = eye.add(look.x * reach, look.y * reach, look.z * reach);
		AABB area = camera.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0, 1.0, 1.0);
		EntityHitResult entity = ProjectileUtil.getEntityHitResult(camera, eye, end, area, candidate -> !candidate.isSpectator() && candidate != camera, blockDistance);
		String dimension = this.dimension();
		if (entity != null) {
			Entity target = entity.getEntity();
			return new EntityHit(dimension, target.getUUID(), target.getId(), McEntityUtil.typeId(target), McTextUtil.fromMc(target.getDisplayName()));
		}
		if (block instanceof BlockHitResult blockHit && block.getType() == HitResult.Type.BLOCK) {
			net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			return new BlockHit(dimension, McBlockUtil.fromMc(pos), McBlockUtil.id(state), level.getBlockEntity(pos) != null);
		}
		return null;
	}

	@Override
	public BlockData clientBlock(BlockPos pos) {
		ClientLevel level = this.level();
		if (level == null) {
			return null;
		}
		net.minecraft.core.BlockPos mcPos = McBlockUtil.toMc(pos);
		BlockState state = level.getBlockState(mcPos);
		BlockEntity blockEntity = level.getBlockEntity(mcPos);
		CompoundTag tag = blockEntity == null ? null : McTagUtil.fromMc(McBlockUtil.saveBlockEntity(blockEntity));
		return new BlockData(McBlockUtil.id(state), McBlockUtil.properties(state), tag);
	}

	@Override
	public CompoundTag clientEntity(int networkId) {
		ClientLevel level = this.level();
		Entity entity = level == null ? null : level.getEntity(networkId);
		return entity == null ? null : McTagUtil.fromMc(McEntityUtil.save(entity));
	}

	@Override
	public boolean isSingleplayer() {
		return Minecraft.getInstance().hasSingleplayerServer();
	}

	@Override
	public String serverAddress() {
		ServerData server = Minecraft.getInstance().getCurrentServer();
		return server == null ? null : server.ip;
	}

	@Override
	public void pickListener(PickListener listener) {
		NbteRuntime.INSTANCE.pickListener(listener);
	}

	@Override
	public List<GameItem> nearbyPlayerItems(double range) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = this.level();
		List<GameItem> items = new ArrayList<>();
		if (level == null || mc.player == null) {
			return items;
		}
		double max = range * range;
		EquipmentSlot[] slots = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		for (AbstractClientPlayer other : level.players()) {
			if (other == mc.player || other.distanceToSqr(mc.player) > max) {
				continue;
			}
			for (EquipmentSlot slot : slots) {
				ItemStack stack = other.getItemBySlot(slot);
				if (!stack.isEmpty()) {
					items.add(FabricGameItem.of(stack));
				}
			}
		}
		return items;
	}
}
