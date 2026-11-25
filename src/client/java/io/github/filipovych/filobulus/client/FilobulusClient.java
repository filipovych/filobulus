package io.github.filipovych.filobulus.client;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import io.github.filipovych.filobulus.client.alert.ShaftAlert;
import io.github.filipovych.filobulus.client.alert.WispWatch;
import io.github.filipovych.filobulus.client.command.FiloCommand;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.corpse.BazaarPrices;
import io.github.filipovych.filobulus.client.corpse.CorpseWatch;
import io.github.filipovych.filobulus.client.hud.Hud;
import io.github.filipovych.filobulus.client.journal.JournalStore;
import io.github.filipovych.filobulus.client.journal.ShaftLog;
import io.github.filipovych.filobulus.client.profit.ProfitStore;
import io.github.filipovych.filobulus.client.profit.ProfitWatch;
import io.github.filipovych.filobulus.client.render.RouteRenderer;
import io.github.filipovych.filobulus.client.route.RouteStore;
import io.github.filipovych.filobulus.client.route.RouteTracker;
import io.github.filipovych.filobulus.client.route.SplitStore;
import io.github.filipovych.filobulus.client.sound.SoundBoard;
import io.github.filipovych.filobulus.route.RouteFormatException;
import io.github.filipovych.filobulus.route.RouteLibrary;
import io.github.filipovych.filobulus.route.RouteSelector;
import io.github.filipovych.filobulus.route.RouteSelector.LoadResult;
import java.io.IOException;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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
	private static JournalStore journal;
	private static ShaftLog shaftLog;
	private static WispWatch wisp;
	// one price list for everything that needs prices
	private static final BazaarPrices PRICES = new BazaarPrices();
	private static ProfitWatch profit;
	private static SoundBoard sounds;
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
		journal = new JournalStore(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("journal.jsonl"));
		journal.load();
		shaftLog = new ShaftLog(journal);
		selector = new RouteSelector(routes, store.settings());
		tracker = new RouteTracker(selector);

		sounds = new SoundBoard(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("sounds.json"),
				FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("sounds"));
		sounds.load();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> FiloCommand.register(dispatcher));
		ShaftAlert shaftAlert = new ShaftAlert();
		wisp = new WispWatch();
		wisp.register();
		CorpseWatch corpses = new CorpseWatch();
		corpses.register();
		profit = new ProfitWatch(
				new ProfitStore(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("profit.json")), PRICES);
		profit.register();
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			tracker.tick(minecraft);
			shaftLog.tick(minecraft);
			shaftAlert.tick();
			wisp.tick(minecraft);
			corpses.tick(minecraft);
			profit.tick(minecraft);
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
			shaftLog.close();
			profit.close();
		});
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

	public static JournalStore journal() {
		return journal;
	}

	public static ShaftLog shaftLog() {
		return shaftLog;
	}

	public static WispWatch wisp() {
		return wisp;
	}

	public static BazaarPrices prices() {
		return PRICES;
	}

	public static SoundBoard sounds() {
		return sounds;
	}

	public static ProfitWatch profit() {
		return profit;
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

	// the profile was switched, the shaft we stand in may use another route now
	public static void profileChanged() {
		tracker.location().shaft().ifPresent(type -> routeChanged(type.routeKey()));
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
