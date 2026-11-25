package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.hud.HudPanel.Content;
import io.github.filipovych.filobulus.client.hud.HudPanel.Row;
import io.github.filipovych.filobulus.client.route.RouteTracker;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.route.CustomRoute;
import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.route.RouteSplits;
import io.github.filipovych.filobulus.route.SplitText;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;

// shown in a mineshaft: its type code, the stop the line points at, the time spent inside and the run against the best
public final class RouteHud implements HudWidget {
	// argb, ahead of the best run and behind it
	private static final int AHEAD = 0xFF55FF55;
	private static final int BEHIND = 0xFFFF5555;

	@Override
	public String name() {
		return "Route HUD";
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
		RouteTracker tracker = FilobulusClient.tracker();
		Location location = tracker.location();
		if (!HudConfig.routeHud) {
			return Optional.empty();
		}
		if (location.area() != Area.MINESHAFT) {
			return tracker.custom().map(route -> own(tracker, route));
		}
		List<Row> rows = new ArrayList<>();
		// the stop row goes away once the route is done or when none is loaded
		tracker.progress().filter(progress -> !progress.finished()).ifPresent(progress ->
				rows.add(new Row("Stop", (progress.reached() + 1) + "/" + progress.total())));
		long now = System.currentTimeMillis();
		rows.add(new Row("Time", SplitText.clock(now - tracker.shaftEnteredAt())));
		if (HudConfig.splits) {
			tracker.splits().ifPresent(run -> splitRows(rows, run, tracker.best(), now));
		}
		// the type code comes a moment after the area, until then the title is plain
		String title = location.shaft().map(ShaftType::code).orElse("Mineshaft");
		int color = location.shaft().map(type -> ShaftColors.of(type.material())).orElse(ShaftColors.GLACITE);
		ItemStack icon = ShaftIcons.of(location.shaft().map(ShaftType::material).orElse(ShaftMaterial.LITTLEFOOT));
		return Optional.of(new Content(icon, title, color, rows));
	}

	// one of the player's own routes, outside of a shaft: its name, the stop and the lap against the best one
	private static HudDrawable own(RouteTracker tracker, CustomRoute route) {
		List<Row> rows = new ArrayList<>();
		tracker.progress().filter(progress -> !progress.finished()).ifPresent(progress ->
				rows.add(new Row("Stop", (progress.reached() + 1) + "/" + progress.total())));
		if (HudConfig.splits) {
			tracker.splits().ifPresent(run -> splitRows(rows, run, tracker.best(), System.currentTimeMillis()));
		}
		return new Content(RouteIcons.of(route.icon()), route.name(), ShaftColors.GLACITE, rows);
	}

	@Override
	public Content sample() {
		return new Content(ShaftIcons.of(ShaftMaterial.JASPER), "JASP_1", ShaftColors.of(ShaftMaterial.JASPER),
				List.of(new Row("Stop", "12/47"), new Row("Time", "2:31"), new Row("Run", "1:42"),
						new Row("Split", "-3.2s", AHEAD), new Row("Left", "4:55")));
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

	// before the first stop only the best is shown, after it the run, how it compares and what is left
	private static void splitRows(List<Row> rows, RouteSplits run, Optional<long[]> best, long now) {
		if (!run.started()) {
			best.ifPresent(times -> rows.add(new Row("Best", SplitText.clock(times[times.length - 1]))));
			return;
		}
		rows.add(new Row("Run", SplitText.clock(run.elapsed(now))));
		best.ifPresent(times -> {
			long behind = run.behind(times);
			rows.add(new Row("Split", SplitText.delta(behind), behind <= 0 ? AHEAD : BEHIND));
			rows.add(new Row("Left", SplitText.clock(run.left(times, now))));
		});
	}
}
