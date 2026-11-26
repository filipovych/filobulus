package io.github.filipovych.filobulus.achievement;

// what the achievements screen lists, rarity null means every rarity
public record AchievementFilter(Status status, AchievementRarity rarity) {
	public static final AchievementFilter ALL = new AchievementFilter(Status.ALL, null);

	public enum Status {
		ALL("All"), UNLOCKED("Unlocked"), LOCKED("Locked");

		private final String label;

		Status(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}
	}

	public boolean shows(Achievement achievement, boolean unlocked) {
		boolean status = switch (this.status) {
			case ALL -> true;
			case UNLOCKED -> unlocked;
			case LOCKED -> !unlocked;
		};
		return status && (rarity == null || achievement.rarity() == rarity);
	}

	public AchievementFilter withStatus(Status status) {
		return new AchievementFilter(status, rarity);
	}

	// steps through every rarity and then back to all of them
	public AchievementFilter nextRarity(boolean back) {
		AchievementRarity[] all = AchievementRarity.values();
		int index = rarity == null ? all.length : rarity.ordinal();
		int next = Math.floorMod(index + (back ? -1 : 1), all.length + 1);
		return new AchievementFilter(status, next == all.length ? null : all[next]);
	}
}
