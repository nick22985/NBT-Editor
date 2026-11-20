package com.nick22985.nbteditor.hook;

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class CoreHooks {
	public static final HookKey<ClientTickEvent> CLIENT_TICK = HookKey.of("client_tick");
	public static final HookKey<WorldJoinEvent> WORLD_JOIN = HookKey.of("world_join");
	public static final HookKey<WorldLeaveEvent> WORLD_LEAVE = HookKey.of("world_leave");
	public static final HookKey<TooltipEvent> ITEM_TOOLTIP = HookKey.of("item_tooltip");
	public static final HookKey<ChatReceiveEvent> CHAT_RECEIVE = HookKey.of("chat_receive");
	public static final HookKey<ServerStartEvent> SERVER_START = HookKey.of("server_start");
	public static final HookKey<ServerStopEvent> SERVER_STOP = HookKey.of("server_stop");
	public static final HookKey<PlayerJoinServerEvent> PLAYER_JOIN_SERVER = HookKey.of("player_join_server");
	public static final HookKey<PlayerLeaveServerEvent> PLAYER_LEAVE_SERVER = HookKey.of("player_leave_server");
	public static final HookKey<ItemLostEvent> ITEM_LOST = HookKey.of("item_lost");
	public static final HookKey<SlotCheckEvent> SLOT_CHECK = HookKey.of("slot_check");
	public static final HookKey<ScreenKeyEvent> SCREEN_KEY = HookKey.of("screen_key");
	public static final HookKey<SlotClickEvent> SLOT_CLICK = HookKey.of("slot_click");
	public static final HookKey<FilesDropEvent> FILES_DROP = HookKey.of("files_drop");

	private CoreHooks() {
	}
}
