package com.nick22985.nbteditor.data;

import com.nick22985.nbteditor.nbt.CompoundTag;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public record PotionEffect(String id, int amplifier, int duration, boolean ambient, boolean showParticles, boolean showIcon, CompoundTag extra) {
	public static final int INFINITE = -1;

	public static PotionEffect create(String id) {
		return new PotionEffect(id, 0, 600, false, true, true, new CompoundTag());
	}

	public PotionEffect withId(String id) {
		return new PotionEffect(id, this.amplifier, this.duration, this.ambient, this.showParticles, this.showIcon, this.extra);
	}

	public PotionEffect withAmplifier(int amplifier) {
		return new PotionEffect(this.id, amplifier, this.duration, this.ambient, this.showParticles, this.showIcon, this.extra);
	}

	public PotionEffect withDuration(int duration) {
		return new PotionEffect(this.id, this.amplifier, duration, this.ambient, this.showParticles, this.showIcon, this.extra);
	}

	public PotionEffect withAmbient(boolean ambient) {
		return new PotionEffect(this.id, this.amplifier, this.duration, ambient, this.showParticles, this.showIcon, this.extra);
	}

	public PotionEffect withShowParticles(boolean showParticles) {
		return new PotionEffect(this.id, this.amplifier, this.duration, this.ambient, showParticles, this.showIcon, this.extra);
	}

	public PotionEffect withShowIcon(boolean showIcon) {
		return new PotionEffect(this.id, this.amplifier, this.duration, this.ambient, this.showParticles, showIcon, this.extra);
	}
}
