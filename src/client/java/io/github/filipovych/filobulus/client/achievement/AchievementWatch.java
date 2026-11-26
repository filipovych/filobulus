package io.github.filipovych.filobulus.client.achievement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.achievement.Achievement;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.alert.Alerts;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.hud.Frost;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.sound.SoundSlot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;

// unlocks achievements as the journal grows, config/filobulus/achievements.json keeps when each one came
// what was already earned before the first start with achievements is unlocked quietly
public final class AchievementWatch {
	private static final int COLOR = 0xFFD75E;

	private final Path file;
	private final Map<Achievement, Long> unlocked = new EnumMap<>(Achievement.class);
	private int checkedVersion = -1;
	private boolean first = true;

	public AchievementWatch(Path file) {
		this.file = file;
	}

	public void load() {
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			first = false;
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				for (Achievement achievement : Achievement.values()) {
					if (achievement.name().equalsIgnoreCase(entry.getKey())) {
						unlocked.put(achievement, entry.getValue().getAsLong());
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			FilobulusClient.LOGGER.error("Could not read {}", file, e);
		}
	}

	public Map<Achievement, Long> unlocked() {
		return Map.copyOf(unlocked);
	}

	public void tick(Minecraft minecraft) {
		int version = FilobulusClient.journal().journal().version();
		if (version == checkedVersion) {
			return;
		}
		checkedVersion = version;
		List<ShaftVisit> visits = FilobulusClient.journal().journal().visits();
		boolean changed = false;
		for (Achievement achievement : Achievement.values()) {
			if (unlocked.containsKey(achievement) || !achievement.earned(visits)) {
				continue;
			}
			unlocked.put(achievement, System.currentTimeMillis());
			changed = true;
			if (!first) {
				announce(minecraft, achievement);
			}
		}
		first = false;
		if (changed) {
			save();
		}
	}

	private void announce(Minecraft minecraft, Achievement achievement) {
		if (AlertsConfig.achievements) {
			Alerts.show("achievement", "ACHIEVEMENT", achievement.title() + ": " + achievement.description(), COLOR, 3,
					Alerts.LONG, SoundSlot.ACHIEVEMENT);
			Frost.glint(Frost.ALL, COLOR);
		}
		GameCompat.say(minecraft, FilobulusClient.chatLine("Achievement unlocked: " + achievement.title()
				+ " (" + achievement.description() + ")"));
	}

	private void save() {
		JsonObject root = new JsonObject();
		unlocked.forEach((achievement, at) -> root.addProperty(achievement.name().toLowerCase(Locale.ROOT), at));
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling("achievements.json.tmp");
			Files.writeString(temp, root + "\n", StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not save {}", file, e);
		}
	}
}
