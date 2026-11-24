package io.github.filipovych.filobulus.client.alert;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

// the sounds an alert can be given in the settings, all of them the game's own
public enum AlertSound {
	LEVEL_UP("Level up", () -> SoundEvents.PLAYER_LEVELUP),
	CHALLENGE("Challenge complete", () -> SoundEvents.UI_TOAST_CHALLENGE_COMPLETE),
	PLING("Pling", () -> SoundEvents.NOTE_BLOCK_PLING.value()),
	BELL("Bell", () -> SoundEvents.NOTE_BLOCK_BELL.value());

	private final String title;
	private final Supplier<SoundEvent> event;

	AlertSound(String title, Supplier<SoundEvent> event) {
		this.title = title;
		this.event = event;
	}

	public SoundEvent event() {
		return event.get();
	}

	// what the settings screen shows
	@Override
	public String toString() {
		return title;
	}
}
