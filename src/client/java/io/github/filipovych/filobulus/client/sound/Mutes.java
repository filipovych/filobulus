package io.github.filipovych.filobulus.client.sound;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.CorpsesConfig;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.route.Location.Area;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;

// sounds of the game we leave out, only on our side
public final class Mutes {
	// the organ donor accessory dings with a harp note while an unlooted corpse is near
	private static final Identifier HARP = Identifier.withDefaultNamespace("block.note_block.harp");

	private Mutes() {
	}

	public static boolean muted(SoundInstance sound) {
		try {
			return FilobulusConfig.enabled && organDonor(sound);
		} catch (RuntimeException e) {
			// never let a mute break the sound of the game
			return false;
		}
	}

	private static boolean organDonor(SoundInstance sound) {
		if (CorpsesConfig.muteOrganDonor == CorpsesConfig.Mute.NEVER || !HARP.equals(sound.getIdentifier())
				// our own sounds play at the player and are never muted
				|| sound.isRelative()
				|| FilobulusClient.tracker().location().area() != Area.MINESHAFT) {
			return false;
		}
		if (CorpsesConfig.muteOrganDonor == CorpsesConfig.Mute.IN_SHAFTS) {
			return true;
		}
		// only once every corpse of the shaft is opened, until then the ding still helps find them
		ShaftVisit open = FilobulusClient.shaftLog().open();
		CorpseTally corpses = open == null ? CorpseTally.NONE : open.corpses();
		return !corpses.isEmpty() && corpses.looted().length() >= corpses.seen().length();
	}
}
