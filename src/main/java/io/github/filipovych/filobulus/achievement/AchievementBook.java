package io.github.filipovych.filobulus.achievement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// which achievements are unlocked and when, saved by their lower case name
public final class AchievementBook {
	private final Map<Achievement, Long> unlocked = new EnumMap<>(Achievement.class);

	// names we do not know any more are left out
	public static AchievementBook of(Map<String, Long> saved) {
		AchievementBook book = new AchievementBook();
		saved.forEach((name, at) -> {
			for (Achievement achievement : Achievement.values()) {
				if (achievement.name().equalsIgnoreCase(name) && at != null) {
					book.unlocked.put(achievement, at);
				}
			}
		});
		return book;
	}

	public Map<String, Long> saved() {
		Map<String, Long> saved = new LinkedHashMap<>();
		unlocked.forEach((achievement, at) -> saved.put(achievement.name().toLowerCase(Locale.ROOT), at));
		return saved;
	}

	public boolean has(Achievement achievement) {
		return unlocked.containsKey(achievement);
	}

	// null while it is locked
	public Long at(Achievement achievement) {
		return unlocked.get(achievement);
	}

	public int count() {
		return unlocked.size();
	}

	// unlocks what the facts earn, returns the new ones rarest first
	public List<Achievement> update(AchievementFacts facts, long now) {
		List<Achievement> fresh = new ArrayList<>();
		for (Achievement achievement : Achievement.values()) {
			if (!unlocked.containsKey(achievement) && achievement.earned(facts)) {
				unlocked.put(achievement, now);
				fresh.add(achievement);
			}
		}
		fresh.sort(Comparator.comparing(Achievement::rarity).reversed());
		return fresh;
	}
}
