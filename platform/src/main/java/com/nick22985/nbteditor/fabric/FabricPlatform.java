package com.nick22985.nbteditor.fabric;

import com.nick22985.nbteditor.log.Log;
import com.nick22985.nbteditor.net.Transport;
import com.nick22985.nbteditor.platform.ClientPlatform;
import com.nick22985.nbteditor.platform.GameInfo;
import com.nick22985.nbteditor.platform.Platform;
import com.nick22985.nbteditor.platform.Scheduler;
import com.nick22985.nbteditor.platform.ServerPlatform;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricPlatform implements Platform {
	private final GameInfo game;
	private final Log log;
	private final Scheduler scheduler;
	private final Transport transport;
	private final ClientPlatform client;
	private final ServerPlatform server;

	public FabricPlatform(GameInfo game, Log log, Scheduler scheduler, Transport transport, ClientPlatform client, ServerPlatform server) {
		this.game = game;
		this.log = log;
		this.scheduler = scheduler;
		this.transport = transport;
		this.client = client;
		this.server = server;
	}

	@Override
	public GameInfo game() {
		return this.game;
	}

	@Override
	public Log log() {
		return this.log;
	}

	@Override
	public Scheduler scheduler() {
		return this.scheduler;
	}

	@Override
	public Transport transport() {
		return this.transport;
	}

	@Override
	public ClientPlatform client() {
		return this.client;
	}

	@Override
	public ServerPlatform server() {
		return this.server;
	}
}
