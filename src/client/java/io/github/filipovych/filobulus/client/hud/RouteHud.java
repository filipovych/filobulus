package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.hud.HudPanel.Content;
import io.github.filipovych.filobulus.client.hud.HudPanel.Row;
import io.github.filipovych.filobulus.client.route.RouteTracker;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// shown in a mineshaft: its type code, the stop the line points at and the time spent inside
public final class RouteHud implements HudWidget {
	@Override
	public Optional<Content> content() {
		RouteTracker tracker = FilobulusClient.tracker();
		Location location = tracker.location();
		if (!HudConfig.routeHud || location.area() != Area.MINESHAFT) {
			return Optional.empty();
		}
		List<Row> rows = new ArrayList<>();
		// the stop row goes away once the route is done or when none is loaded
		tracker.progress().filter(progress -> !progress.finished()).ifPresent(progress ->
				rows.add(new Row("Stop", (progress.reached() + 1) + "/" + progress.total())));
		rows.add(new Row("Time", clock(System.currentTimeMillis() - tracker.shaftEnteredAt())));
		// the type code comes a moment after the area, until then the title is plain
		String title = location.shaft().map(ShaftType::code).orElse("Mineshaft");
		int color = location.shaft().map(type -> ShaftColors.of(type.material())).orElse(ShaftColors.GLACITE);
		return Optional.of(new Content(title, color, rows));
	}

	@Override
	public Content sample() {
		return new Content("JASP_1", ShaftColors.of(ShaftMaterial.JASPER),
				List.of(new Row("Stop", "12/47"), new Row("Time", "2:31")));
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

	private static String clock(long millis) {
		long seconds = Math.max(0, millis) / 1000;
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}
}
