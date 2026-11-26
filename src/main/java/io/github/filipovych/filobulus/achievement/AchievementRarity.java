package io.github.filipovych.filobulus.achievement;

// the hypixel item rarities and their colours, rgb
public enum AchievementRarity {
	COMMON("Common", 0xFFFFFF),
	UNCOMMON("Uncommon", 0x55FF55),
	RARE("Rare", 0x5555FF),
	EPIC("Epic", 0xAA00AA),
	LEGENDARY("Legendary", 0xFFAA00),
	MYTHIC("Mythic", 0xFF55FF);

	private final String displayName;
	private final int color;

	AchievementRarity(String displayName, int color) {
		this.displayName = displayName;
		this.color = color;
	}

	public String displayName() {
		return displayName;
	}

	public int color() {
		return color;
	}
}
