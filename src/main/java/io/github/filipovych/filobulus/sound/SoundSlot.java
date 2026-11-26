package io.github.filipovych.filobulus.sound;

// every moment the mod can play a sound for, each one gets its own sound and volume on the sounds screen
// fallback is the key of the built in sound it plays until the player picks another
public enum SoundSlot {
	JASPER_SHAFT("Jasper shaft", "Alerts", "level_up"),
	NEW_BEST("New best", "Alerts", "challenge"),
	WISP_GONE("Will-o'-wisp gone", "Alerts", "pling"),
	COLD_POTION("Cold potion", "Alerts", "bass"),
	PORTAL("Shaft portal closing", "Alerts", "bell"),
	ACHIEVEMENT("Achievement", "Alerts", "challenge"),
	CORPSE_DIRT("Dirt", "Corpse reveal", "villager_no"),
	CORPSE_GLACITE("Glacite", "Corpse reveal", "bass"),
	CORPSE_RUBY("Ruby", "Corpse reveal", "xp"),
	CORPSE_AQUAMARINE("Aquamarine", "Corpse reveal", "level_up"),
	CORPSE_JASPER("Jasper", "Corpse reveal", "challenge"),
	COMBO_RANK("Rank up", "Swap combo", "pling"),
	COMBO_BROKEN("Combo broken", "Swap combo", "glass");

	private final String title;
	private final String group;
	private final String fallback;

	SoundSlot(String title, String group, String fallback) {
		this.title = title;
		this.group = group;
		this.fallback = fallback;
	}

	public String title() {
		return title;
	}

	public String group() {
		return group;
	}

	public String fallback() {
		return fallback;
	}
}
