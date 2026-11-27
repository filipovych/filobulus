package io.github.filipovych.filobulus.client;

import com.teamresourceful.resourcefulconfig.api.loader.Configurator;
import io.github.filipovych.filobulus.client.achievement.AchievementWatch;
import io.github.filipovych.filobulus.client.alert.ColdWatch;
import io.github.filipovych.filobulus.client.alert.JasperHud;
import io.github.filipovych.filobulus.client.alert.PortalWatch;
import io.github.filipovych.filobulus.client.alert.ShaftAlert;
import io.github.filipovych.filobulus.client.alert.WispWatch;
import io.github.filipovych.filobulus.client.command.FiloCommand;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.SoundsConfig;
import io.github.filipovych.filobulus.client.corpse.BazaarPrices;
import io.github.filipovych.filobulus.client.corpse.CorpseWatch;
import io.github.filipovych.filobulus.client.corpse.CrackHud;
import io.github.filipovych.filobulus.client.hud.Hud;
import io.github.filipovych.filobulus.client.journal.JournalStore;
import io.github.filipovych.filobulus.client.journal.ShaftLog;
import io.github.filipovych.filobulus.client.journal.WrappedNotice;
import io.github.filipovych.filobulus.client.mining.ComboWatch;
import io.github.filipovych.filobulus.client.mining.MiningWatch;
import io.github.filipovych.filobulus.client.profit.ProfitControls;
import io.github.filipovych.filobulus.client.profit.ProfitStore;
import io.github.filipovych.filobulus.client.profit.ProfitWatch;
import io.github.filipovych.filobulus.client.render.RouteRenderer;
import io.github.filipovych.filobulus.client.route.RouteStore;
import io.github.filipovych.filobulus.client.route.RouteTracker;
import io.github.filipovych.filobulus.client.route.SplitStore;
import io.github.filipovych.filobulus.client.sound.SoundBoard;
import io.github.filipovych.filobulus.route.BundledRoutes;
import io.github.filipovych.filobulus.route.CustomRoute;
import io.github.filipovych.filobulus.route.RouteFormatException;
import io.github.filipovych.filobulus.route.RouteLibrary;
import io.github.filipovych.filobulus.route.RouteSelector;
import io.github.filipovych.filobulus.route.RouteSelector.LoadResult;
import io.github.filipovych.filobulus.sound.OldSoundSwitch;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
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
	private static MiningWatch mining;
	private static ComboWatch combo;
	private static SoundBoard sounds;
	private static ColdWatch cold;
	private static AchievementWatch achievements;
	private static RouteSelector selector;
	private static RouteTracker tracker;

	// 0.3 had the sounds switch under alerts, keep it off for players who turned it off
	private static boolean oldSoundsWereOff() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".jsonc");
		try {
			return Files.isRegularFile(file) && OldSoundSwitch.wasMuted(Files.readString(file));
		} catch (IOException | RuntimeException e) {
			return false;
		}
	}

	@Override
	public void onInitializeClient() {
		// reads config/filobulus.jsonc, creates it on first run
		boolean oldMute = oldSoundsWereOff();
		CONFIGURATOR.register(FilobulusConfig.class);
		if (oldMute) {
			SoundsConfig.sounds = false;
			CONFIGURATOR.saveConfig(FilobulusConfig.class);
			LOGGER.info("Moved the old sounds off setting over");
		}

		// the mod ships no shaft routes, every one of those is the player's own
		RouteLibrary routes = new RouteLibrary(List.of());
		List<CustomRoute> bundled = List.of();
		try {
			bundled = BundledRoutes.load();
		} catch (IOException | RouteFormatException e) {
			// the mod still loads, without them
			LOGGER.error("Could not load the bundled routes", e);
		}
		// the player's own routes and choices sit next to the settings file
		Path dir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
		store = new RouteStore(dir.resolve("routes.json"), routes, bundled);
		store.load();
		splits = new SplitStore(dir.resolve("splits.json"));
		splits.load();
		journal = new JournalStore(dir.resolve("journal.jsonl"));
		journal.load();
		shaftLog = new ShaftLog(journal);
		shaftLog.register();
		selector = new RouteSelector(routes, store.settings());
		tracker = new RouteTracker(selector);

		sounds = new SoundBoard(dir.resolve("sounds.json"), dir.resolve("sounds"));
		sounds.load();
		achievements = new AchievementWatch(dir.resolve("achievements.json"));
		achievements.load();
		cold = new ColdWatch();
		PortalWatch portal = new PortalWatch();
		portal.register();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> FiloCommand.register(dispatcher));
		ShaftAlert shaftAlert = new ShaftAlert();
		wisp = new WispWatch();
		wisp.register();
		CorpseWatch corpses = new CorpseWatch();
		corpses.register();
		profit = new ProfitWatch(new ProfitStore(dir.resolve("profit.json")), PRICES);
		profit.register();
		ProfitControls.register();
		mining = new MiningWatch();
		mining.register();
		combo = new ComboWatch(dir.resolve("combo.json"));
		combo.load();
		WrappedNotice wrapped = new WrappedNotice(dir.resolve("wrapped.txt"));
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			Guard.run("route tracker", () -> tracker.tick(minecraft));
			Guard.run("shaft log", () -> shaftLog.tick(minecraft));
			Guard.run("shaft alert", shaftAlert::tick);
			Guard.run("wisp", () -> wisp.tick(minecraft));
			Guard.run("corpses", () -> corpses.tick(minecraft));
			Guard.run("profit", () -> profit.tick(minecraft));
			Guard.run("cold", () -> cold.tick(minecraft));
			Guard.run("portal", () -> portal.tick(minecraft));
			Guard.run("achievements", () -> achievements.tick(minecraft));
			Guard.run("mining", () -> mining.tick(minecraft));
			Guard.run("combo", () -> combo.tick(minecraft));
			Guard.run("wrapped", () -> wrapped.tick(minecraft));
			Guard.run("jasper moment", JasperHud::tick);
			Guard.run("corpse reveal", CrackHud::tick);
			Hud.tick();
		});
		ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> {
			shaftLog.close();
			profit.close();
			combo.close();
		});
		LevelRenderEvents.BEFORE_GIZMOS.register(RouteRenderer::draw);
		Hud.register();

		LOGGER.info("Filobulus loaded");
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

	public static ColdWatch cold() {
		return cold;
	}

	public static AchievementWatch achievements() {
		return achievements;
	}

	public static MiningWatch mining() {
		return mining;
	}

	public static ComboWatch combo() {
		return combo;
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
