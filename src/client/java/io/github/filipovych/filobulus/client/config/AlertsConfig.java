package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;

@Category("alerts")
@ConfigInfo(title = "Alerts", description = "Big text in the middle of the screen. Move it with /filo hud.", icon = "bell")
public final class AlertsConfig {
	@ConfigEntry(id = "shaftType", translation = "filobulus.config.alerts.shaftType")
	@Comment("When you enter a mineshaft, show its type in big letters. A jasper shaft, crystal or not, stays longer and has a sound.")
	public static boolean shaftType = true;

	@ConfigEntry(id = "newBest", translation = "filobulus.config.alerts.newBest")
	@Comment("Show a new best time on a shaft route in big letters.")
	public static boolean newBest = true;

	@ConfigEntry(id = "sounds", translation = "filobulus.config.alerts.sounds")
	@Comment("Play a sound with the alerts that have one.")
	public static boolean sounds = true;

	private AlertsConfig() {
	}
}
