package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.config.JournalConfig;
import io.github.filipovych.filobulus.client.hud.HudPanel.Content;
import io.github.filipovych.filobulus.client.hud.HudPanel.Row;
import io.github.filipovych.filobulus.client.journal.JournalStore;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import java.util.List;
import java.util.Optional;

// shown in the tunnels and in a shaft: shafts entered since the game was started and since the last jasper one
public final class SessionHud implements HudWidget {
	@Override
	public Optional<HudDrawable> content() {
		Area area = FilobulusClient.tracker().location().area();
		if (!HudConfig.sessionHud || !JournalConfig.journal || area == Area.ELSEWHERE) {
			return Optional.empty();
		}
		JournalStore store = FilobulusClient.journal();
		return Optional.of(panel(store.journal().since(store.sessionStart()).size(), store.journal().sinceJasper()));
	}

	@Override
	public HudDrawable sample() {
		return panel(12, 7);
	}

	private static Content panel(int shafts, int sinceJasper) {
		return new Content(ShaftIcons.of(ShaftMaterial.LITTLEFOOT), "Session", ShaftColors.GLACITE,
				List.of(new Row("Shafts", String.valueOf(shafts)), new Row("Since jasper", String.valueOf(sinceJasper))));
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
		return new HudPlacement(HudAnchor.START, 4, HudAnchor.CENTRE, -62, 1f);
	}
}
