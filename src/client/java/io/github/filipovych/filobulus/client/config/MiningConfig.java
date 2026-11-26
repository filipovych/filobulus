package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import io.github.filipovych.filobulus.hud.HudAnchor;

@Category("mining")
@ConfigInfo(title = "Mining", description = "Breakpoints, cold and pity. Most need a tab list widget switched on in /tab.", icon = "pickaxe")
public final class MiningConfig {
	@ConfigEntry(id = "breakpoints", translation = "filobulus.config.mining.breakpoints")
	@Comment("For the block you look at (or the shaft's gemstone): how many ticks it takes at your mining speed, and how much more speed the next breakpoint needs. Reads Mining Speed from the Stats widget of the tab list.")
	public static boolean breakpoints = true;

	@ConfigEntry(id = "alphaStrengths", translation = "filobulus.config.mining.alphaStrengths")
	@Comment("Use the gemstone strengths of the Alpha server (November 2026: jasper 4,000, onyx 4,000, ruby 2,500). Switch on once that change reaches the main server.")
	public static boolean alphaStrengths = false;

	@ConfigEntry(id = "msbBonus", translation = "filobulus.config.mining.msbBonus")
	@Comment("How much Mining Speed Boost adds for you, in percent: 200, 250 with an omelette or Center of the Mountain tier 2, 300 with both.")
	@ConfigOption.Range(min = 100, max = 400)
	@ConfigOption.Slider
	public static int msbBonus = 200;

	@ConfigEntry(id = "swapCue", translation = "filobulus.config.mining.swapCue")
	@Comment("Ping gliding: a small cue near the crosshair when the server should already have the block broken, half your ping before the animation ends, so you can move to the next one.")
	public static boolean swapCue = true;

	@ConfigEntry(id = "cold", translation = "filobulus.config.mining.cold")
	@Comment("Show your cold on the mining panel with how long until 100 at the rate it is going up. Reads Cold from the sidebar.")
	public static boolean cold = true;

	@ConfigEntry(id = "pity", translation = "filobulus.config.mining.pity")
	@Comment("Show the mineshaft pity on the mining panel. Needs the Pity widget in /tab.")
	public static boolean pity = true;

	// where the panels of this page sit, changed in /filo hud
	@ConfigEntry(id = "breakHudAnchorX")
	@ConfigOption.Hidden
	public static HudAnchor breakHudAnchorX = HudAnchor.END;

	@ConfigEntry(id = "breakHudX")
	@ConfigOption.Hidden
	public static int breakHudX = -4;

	@ConfigEntry(id = "breakHudAnchorY")
	@ConfigOption.Hidden
	public static HudAnchor breakHudAnchorY = HudAnchor.CENTRE;

	@ConfigEntry(id = "breakHudY")
	@ConfigOption.Hidden
	public static int breakHudY = 30;

	@ConfigEntry(id = "breakHudScale")
	@ConfigOption.Hidden
	public static float breakHudScale = 1f;

	@ConfigEntry(id = "swapHudAnchorX")
	@ConfigOption.Hidden
	public static HudAnchor swapHudAnchorX = HudAnchor.CENTRE;

	@ConfigEntry(id = "swapHudX")
	@ConfigOption.Hidden
	public static int swapHudX = 0;

	@ConfigEntry(id = "swapHudAnchorY")
	@ConfigOption.Hidden
	public static HudAnchor swapHudAnchorY = HudAnchor.CENTRE;

	@ConfigEntry(id = "swapHudY")
	@ConfigOption.Hidden
	public static int swapHudY = 12;

	@ConfigEntry(id = "swapHudScale")
	@ConfigOption.Hidden
	public static float swapHudScale = 1f;

	private MiningConfig() {
	}
}
