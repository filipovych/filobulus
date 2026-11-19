package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import io.github.filipovych.filobulus.shaft.ShaftGroup;

@Category("routes")
@ConfigInfo(title = "Routes", description = "Waypoint routes inside mineshafts.", icon = "route")
public final class RoutesConfig {
	@ConfigEntry(id = "showRoutes", translation = "filobulus.config.routes.showRoutes")
	@Comment("Draw the loaded route in the world.")
	public static boolean showRoutes = true;

	@ConfigEntry(id = "autoLoad", translation = "filobulus.config.routes.autoLoad")
	@Comment("Entering a mineshaft of one of these types loads its route. Others show nothing until you use /filo load.")
	@ConfigOption.Select("Shaft types")
	public static ShaftGroup[] autoLoad = {ShaftGroup.JASPER, ShaftGroup.JASPER_CRYSTAL};

	@ConfigEntry(id = "jasperRoute", translation = "filobulus.config.routes.jasperRoute")
	@Comment("Which route auto-load uses in a normal (non-crystal) jasper shaft.")
	public static JasperRoute jasperRoute = JasperRoute.DEFAULT;

	@ConfigEntry(id = "announceLoad", translation = "filobulus.config.routes.announceLoad")
	@Comment("Say in chat which route was loaded.")
	public static boolean announceLoad = true;

	private RoutesConfig() {
	}
}
