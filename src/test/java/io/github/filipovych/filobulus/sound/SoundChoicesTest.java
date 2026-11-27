package io.github.filipovych.filobulus.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.sound.SoundChoices.Choice;
import org.junit.jupiter.api.Test;

class SoundChoicesTest {
	@Test
	void anUnsetSlotPlaysItsOwnSoundAtFullVolume() {
		assertEquals(new Choice("level_up", 100), new SoundChoices().get(SoundSlot.JASPER_SHAFT));
	}

	@Test
	void choicesSurviveTheFile() {
		SoundChoices choices = new SoundChoices();
		choices.set(SoundSlot.CORPSE_DIRT, new Choice("file:sad trombone.ogg", 60));
		choices.set(SoundSlot.PORTAL, new Choice("none", 100));
		SoundChoices read = SoundChoices.fromJson(choices.toJson());
		assertEquals(new Choice("file:sad trombone.ogg", 60), read.get(SoundSlot.CORPSE_DIRT));
		assertTrue(read.get(SoundSlot.CORPSE_DIRT).isFile());
		assertEquals("sad trombone.ogg", read.get(SoundSlot.CORPSE_DIRT).fileName());
		assertEquals("none", read.get(SoundSlot.PORTAL).key());
	}

	@Test
	void volumeStaysInRangeAndABadFileGivesTheDefaults() {
		assertEquals(0, new Choice("bell", -20).volume());
		assertEquals(100, new Choice("bell", 300).volume());
		assertEquals("none", new Choice("", 50).key());
		assertEquals(new Choice("pling", 100), SoundChoices.fromJson("not json").get(SoundSlot.WISP_GONE));
	}

	@Test
	void parseThrowsOnABadFile() {
		assertThrows(RuntimeException.class, () -> SoundChoices.parse("not json"));
		assertThrows(RuntimeException.class, () -> SoundChoices.parse("{\"portal\": {\"volume\": \"loud\"}}"));
	}
}
