package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.alert.WispTimer;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.config.JournalConfig;
import io.github.filipovych.filobulus.client.config.MiningConfig;
import io.github.filipovych.filobulus.client.hud.HudPanel.Content;
import io.github.filipovych.filobulus.client.hud.HudPanel.Row;
import io.github.filipovych.filobulus.client.journal.JournalStore;
import io.github.filipovych.filobulus.client.mining.MiningWatch;
import io.github.filipovych.filobulus.client.route.RouteTracker;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.mining.ColdRate;
import io.github.filipovych.filobulus.route.CustomRoute;
import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.route.RouteSplits;
import io.github.filipovych.filobulus.route.SplitText;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.world.item.ItemStack;

// the one panel for mining: in a shaft its name, the stop, the time inside, the best run and how this one compares
// under that the session: shafts and since jasper, pity, cold, the wisp and the cold potion
// in the tunnels only the session part shows, outside them a route of the player's own
public final class RouteHud implements HudWidget {
	// argb
	private static final int AHEAD = 0xFF55FF55;
	private static final int BEHIND = 0xFFFF5555;
	private static final int LOW = 0xFFFFAA00;
	private static final int GONE = 0xFFFF5555;
	private static final long LOW_MILLIS = 30_000;
	private static final int VALUE = 0xFFFFFFFF;
	private final TickCache<Optional<HudDrawable>> cache = new TickCache<>();

	@Override
	public String name() {
		return "Mining panel";
	}

	@Override
	public boolean enabled() {
		return HudConfig.routeHud;
	}

	@Override
	public void setEnabled(boolean on) {
		HudConfig.routeHud = on;
	}

	@Override
	public Optional<HudDrawable> content() {
		return cache.get(this::build);
	}

	private Optional<HudDrawable> build() {
		if (!HudConfig.routeHud) {
			return Optional.empty();
		}
		RouteTracker tracker = FilobulusClient.tracker();
		Location location = tracker.location();
		long now = System.currentTimeMillis();
		if (location.area() == Area.MINESHAFT) {
			List<Row> rows = new ArrayList<>();
			// the stop row goes away once the route is done or when none is loaded
			tracker.progress().filter(progress -> !progress.finished()).ifPresent(progress ->
					rows.add(new Row("Stop", (progress.reached() + 1) + "/" + progress.total())));
			rows.add(new Row("Time", SplitText.clock(now - tracker.shaftEnteredAt())));
			if (HudConfig.splits) {
				tracker.splits().ifPresent(run -> splitRows(rows, run, tracker.best()));
			}
			session(rows, now);
			// the type comes a moment after the area, until then the title is plain
			String title = location.shaft().map(ShaftType::displayName).orElse("Mineshaft");
			int color = location.shaft().map(type -> ShaftColors.of(type.material())).orElse(ShaftColors.GLACITE);
			ItemStack icon = ShaftIcons.of(location.shaft().map(ShaftType::material).orElse(ShaftMaterial.LITTLEFOOT));
			return Optional.of(new Content(icon, title, color, rows));
		}
		Optional<CustomRoute> own = tracker.custom();
		if (own.isPresent()) {
			return Optional.of(own(tracker, own.get(), now, location.area() == Area.GLACITE_TUNNELS));
		}
		if (location.area() == Area.GLACITE_TUNNELS) {
			List<Row> rows = new ArrayList<>();
			session(rows, now);
			if (rows.isEmpty()) {
				return Optional.empty();
			}
			rows.remove(0);
			return Optional.of(new Content(ShaftIcons.of(ShaftMaterial.LITTLEFOOT), "Glacite Tunnels", ShaftColors.GLACITE, rows));
		}
		return Optional.empty();
	}

	// one of the player's own routes, outside of a shaft: its name, the stop and the lap against the best one
	private static HudDrawable own(RouteTracker tracker, CustomRoute route, long now, boolean tunnels) {
		List<Row> rows = new ArrayList<>();
		tracker.progress().filter(progress -> !progress.finished()).ifPresent(progress ->
				rows.add(new Row("Stop", (progress.reached() + 1) + "/" + progress.total())));
		tracker.splits().ifPresent(run -> rows.add(new Row(route.loop() ? "Lap" : "Time", SplitText.clock(run.elapsed(now)))));
		if (HudConfig.splits) {
			tracker.splits().ifPresent(run -> splitRows(rows, run, tracker.best()));
		}
		if (tunnels) {
			session(rows, now);
		}
		return new Content(RouteIcons.of(route.icon()), route.name(), ShaftColors.GLACITE, rows);
	}

	// shafts this session and since jasper, then the wisp and the cold potion, after a small gap
	private static void session(List<Row> rows, long now) {
		List<Row> more = new ArrayList<>();
		if (HudConfig.sessionHud && JournalConfig.journal) {
			JournalStore store = FilobulusClient.journal();
			List<ShaftVisit> session = store.journal().since(store.sessionStart());
			more.add(new Row(ItemStack.EMPTY, "Shafts", perHour(session, now), String.valueOf(session.size()), VALUE));
			more.add(new Row("Since jasper", String.valueOf(store.journal().sinceJasper())));
		}
		MiningWatch mining = FilobulusClient.mining();
		if (MiningConfig.pity) {
			mining.pity().ifPresent(pity -> more.add(new Row("Pity",
					String.format(Locale.ROOT, "%,d/%,d", pity.now(), pity.full()))));
		}
		ColdRate freezing = mining.cold();
		if (MiningConfig.cold && freezing.known() && freezing.cold() > 0) {
			long until = freezing.untilDeadly(now);
			int color = freezing.cold() >= 75 ? GONE : freezing.cold() >= 50 ? LOW : VALUE;
			more.add(new Row(ItemStack.EMPTY, "Cold", until < 0 ? "" : SplitText.clock(until) + " left", String.valueOf(freezing.cold()), color));
		}
		WispTimer wisp = FilobulusClient.wisp().timer();
		if (AlertsConfig.wisp && wisp.known()) {
			more.add(timer("Wisp", wisp.left(now), "gone"));
		}
		OptionalLong cold = FilobulusClient.cold().left();
		if (AlertsConfig.coldPotion && cold.isPresent()) {
			more.add(timer("Cold pot", cold.getAsLong(), "none"));
		}
		if (!more.isEmpty()) {
			rows.add(HudPanel.SPACER);
			rows.addAll(more);
		}
	}

	// shafts an hour this session, once there are a few and enough time to say
	private static String perHour(List<ShaftVisit> session, long now) {
		if (session.size() < 2) {
			return "";
		}
		long spent = now - session.get(0).enteredAt();
		return spent < 10 * 60_000 ? "" : String.format(Locale.ROOT, "%.1f/h", session.size() * 3_600_000d / spent);
	}

	private static Row timer(String label, long left, String none) {
		return left == 0 ? new Row(label, none, GONE)
				: left <= LOW_MILLIS ? new Row(label, SplitText.clock(left), LOW)
				: new Row(label, SplitText.clock(left));
	}

	@Override
	public Content sample() {
		return new Content(ShaftIcons.of(ShaftMaterial.JASPER), "Jasper", ShaftColors.of(ShaftMaterial.JASPER),
				List.of(new Row("Stop", "12/47"), new Row("Time", "2:31"), new Row("Best", "2:48"), new Row("Split", "-3.2s", AHEAD),
						HudPanel.SPACER, new Row(ItemStack.EMPTY, "Shafts", "4.1/h", "12", VALUE), new Row("Since jasper", "7"),
						new Row("Wisp", "3:21")));
	}

	@Override
	public HudPlacement placement() {
		return new HudPlacement(HudConfig.routeHudAnchorX, HudConfig.routeHudX,
				HudConfig.routeHudAnchorY, HudConfig.routeHudY, HudConfig.routeHudScale);
	}

	@Override
	public void place(HudPlacement placement) {
		HudConfig.routeHudAnchorX = placement.anchorX();
		HudConfig.routeHudX = placement.x();
		HudConfig.routeHudAnchorY = placement.anchorY();
		HudConfig.routeHudY = placement.y();
		HudConfig.routeHudScale = placement.scale();
	}

	@Override
	public HudPlacement defaultPlacement() {
		// left edge, half way up, clear of the chat, the sidebar and the boss bar
		return new HudPlacement(HudAnchor.START, 4, HudAnchor.CENTRE, 0, 1f);
	}

	// the best run of the route on the same clock as the time, and once a stop was reached how this run compares at the last one
	private static void splitRows(List<Row> rows, RouteSplits run, Optional<long[]> best) {
		best.ifPresent(times -> {
			rows.add(new Row("Best", SplitText.clock(times[times.length - 1])));
			if (run.started()) {
				long behind = run.behind(times);
				rows.add(new Row("Split", SplitText.delta(behind), behind <= 0 ? AHEAD : BEHIND));
			}
		});
	}
}
