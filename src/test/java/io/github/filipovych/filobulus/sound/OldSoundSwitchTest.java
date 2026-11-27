package io.github.filipovych.filobulus.sound;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OldSoundSwitchTest {
	@Test
	void anOldFileWithSoundsOffIsMuted() {
		String file = """
				{
				  "enabled": true,
				  "alerts": {
				    // Play a sound with the alerts that have one.
				    "sounds": false,
				    "newBest": true
				  }
				}
				""";
		assertTrue(OldSoundSwitch.wasMuted(file));
	}

	@Test
	void soundsOnOrAlreadyMovedIsLeftAlone() {
		assertFalse(OldSoundSwitch.wasMuted("{\"alerts\": {\"sounds\": true}}"));
		assertFalse(OldSoundSwitch.wasMuted("{\"alerts\": {\"sounds\": false}, \"sounds\": {\"sounds\": true}}"));
		assertFalse(OldSoundSwitch.wasMuted("{\"alerts\": {}}"));
		assertFalse(OldSoundSwitch.wasMuted("{}"));
	}

	@Test
	void aBrokenFileDoesNothing() {
		assertFalse(OldSoundSwitch.wasMuted("not json {"));
		assertFalse(OldSoundSwitch.wasMuted(""));
		assertFalse(OldSoundSwitch.wasMuted("[1, 2]"));
	}
}
