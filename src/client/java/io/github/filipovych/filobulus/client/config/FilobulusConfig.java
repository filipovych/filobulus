package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.Config;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;

// resourceful config builds the screen from these, entries must be public static and not final
@Config(value = "filobulus", categories = {RoutesConfig.class, HudConfig.class, AlertsConfig.class, JournalConfig.class})
@ConfigInfo(
		title = "Filobulus",
		description = "Endgame mining: Glacite Tunnels and Mineshafts.",
		icon = "pickaxe",
		links = {@ConfigInfo.Link(value = "https://github.com/filipovych/filobulus", icon = "github", text = "GitHub")})
public final class FilobulusConfig {
	@ConfigEntry(id = "enabled", translation = "filobulus.config.enabled")
	@Comment("Master switch. Off means the mod does nothing.")
	public static boolean enabled = true;

	private FilobulusConfig() {
	}
}
