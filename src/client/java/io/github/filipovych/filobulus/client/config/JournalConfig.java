package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;

@Category("journal")
@ConfigInfo(title = "Journal", description = "A log of every mineshaft you enter.", icon = "book-open")
public final class JournalConfig {
	@ConfigEntry(id = "journal", translation = "filobulus.config.journal.journal")
	@Comment("Write every mineshaft you enter to the journal: its type, when, and how long you stayed.")
	public static boolean journal = true;

	private JournalConfig() {
	}
}
