package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigButton;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.screen.JournalScreen;
import net.minecraft.client.Minecraft;

@Category("journal")
@ConfigInfo(title = "Journal", description = "A log of every mineshaft you enter.", icon = "book-open")
public final class JournalConfig {
	@ConfigEntry(id = "journal", translation = "filobulus.config.journal.journal")
	@Comment("Write every mineshaft you enter to the journal: its type, when, and how long you stayed.")
	public static boolean journal = true;

	@ConfigEntry(id = "recap", translation = "filobulus.config.journal.recap")
	@Comment("When you leave a mineshaft, show a card for a few seconds: the time inside, the route time, the corpses you opened and what the shaft paid. /filo recap shows the last one again. Move it with /filo hud.")
	public static boolean recap = true;

	@ConfigEntry(id = "recapSeconds", translation = "filobulus.config.journal.recapSeconds")
	@Comment("How many seconds the card stays.")
	@ConfigOption.Range(min = 3, max = 30)
	@ConfigOption.Slider
	public static int recapSeconds = 10;

	@ConfigButton(title = "Shaft journal", text = "Open")
	@Comment("Every shaft you were in, with totals per shaft type. Also at /filo journal.")
	public static final Runnable openJournal = () -> {
		Minecraft minecraft = Minecraft.getInstance();
		GameCompat.setScreen(minecraft, new JournalScreen(GameCompat.screen(minecraft)));
	};

	private JournalConfig() {
	}
}
