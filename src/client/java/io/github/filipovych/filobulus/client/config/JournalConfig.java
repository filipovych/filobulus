package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigButton;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.screen.JournalScreen;
import net.minecraft.client.Minecraft;

@Category("journal")
@ConfigInfo(title = "Journal", description = "A log of every mineshaft you enter.", icon = "book-open")
public final class JournalConfig {
	@ConfigEntry(id = "journal", translation = "filobulus.config.journal.journal")
	@Comment("Write every mineshaft you enter to the journal: its type, when, and how long you stayed.")
	public static boolean journal = true;

	@ConfigButton(title = "Shaft journal", text = "Open")
	@Comment("Every shaft you were in, with totals per shaft type. Also at /filo journal.")
	public static final Runnable openJournal = () -> {
		Minecraft minecraft = Minecraft.getInstance();
		GameCompat.setScreen(minecraft, new JournalScreen(GameCompat.screen(minecraft)));
	};

	private JournalConfig() {
	}
}
