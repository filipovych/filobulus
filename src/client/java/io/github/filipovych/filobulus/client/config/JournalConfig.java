package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigButton;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigOption;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.screen.JournalScreen;
import io.github.filipovych.filobulus.client.screen.RecapScreen;
import net.minecraft.client.Minecraft;

@Category("journal")
@ConfigInfo(title = "Journal", description = "A log of every mineshaft you enter.", icon = "book-open")
public final class JournalConfig {
	@ConfigEntry(id = "journal", translation = "filobulus.config.journal.journal")
	@Comment("Write every mineshaft you enter to the journal: its type, when, and how long you stayed.")
	public static boolean journal = true;

	@ConfigEntry(id = "party", translation = "filobulus.config.journal.party")
	@Comment("Show the shafts you joined (found by someone else, you came in through their portal) on a Party tab of the journal, and mark them in its log. A shaft counts as joined when the portal line was not in your chat in the two minutes before.")
	public static boolean party = true;

	// a new id, the old "recap" was on by default and this one starts off
	@ConfigEntry(id = "autoRecap", translation = "filobulus.config.journal.recap")
	@Comment("When you leave a mineshaft, show a small card for a few seconds: what the shaft paid, the rate, the time and the corpses. /filo recap always opens the full recap. Move the card with /filo hud.")
	public static boolean recap = false;

	@ConfigEntry(id = "recapSeconds", translation = "filobulus.config.journal.recapSeconds")
	@Comment("How many seconds the small card stays.")
	@ConfigOption.Range(min = 3, max = 30)
	@ConfigOption.Slider
	public static int recapSeconds = 10;

	@ConfigButton(title = "Shaft recap", text = "Open")
	@Comment("The last shaft you were in, and the ones before it. Also at /filo recap.")
	public static final Runnable openRecap = () -> {
		Minecraft minecraft = Minecraft.getInstance();
		GameCompat.setScreen(minecraft, new RecapScreen(GameCompat.screen(minecraft), 0));
	};

	@ConfigButton(title = "Shaft journal", text = "Open")
	@Comment("Every shaft you were in, with totals per shaft type. Also at /filo journal.")
	public static final Runnable openJournal = () -> {
		Minecraft minecraft = Minecraft.getInstance();
		GameCompat.setScreen(minecraft, new JournalScreen(GameCompat.screen(minecraft)));
	};

	private JournalConfig() {
	}
}
