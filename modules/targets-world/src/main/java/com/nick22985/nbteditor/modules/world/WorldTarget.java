package com.nick22985.nbteditor.modules.world;

import com.nick22985.nbteditor.module.ModuleContext;
import com.nick22985.nbteditor.target.SubjectKind;
import com.nick22985.nbteditor.target.Target;
import com.nick22985.nbteditor.target.TargetCapability;
import com.nick22985.nbteditor.target.TargetException;
import com.nick22985.nbteditor.target.WorldBackend;
import com.nick22985.nbteditor.target.WorldBackends;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public abstract class WorldTarget implements Target {
	protected final ModuleContext ctx;

	protected WorldTarget(ModuleContext ctx) {
		this.ctx = ctx;
	}

	protected Optional<WorldBackend> backend(TargetCapability capability) {
		return this.ctx.services().get(WorldBackends.class).best(this.kind(), capability);
	}

	protected boolean clientReadable() {
		return false;
	}

	protected <T> CompletableFuture<T> failure(String key, Object... args) {
		return CompletableFuture.failedFuture(new TargetException(this.ctx.tr(key, args)));
	}

	protected WorldBackend writer() {
		return this.backend(TargetCapability.WRITE).orElseThrow(() -> new TargetException(this.ctx.tr("no_backend")));
	}

	@Override
	public Set<TargetCapability> capabilities() {
		Set<TargetCapability> capabilities = EnumSet.noneOf(TargetCapability.class);
		Optional<WorldBackend> reader = this.backend(TargetCapability.READ);
		if (reader.isPresent() || this.clientReadable()) {
			capabilities.add(TargetCapability.READ);
		}
		if (reader.isPresent()) {
			capabilities.add(TargetCapability.REMOTE);
		}
		if (this.backend(TargetCapability.WRITE).isPresent()) {
			capabilities.add(TargetCapability.WRITE);
		}
		return capabilities;
	}

	@Override
	public abstract SubjectKind kind();
}
