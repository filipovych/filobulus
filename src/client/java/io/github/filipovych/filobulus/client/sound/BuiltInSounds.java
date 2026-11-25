package io.github.filipovych.filobulus.client.sound;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

// the game's own sounds a slot can play, by the key that is saved
public final class BuiltInSounds {
	public record BuiltIn(String title, Supplier<SoundEvent> event, float pitch) {
	}

	private static final Map<String, BuiltIn> SOUNDS = new LinkedHashMap<>();

	static {
		SOUNDS.put("level_up", new BuiltIn("Level up", () -> SoundEvents.PLAYER_LEVELUP, 1f));
		SOUNDS.put("challenge", new BuiltIn("Challenge complete", () -> SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f));
		SOUNDS.put("pling", new BuiltIn("Pling", () -> SoundEvents.NOTE_BLOCK_PLING.value(), 1f));
		SOUNDS.put("bell", new BuiltIn("Bell", () -> SoundEvents.NOTE_BLOCK_BELL.value(), 1f));
		SOUNDS.put("chime", new BuiltIn("Chime", () -> SoundEvents.NOTE_BLOCK_CHIME.value(), 1f));
		SOUNDS.put("bass", new BuiltIn("Bass", () -> SoundEvents.NOTE_BLOCK_BASS.value(), 0.5f));
		SOUNDS.put("xp", new BuiltIn("Experience orb", () -> SoundEvents.EXPERIENCE_ORB_PICKUP, 1f));
		SOUNDS.put("villager_no", new BuiltIn("Villager no", () -> SoundEvents.VILLAGER_NO, 0.7f));
		SOUNDS.put("glass", new BuiltIn("Glass break", () -> SoundEvents.GLASS_BREAK, 1f));
	}

	private BuiltInSounds() {
	}

	public static List<String> keys() {
		return List.copyOf(SOUNDS.keySet());
	}

	public static BuiltIn get(String key) {
		return SOUNDS.get(key);
	}
}
