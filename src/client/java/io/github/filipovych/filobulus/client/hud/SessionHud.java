package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.alert.WispTimer;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.config.JournalConfig;
import io.github.filipovych.filobulus.client.hud.HudPanel.Content;
import io.github.filipovych.filobulus.client.hud.HudPanel.Row;
import io.github.filipovych.filobulus.client.journal.JournalStore;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.route.SplitText;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// shown in the tunnels and in a shaft: shafts entered since the game was started and since the last jasper one
// and how long the will-o'-wisp still has, when we know of one
public final class SessionHud implements HudWidget {
	// argb
	private static final int WISP_LOW = 0xFFFFAA00;
	private static final int WISP_GONE = 0xFFFF5555;
	private static final long LOW_MILLIS = 30_000;

	@Override
	public String name() {
		return "Session";
	}

	@Override
	public boolean enabled() {
		return HudConfig.sessionHud;
	}

	@Override
	public void setEnabled(boolean on) {
		HudConfig.sessionHud = on;
	}

	@Override
	public Optional<HudDrawable> content() {
		Area area = FilobulusClient.tracker().location().area();
		if (!HudConfig.sessionHud || !JournalConfig.journal || area == Area.ELSEWHERE) {
			return Optional.empty();
		}
		JournalStore store = FilobulusClient.journal();
		List<Row> rows = rows(store.journal().since(store.sessionStart()).size(), store.journal().sinceJasper());
		WispTimer wisp = FilobulusClient.wisp().timer();
		if (AlertsConfig.wisp && wisp.known()) {
			long left = wisp.left(System.currentTimeMillis());
			rows.add(left == 0 ? new Row("Wisp", "gone", WISP_GONE)
					: left <= LOW_MILLIS ? new Row("Wisp", SplitText.clock(left), WISP_LOW)
					: new Row("Wisp", SplitText.clock(left)));
		}
		return Optional.of(panel(rows));
	}

	@Override
	public HudDrawable sample() {
		List<Row> rows = rows(12, 7);
		rows.add(new Row("Wisp", "3:21"));
		return panel(rows);
	}

	private static List<Row> rows(int shafts, int sinceJasper) {
		List<Row> rows = new ArrayList<>();
		rows.add(new Row("Shafts", String.valueOf(shafts)));
		rows.add(new Row("Since jasper", String.valueOf(sinceJasper)));
		return rows;
	}

	private static Content panel(List<Row> rows) {
		return new Content(ShaftIcons.of(ShaftMaterial.LITTLEFOOT), "Session", ShaftColors.GLACITE, rows);
	}

	@Override
	public HudPlacement placement() {
		return new HudPlacement(HudConfig.sessionHudAnchorX, HudConfig.sessionHudX,
				HudConfig.sessionHudAnchorY, HudConfig.sessionHudY, HudConfig.sessionHudScale);
	}

	@Override
	public void place(HudPlacement placement) {
		HudConfig.sessionHudAnchorX = placement.anchorX();
		HudConfig.sessionHudX = placement.x();
		HudConfig.sessionHudAnchorY = placement.anchorY();
		HudConfig.sessionHudY = placement.y();
		HudConfig.sessionHudScale = placement.scale();
	}

	@Override
	public HudPlacement defaultPlacement() {
		// left edge, right above the route hud at its tallest, further down it would sit in the chat
		return new HudPlacement(HudAnchor.START, 4, HudAnchor.CENTRE, -66, 1f);
	}
}
