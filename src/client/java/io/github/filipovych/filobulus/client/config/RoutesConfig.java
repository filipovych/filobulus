package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigButton;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.screen.RoutesScreen;
import net.minecraft.client.Minecraft;

@Category("routes")
@ConfigInfo(title = "Routes", description = "Waypoint routes inside mineshafts.", icon = "route")
public final class RoutesConfig {
	@ConfigEntry(id = "showRoutes", translation = "filobulus.config.routes.showRoutes")
	@Comment("Draw the loaded route in the world.")
	public static boolean showRoutes = true;

	// which shafts auto-load and which route each one uses is set on a screen of our own, also at /filo routes
	@ConfigButton(title = "Shaft routes", text = "Open")
	@Comment("Pick the shafts that load a route by themselves and the route each one uses. Routes are imported and exported here too.")
	public static final Runnable manageRoutes = () -> {
		Minecraft minecraft = Minecraft.getInstance();
		GameCompat.setScreen(minecraft, new RoutesScreen(GameCompat.screen(minecraft)));
	};

	@ConfigEntry(id = "reach", translation = "filobulus.config.routes.reach")
	@Comment("How close you have to get to a stop, in blocks, before the route moves on to the next one.")
	@ConfigOption.Range(min = 1, max = 10)
	@ConfigOption.Slider
	public static int reach = 3;

	@ConfigEntry(id = "stopsShown", translation = "filobulus.config.routes.stopsShown")
	@Comment("How many of the next stops are drawn. The next one is filled, the ones after it get fainter.")
	@ConfigOption.Range(min = 1, max = 6)
	@ConfigOption.Slider
	public static int stopsShown = 2;

	@ConfigEntry(id = "announceLoad", translation = "filobulus.config.routes.announceLoad")
	@Comment("Say in chat which route was loaded.")
	public static boolean announceLoad = true;

	private RoutesConfig() {
	}
}
