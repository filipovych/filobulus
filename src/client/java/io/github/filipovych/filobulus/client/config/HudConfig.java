package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import io.github.filipovych.filobulus.hud.HudAnchor;

@Category("hud")
@ConfigInfo(title = "HUD", description = "Panels on screen. Move them with /filo hud.", icon = "layout-dashboard")
public final class HudConfig {
	@ConfigEntry(id = "routeHud", translation = "filobulus.config.hud.routeHud")
	@Comment("In a mineshaft, show its type, the stop you are on and the time spent inside.")
	public static boolean routeHud = true;

	@ConfigEntry(id = "panel", translation = "filobulus.config.hud.panel")
	@Comment("Draw a dark panel behind the text. Off shows only the text over the game.")
	public static boolean panel = false;

	// where the route hud sits, changed by dragging it in /filo hud and not shown in the settings
	@ConfigEntry(id = "routeHudAnchorX")
	@ConfigOption.Hidden
	public static HudAnchor routeHudAnchorX = HudAnchor.START;

	@ConfigEntry(id = "routeHudX")
	@ConfigOption.Hidden
	public static int routeHudX = 4;

	@ConfigEntry(id = "routeHudAnchorY")
	@ConfigOption.Hidden
	public static HudAnchor routeHudAnchorY = HudAnchor.CENTRE;

	@ConfigEntry(id = "routeHudY")
	@ConfigOption.Hidden
	public static int routeHudY = 0;

	@ConfigEntry(id = "routeHudScale")
	@ConfigOption.Hidden
	public static float routeHudScale = 1f;

	private HudConfig() {
	}
}
