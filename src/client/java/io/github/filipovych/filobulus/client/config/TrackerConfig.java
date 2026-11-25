package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;

@Category("tracker")
@ConfigInfo(title = "Profit tracker", description = "What you gained and what it sells for. Move it with /filo hud.", icon = "coins")
public final class TrackerConfig {
	// what counts as loot
	public enum Items {
		MINING("Mining"),
		EVERYTHING("Everything");

		private final String title;

		Items(String title) {
			this.title = title;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	public enum Price {
		INSTANT_SELL("Instant sell"),
		SELL_ORDER("Sell order"),
		NPC("NPC");

		private final String title;

		Price(String title) {
			this.title = title;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	public enum Period {
		SESSION("Session"),
		ALL_TIME("All time");

		private final String title;

		Period(String title) {
			this.title = title;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	@ConfigEntry(id = "profit", translation = "filobulus.config.tracker.profit")
	@Comment("Count what you gain, from your sack messages and your inventory, and show what it sells for at the bazaar. /filo profit reset starts over.")
	public static boolean profit = true;

	@ConfigEntry(id = "items", translation = "filobulus.config.tracker.items")
	@Comment("Mining counts ores, gemstones and the other things mining gives, on the mining islands. Everything counts any item that has a price, on every island, for the other skills, and downloads Hypixel's public item list once to know the items by name.")
	public static Items items = Items.MINING;

	@ConfigEntry(id = "price", translation = "filobulus.config.tracker.price")
	@Comment("The price an item is valued at: what the bazaar pays at once, what a sell order would get, or what an NPC pays. NPC downloads Hypixel's public item list once for its prices.")
	public static Price price = Price.INSTANT_SELL;

	@ConfigEntry(id = "period", translation = "filobulus.config.tracker.period")
	@Comment("Session is since the game was started or since the last reset. All time is kept between sessions.")
	public static Period period = Period.SESSION;

	@ConfigEntry(id = "lines", translation = "filobulus.config.tracker.lines")
	@Comment("How many items are listed above the total, the most valuable first. 0 shows only the total.")
	@ConfigOption.Range(min = 0, max = 15)
	@ConfigOption.Slider
	public static int lines = 6;

	@ConfigEntry(id = "idle", translation = "filobulus.config.tracker.idle")
	@Comment("Seconds without a gain after which the clock for coins per hour stops, so a break does not pull the rate down.")
	@ConfigOption.Range(min = 15, max = 300)
	@ConfigOption.Slider
	public static int idle = 60;

	@ConfigEntry(id = "hideEmpty", translation = "filobulus.config.tracker.hideEmpty")
	@Comment("Hide the panel while nothing has been gained yet.")
	public static boolean hideEmpty = false;

	private TrackerConfig() {
	}
}
