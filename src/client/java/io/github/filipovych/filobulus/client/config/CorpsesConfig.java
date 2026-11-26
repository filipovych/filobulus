package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;

@Category("corpses")
@ConfigInfo(title = "Corpses", description = "What the loot of a corpse was worth against its key. Move it with /filo hud.", icon = "skull")
public final class CorpsesConfig {
	@ConfigEntry(id = "reveal", translation = "filobulus.config.corpses.reveal")
	@Comment("When you loot a corpse in a mineshaft, a block of ice cracks open and shows how the loot did against the price of the key, from dirt to jasper. Only shows for tungsten, umber and vanguard corpses. Prices come from Hypixel's public bazaar list. /filo crack plays it.")
	public static boolean reveal = true;

	@ConfigEntry(id = "revealSounds", translation = "filobulus.config.corpses.revealSounds")
	@Comment("Play the cracking sounds and the sound of the reveal. Which sound each tier plays is picked on the Sounds page.")
	public static boolean revealSounds = true;

	@ConfigEntry(id = "muteOrganDonor", translation = "filobulus.config.corpses.muteOrganDonor")
	@Comment("Mute the ding of the Organ Donor accessory in a mineshaft. Once all opened waits until every corpse of the shaft is looted, so the ding still helps you find them.")
	public static Mute muteOrganDonor = Mute.IN_SHAFTS;

	public enum Mute {
		NEVER("Never"),
		IN_SHAFTS("In shafts"),
		ONCE_ALL_OPENED("Once all opened");

		private final String title;

		Mute(String title) {
			this.title = title;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	private CorpsesConfig() {
	}
}
