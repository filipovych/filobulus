package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.hud.HudPlacement;
import java.util.Optional;

// one panel on screen that the player can move around
public interface HudWidget {
	// what /filo hud calls it
	String name();

	// the switch that turns this panel on and off, the same one the settings screen has
	boolean enabled();

	void setEnabled(boolean on);

	// what to show right now, empty hides the panel
	Optional<HudDrawable> content();

	// made up content for /filo hud, so the panel can be placed while it has nothing to show
	HudDrawable sample();

	HudPlacement placement();

	void place(HudPlacement placement);

	HudPlacement defaultPlacement();
}
