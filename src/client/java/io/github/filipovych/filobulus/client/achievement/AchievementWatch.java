package io.github.filipovych.filobulus.client.achievement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.achievement.Achievement;
import io.github.filipovych.filobulus.achievement.AchievementBook;
import io.github.filipovych.filobulus.achievement.AchievementFacts;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.alert.Alerts;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.JournalConfig;
import io.github.filipovych.filobulus.client.hud.Frost;
import io.github.filipovych.filobulus.journal.ShaftJournal;
import io.github.filipovych.filobulus.sound.SoundSlot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// unlocks achievements as the journal grows, config/filobulus/achievements.json keeps when each one came
// what the journal already earned before this start, or while they were switched off, is unlocked quietly
// so a first start, or an update that brings new ones, does not flood the chat
public final class AchievementWatch {
	private final Path file;
	private AchievementBook book = new AchievementBook();
	private AchievementFacts facts = AchievementFacts.NONE;
	private int factsVersion = -1;
	private int checkedVersion = -1;
	private boolean quiet = true;

	public AchievementWatch(Path file) {
		this.file = file;
	}

	public void load() {
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			Map<String, Long> saved = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
				saved.put(entry.getKey(), entry.getValue().getAsLong());
			}
			book = AchievementBook.of(saved);
		} catch (IOException | RuntimeException e) {
			FilobulusClient.LOGGER.error("Could not read {}", file, e);
		}
	}

	public AchievementBook book() {
		return book;
	}

	// counted again only when the journal changed
	public AchievementFacts facts() {
		ShaftJournal journal = FilobulusClient.journal().journal();
		if (journal.version() != factsVersion) {
			factsVersion = journal.version();
			facts = AchievementFacts.of(journal.visits(), ZoneId.systemDefault());
		}
		return facts;
	}

	public void tick(Minecraft minecraft) {
		if (!FilobulusConfig.enabled || !JournalConfig.achievements) {
			quiet = true;
			checkedVersion = -1;
			return;
		}
		int version = FilobulusClient.journal().journal().version();
		if (version == checkedVersion) {
			return;
		}
		checkedVersion = version;
		List<Achievement> fresh = book.update(facts(), System.currentTimeMillis());
		if (!fresh.isEmpty()) {
			if (!quiet) {
				announce(minecraft, fresh);
			}
			save();
		}
		quiet = false;
	}

	// one alert for the rarest, a chat line for each
	private void announce(Minecraft minecraft, List<Achievement> fresh) {
		Achievement best = fresh.getFirst();
		if (AlertsConfig.achievements) {
			String more = fresh.size() > 1 ? " (+" + (fresh.size() - 1) + " more)" : "";
			Alerts.show("achievement", best.rarity().displayName().toUpperCase(Locale.ROOT) + " ACHIEVEMENT",
					best.title() + more, best.rarity().color(), 3, Alerts.LONG, SoundSlot.ACHIEVEMENT);
			Frost.glint(Frost.ALL, best.rarity().color());
		}
		if (AlertsConfig.achievementChat) {
			for (Achievement achievement : fresh) {
				GameCompat.say(minecraft, chatLine(achievement));
			}
		}
	}

	public static Component chatLine(Achievement achievement) {
		return FilobulusClient.chatLine("Achievement unlocked: ")
				.append(Component.literal(achievement.title()).withColor(achievement.rarity().color()).withStyle(ChatFormatting.BOLD))
				.append(Component.literal(" (" + achievement.rarity().displayName() + "): " + achievement.description())
						.withStyle(ChatFormatting.GRAY));
	}

	private void save() {
		JsonObject root = new JsonObject();
		book.saved().forEach(root::addProperty);
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
