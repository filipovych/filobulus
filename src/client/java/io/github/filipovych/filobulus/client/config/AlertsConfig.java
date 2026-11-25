package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;

@Category("alerts")
@ConfigInfo(title = "Alerts", description = "Big text in the middle of the screen. Move it with /filo hud, pick their sounds on the Sounds page.", icon = "bell")
public final class AlertsConfig {
	@ConfigEntry(id = "alerts", translation = "filobulus.config.alerts.alerts")
	@Comment("Show alerts at all. Off silences every alert below.")
	public static boolean alerts = true;

	@ConfigEntry(id = "shaftType", translation = "filobulus.config.alerts.shaftType")
	@Comment("When you enter a mineshaft, show its type in big letters. A jasper shaft, crystal or not, stays longer and has a sound.")
	public static boolean shaftType = true;

	@ConfigEntry(id = "wisp", translation = "filobulus.config.alerts.wisp")
	@Comment("Warn when your Will-o'-wisp has 10 seconds left and when it is gone, 5 minutes after you used it. /filo wisp starts the timer by hand, for a wisp someone else placed.")
	public static boolean wisp = true;

	@ConfigEntry(id = "newBest", translation = "filobulus.config.alerts.newBest")
	@Comment("Show a new best time on a shaft route in big letters.")
	public static boolean newBest = true;

	private AlertsConfig() {
	}
}
