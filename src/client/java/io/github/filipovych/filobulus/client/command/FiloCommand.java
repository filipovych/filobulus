package io.github.filipovych.filobulus.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.teamresourceful.resourcefulconfig.api.client.ResourcefulConfigScreen;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.alert.Alerts;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.TrackerConfig;
import io.github.filipovych.filobulus.client.corpse.CrackHud;
import io.github.filipovych.filobulus.client.detect.GameText;
import io.github.filipovych.filobulus.client.hud.HudEditScreen;
import io.github.filipovych.filobulus.client.journal.RecapHud;
import io.github.filipovych.filobulus.client.screen.JournalScreen;
import io.github.filipovych.filobulus.client.screen.RoutesScreen;
import io.github.filipovych.filobulus.corpse.CrackTier;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.RouteSelector.LoadResult;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
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
		LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommands.literal(name)
				.executes(FiloCommand::openSettings)
				.then(ClientCommands.literal("load")
						.executes(context -> load(context, FilobulusClient.selector().loadHere()))
						.then(ClientCommands.argument("route", StringArgumentType.word())
								.suggests((context, builder) ->
										SharedSuggestionProvider.suggest(FilobulusClient.routes().ids(), builder))
								.executes(context -> load(context, FilobulusClient.selector()
										.load(StringArgumentType.getString(context, "route"))))))
				.then(ClientCommands.literal("unload").executes(FiloCommand::unload))
				.then(ClientCommands.literal("routes").executes(FiloCommand::openRoutes))
				.then(ClientCommands.literal("journal").executes(FiloCommand::openJournal))
				.then(ClientCommands.literal("hud").executes(FiloCommand::editHud))
				.then(ClientCommands.literal("alert").executes(FiloCommand::tryAlert))
				.then(ClientCommands.literal("wisp").executes(FiloCommand::wisp))
				.then(ClientCommands.literal("crack")
						.executes(context -> crack(context, "jasper"))
						.then(ClientCommands.argument("tier", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										Arrays.stream(CrackTier.values()).map(tier -> tier.name().toLowerCase(Locale.ROOT)), builder))
								.executes(context -> crack(context, StringArgumentType.getString(context, "tier")))))
				.then(ClientCommands.literal("recap").executes(FiloCommand::recap))
				.then(ClientCommands.literal("profit")
						.then(ClientCommands.literal("reset").executes(FiloCommand::resetProfit)))
				.then(ClientCommands.literal("debug").executes(FiloCommand::debug));
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			// /filo pretend JASP_1 acts like standing in that shaft, anything else switches it off
			command.then(ClientCommands.literal("pretend")
					.then(ClientCommands.argument("code", StringArgumentType.word()).executes(FiloCommand::pretend)));
		}
		return command;
	}

	private static int openSettings(CommandContext<FabricClientCommandSource> context) {
		// chat closes right after the command and would take our screen with it, so open it next tick
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.schedule(() -> GameCompat.setScreen(minecraft,
				ResourcefulConfigScreen.make(FilobulusClient.configurator(), FilobulusConfig.class).build()));
		return 1;
	}

	private static int editHud(CommandContext<FabricClientCommandSource> context) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.schedule(() -> GameCompat.setScreen(minecraft, new HudEditScreen()));
		return 1;
	}

	private static int openRoutes(CommandContext<FabricClientCommandSource> context) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.schedule(() -> GameCompat.setScreen(minecraft, new RoutesScreen(null)));
		return 1;
	}

	private static int openJournal(CommandContext<FabricClientCommandSource> context) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.schedule(() -> GameCompat.setScreen(minecraft, new JournalScreen(null)));
		return 1;
	}

	// shows the jasper alert with the sound picked in the settings, to hear it and to see where alerts sit
	private static int tryAlert(CommandContext<FabricClientCommandSource> context) {
		Alerts.show("shaft", "JASPER", "This is a test", 0xFF55FF, 1, Alerts.LONG, AlertsConfig.jasperSound.event());
		return 1;
	}

	private static int wisp(CommandContext<FabricClientCommandSource> context) {
		FilobulusClient.wisp().placed();
		say(context, "Will-o'-wisp timer started, 5 minutes.");
		return 1;
	}

	// plays the corpse reveal with made up numbers, to see and hear a tier without looting a corpse
	private static int crack(CommandContext<FabricClientCommandSource> context, String name) {
		for (CrackTier tier : CrackTier.values()) {
			if (tier.name().equalsIgnoreCase(name)) {
				CrackHud.play(CrackHud.example(tier));
				return 1;
			}
		}
		fail(context, "No tier called \"" + name + "\". Try dirt, glacite, ruby, aquamarine or jasper.");
		return 1;
	}

	// shows the card of the last shaft again
	private static int recap(CommandContext<FabricClientCommandSource> context) {
		ShaftVisit last = FilobulusClient.shaftLog().last();
		List<ShaftVisit> journal = FilobulusClient.journal().journal().visits();
		if (last == null && journal.isEmpty()) {
			fail(context, "No shaft to show yet.");
			return 1;
		}
		RecapHud.show(last != null ? last.enteredAt() : journal.get(journal.size() - 1).enteredAt());
		return 1;
	}

	// starts the profit tracker over, for the period it shows
	private static int resetProfit(CommandContext<FabricClientCommandSource> context) {
		FilobulusClient.profit().reset();
		say(context, "Profit tracker reset (" + TrackerConfig.period + ").");
		return 1;
	}

	// without a name it is the route picked for the shaft we are in
	private static int load(CommandContext<FabricClientCommandSource> context, LoadResult result) {
		switch (result) {
			case LoadResult.Loaded loaded -> {
				FilobulusClient.tracker().restart();
				say(context, "Loaded " + loaded.route().title() + " route (" + loaded.route().stops().size() + " stops)");
			}
			case LoadResult.NotInMineshaft ignored ->
					fail(context, "Shaft routes can only be loaded inside a mineshaft.");
			case LoadResult.UnknownRoute unknown ->
					fail(context, "No route called \"" + unknown.name() + "\". See /filo routes.");
			case LoadResult.NoRouteHere none -> fail(context, none.code().isEmpty()
					? "The shaft type is not read yet, try again in a second."
					: "There is no route for " + none.code() + " yet. Import one at /filo routes.");
		}
		return 1;
	}

	private static int unload(CommandContext<FabricClientCommandSource> context) {
		FilobulusClient.selector().unload();
		say(context, "Route hidden.");
		return 1;
	}

	// shows what the mod reads from the game, for when a shaft is not picked up
	private static int debug(CommandContext<FabricClientCommandSource> context) {
		Minecraft minecraft = Minecraft.getInstance();
		Location location = FilobulusClient.tracker().location();
		List<String> lines = new ArrayList<>();
		lines.add("location: " + location.area() + location.shaft().map(type -> " " + type.code()).orElse(""));
		lines.add("stops left: " + FilobulusClient.tracker().upcoming().size());
		for (String line : GameText.tabLines(minecraft)) {
			if (line.contains("Area")) {
				lines.add("tab: " + line);
			}
		}
		for (String line : GameText.sidebarLines(minecraft)) {
			lines.add("sidebar: " + line);
		}
		for (String line : lines) {
			FilobulusClient.LOGGER.info("debug {}", line);
			context.getSource().sendFeedback(Component.literal(line).withStyle(ChatFormatting.GRAY));
		}
		return 1;
	}

	private static int pretend(CommandContext<FabricClientCommandSource> context) {
		String code = StringArgumentType.getString(context, "code").toUpperCase(Locale.ROOT);
		Optional<ShaftType> type = ShaftType.parse(code);
		FilobulusClient.tracker().pretend(type.map(Location::mineshaft).orElse(null));
		say(context, type.map(t -> "Pretending to be in a " + t.code() + " shaft.").orElse("Pretending switched off."));
		return 1;
	}

	private static void say(CommandContext<FabricClientCommandSource> context, String text) {
		context.getSource().sendFeedback(FilobulusClient.chatLine(text));
	}

	private static void fail(CommandContext<FabricClientCommandSource> context, String text) {
		context.getSource().sendError(FilobulusClient.chatLine(text));
	}
}
