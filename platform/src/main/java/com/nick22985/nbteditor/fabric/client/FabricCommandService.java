package com.nick22985.nbteditor.fabric.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nick22985.nbteditor.command.CommandNode;
import com.nick22985.nbteditor.command.CommandService;
import com.nick22985.nbteditor.fabric.NbteRuntime;
import com.nick22985.nbteditor.fabric.command.BrigadierTreeBuilder;
import com.nick22985.nbteditor.fabric.command.LocalCommandSource;
import com.nick22985.nbteditor.fabric.util.McClientUtil;
import com.nick22985.nbteditor.platform.ChatService;
import com.nick22985.nbteditor.fabric.util.McTextUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
//? if >=1.19 {
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
//?} else {
/*import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
*///?}

/**
 * @author nick22985 Created on 11/20/26 for NBT-Editor
 */
public final class FabricCommandService implements CommandService {
	private final ChatService chat;
	private final List<CommandNode> roots = new ArrayList<>();
	private final CommandDispatcher<LocalCommandSource> local = new CommandDispatcher<>();
	//? if >=1.19
	private CommandDispatcher<FabricClientCommandSource> active;

	public FabricCommandService(ChatService chat) {
		this.chat = chat;
		//? if >=1.19 {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
			this.active = dispatcher;
			for (CommandNode root : this.roots) {
				this.registerClient(dispatcher, root);
			}
		});
		//?}
	}

	@Override
	public void register(CommandNode root) {
		this.roots.add(root);
		BrigadierTreeBuilder<LocalCommandSource> builder = new BrigadierTreeBuilder<>((source, text) -> this.chat.message(McTextUtil.fromMc(text)), (source, text) -> this.chat.message(McTextUtil.fromMc(text.copy().withStyle(ChatFormatting.RED))));
		this.local.register(builder.root(root));
		//? if >=1.19 {
		if (this.active != null) {
			this.registerClient(this.active, root);
		}
		//?} else
		//this.registerClient(ClientCommandManager.DISPATCHER, root);
	}

	private void registerClient(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandNode root) {
		if (dispatcher.getRoot().getChild(root.name()) != null) {
			NbteRuntime.INSTANCE.log().warn("Another mod already registered the client command /" + root.name() + ". If NBT Editor's /" + root.name() + " doesn't run, that mod's command is taking it over.");
		}
		BrigadierTreeBuilder<FabricClientCommandSource> builder = new BrigadierTreeBuilder<>(FabricClientCommandSource::sendFeedback, FabricClientCommandSource::sendError);
		dispatcher.register(builder.root(root));
	}

	@Override
	public void send(String command) {
		String stripped = command.startsWith("/") ? command.substring(1) : command;
		Minecraft mc = McClientUtil.mc();
		mc.execute(() -> {
			if (mc.player == null) {
				return;
			}
			//? if >=1.19.3 {
			ClientPacketListener connection = mc.getConnection();
			if (connection != null) {
				connection.sendCommand(stripped);
			}
			//?} else if >=1.19 {
			/*if (!mc.player.commandUnsigned(stripped)) {
				mc.player.commandSigned(stripped, null);
			}
			*///?} else
			//mc.player.chat("/" + stripped);
		});
	}

	@Override
	public boolean serverKnows(String root) {
		ClientPacketListener connection = McClientUtil.mc().getConnection();
		return connection != null && connection.getCommands().getRoot().getChild(root) != null;
	}

	@Override
	public List<String> serverChildren(String root) {
		ClientPacketListener connection = McClientUtil.mc().getConnection();
		List<String> names = new ArrayList<>();
		if (connection == null || connection.getCommands().getRoot().getChild(root) == null) {
			return names;
		}
		connection.getCommands().getRoot().getChild(root).getChildren().forEach(child -> names.add(child.getName()));
		return names;
	}

	public boolean handles(String command) {
		String stripped = command.startsWith("/") ? command.substring(1) : command;
		int space = stripped.indexOf(' ');
		return this.local.getRoot().getChild(space < 0 ? stripped : stripped.substring(0, space)) != null;
	}

	@Override
	public void execute(String command) {
		String stripped = command.startsWith("/") ? command.substring(1) : command;
		if (!this.handles(stripped)) {
			this.send(stripped);
			return;
		}
		McClientUtil.mc().execute(() -> {
			try {
				this.local.execute(stripped, LocalCommandSource.INSTANCE);
			} catch (CommandSyntaxException e) {
				this.chat.message(McTextUtil.fromMc(McTextUtil.literal(e.getMessage()).copy().withStyle(ChatFormatting.RED)));
			}
		});
	}
}
