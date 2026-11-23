package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category("routes")
@ConfigInfo(title = "Routes", description = "Waypoint routes inside mineshafts.", icon = "route")
public final class RoutesConfig {
	@ConfigEntry(id = "showRoutes", translation = "filobulus.config.routes.showRoutes")
	@Comment("Draw the loaded route in the world.")
	public static boolean showRoutes = true;

	@ConfigEntry(id = "reach", translation = "filobulus.config.routes.reach")
	@Comment("How close you have to get to a stop, in blocks, before the route moves on to the next one.")
	@ConfigOption.Range(min = 1, max = 10)
	@ConfigOption.Slider
	public static int reach = 3;

	@ConfigEntry(id = "announceLoad", translation = "filobulus.config.routes.announceLoad")
	@Comment("Say in chat which route was loaded.")
	public static boolean announceLoad = true;

	private RoutesConfig() {
	}
}
