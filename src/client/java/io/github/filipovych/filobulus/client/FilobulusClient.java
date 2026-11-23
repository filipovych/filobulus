package io.github.filipovych.filobulus.client;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import io.github.filipovych.filobulus.client.command.FiloCommand;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.hud.Hud;
import io.github.filipovych.filobulus.client.render.RouteRenderer;
import io.github.filipovych.filobulus.client.route.RouteStore;
import io.github.filipovych.filobulus.client.route.RouteTracker;
import io.github.filipovych.filobulus.client.route.SplitStore;
import io.github.filipovych.filobulus.route.RouteFormatException;
import io.github.filipovych.filobulus.route.RouteLibrary;
import io.github.filipovych.filobulus.route.RouteSelector;
import io.github.filipovych.filobulus.route.RouteSelector.LoadResult;
import java.io.IOException;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
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
	private static RouteStore store;
	private static SplitStore splits;
	private static RouteSelector selector;
	private static RouteTracker tracker;

	@Override
	public void onInitializeClient() {
		// reads config/filobulus.jsonc, creates it on first run
		CONFIGURATOR.register(FilobulusConfig.class);

		RouteLibrary routes = new RouteLibrary(List.of());
		try {
			routes = RouteLibrary.loadBundled();
		} catch (IOException | RouteFormatException e) {
			// keep the empty library, the mod still loads
			LOGGER.error("Could not load the bundled routes", e);
		}
		// the player's own routes and choices sit next to the settings file
		store = new RouteStore(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("routes.json"), routes);
		store.load();
		splits = new SplitStore(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("splits.json"));
		splits.load();
		selector = new RouteSelector(routes, store.choices());
		tracker = new RouteTracker(selector);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> FiloCommand.register(dispatcher));
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> tracker.tick(minecraft));
		LevelRenderEvents.BEFORE_GIZMOS.register(RouteRenderer::draw);
		Hud.register();

		LOGGER.info("Filobulus loaded with {} routes", routes.all().size());
	}

	public static Configurator configurator() {
		return CONFIGURATOR;
	}

	public static RouteLibrary routes() {
		return store.library();
	}

	public static RouteStore store() {
		return store;
	}

	public static SplitStore splits() {
		return splits;
	}

	// the routes screen changed what a shaft uses, show that at once when we are standing in such a shaft
	public static void routeChanged(String routeKey) {
		if (tracker.location().shaft().filter(type -> type.routeKey().equals(routeKey)).isPresent()) {
			// nothing left to load means its last route was just deleted
			if (!(selector.loadHere() instanceof LoadResult.Loaded)) {
				selector.unload();
			}
			tracker.restart();
		}
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
