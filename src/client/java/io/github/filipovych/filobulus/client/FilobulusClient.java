package io.github.filipovych.filobulus.client;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import io.github.filipovych.filobulus.client.command.FiloCommand;
import io.github.filipovych.filobulus.client.config.ConfigRouteSettings;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.render.RouteRenderer;
import io.github.filipovych.filobulus.client.route.RouteTracker;
import io.github.filipovych.filobulus.route.RouteFormatException;
import io.github.filipovych.filobulus.route.RouteLibrary;
import io.github.filipovych.filobulus.route.RouteSelector;
import java.io.IOException;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FilobulusClient implements ClientModInitializer {
	public static final String MOD_ID = "filobulus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final Configurator CONFIGURATOR = new Configurator(MOD_ID);
	private static RouteLibrary routes = new RouteLibrary(List.of());
	private static RouteSelector selector = new RouteSelector(routes, new ConfigRouteSettings());
	private static RouteTracker tracker = new RouteTracker(selector);

	@Override
	public void onInitializeClient() {
		// reads config/filobulus.jsonc, creates it on first run
		CONFIGURATOR.register(FilobulusConfig.class);

		try {
			routes = RouteLibrary.loadBundled();
			selector = new RouteSelector(routes, new ConfigRouteSettings());
			tracker = new RouteTracker(selector);
		} catch (IOException | RouteFormatException e) {
			// keep the empty library, the mod still loads
			LOGGER.error("Could not load the bundled routes", e);
		}

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> FiloCommand.register(dispatcher));
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> tracker.tick(minecraft));
		LevelRenderEvents.BEFORE_GIZMOS.register(RouteRenderer::draw);

		LOGGER.info("Filobulus loaded with {} routes", routes.all().size());
	}

	public static Configurator configurator() {
		return CONFIGURATOR;
	}

	public static RouteLibrary routes() {
		return routes;
	}

	public static RouteSelector selector() {
		return selector;
	}

	public static RouteTracker tracker() {
		return tracker;
	}

	public static MutableComponent chatLine(String text) {
		return Component.literal("[Filobulus] ").withStyle(ChatFormatting.AQUA)
				.append(Component.literal(text).withStyle(ChatFormatting.WHITE));
	}
}
