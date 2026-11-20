package io.github.filipovych.filobulus.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.route.LibraryRoute;
import io.github.filipovych.filobulus.route.RouteSelector.LoadResult;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class FiloCommand {
	private FiloCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(build("filobulus"));
		dispatcher.register(build("filo"));
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> build(String name) {
		return ClientCommands.literal(name)
				.executes(FiloCommand::openSettings)
				.then(ClientCommands.literal("load")
						.then(ClientCommands.argument("route", StringArgumentType.word())
								.suggests((context, builder) ->
										SharedSuggestionProvider.suggest(FilobulusClient.routes().ids(), builder))
								.executes(FiloCommand::load)))
				.then(ClientCommands.literal("unload").executes(FiloCommand::unload))
				.then(ClientCommands.literal("routes").executes(FiloCommand::listRoutes));
	}

	private static int openSettings(CommandContext<FabricClientCommandSource> context) {
		// chat closes right after the command and would take our screen with it, so open it next tick
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.schedule(() -> minecraft.gui.setScreen(
				ResourcefulConfigScreen.make(FilobulusClient.configurator(), FilobulusConfig.class).build()));
		return 1;
	}

	private static int load(CommandContext<FabricClientCommandSource> context) {
		String name = StringArgumentType.getString(context, "route");
		switch (FilobulusClient.selector().load(name)) {
			case LoadResult.Loaded loaded -> {
				FilobulusClient.tracker().restart();
				say(context, "Loaded " + loaded.route().title() + " route (" + loaded.route().stops().size() + " stops)");
			}
			case LoadResult.NotInMineshaft ignored ->
					fail(context, "Shaft routes can only be loaded inside a mineshaft.");
			case LoadResult.UnknownRoute unknown ->
					fail(context, "No route called \"" + unknown.name() + "\". See /filo routes.");
		}
		return 1;
	}

	private static int unload(CommandContext<FabricClientCommandSource> context) {
		FilobulusClient.selector().unload();
		say(context, "Route hidden.");
		return 1;
	}

	private static int listRoutes(CommandContext<FabricClientCommandSource> context) {
		say(context, "Routes:");
		for (LibraryRoute route : FilobulusClient.routes().all()) {
			context.getSource().sendFeedback(Component.literal("  " + route.id()).withStyle(ChatFormatting.AQUA)
					.append(Component.literal(" - " + route.title() + ", " + route.stops().size() + " stops")
							.withStyle(ChatFormatting.GRAY)));
		}
		return 1;
	}

	private static void say(CommandContext<FabricClientCommandSource> context, String text) {
		context.getSource().sendFeedback(FilobulusClient.chatLine(text));
	}

	private static void fail(CommandContext<FabricClientCommandSource> context, String text) {
		context.getSource().sendError(FilobulusClient.chatLine(text));
	}
}
