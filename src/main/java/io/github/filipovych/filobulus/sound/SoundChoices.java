package io.github.filipovych.filobulus.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

// what plays for each slot and how loud, a key is a built in sound, "none", or "file:" and the name of an .ogg in the sounds folder
public final class SoundChoices {
	public static final String NONE = "none";
	public static final String FILE = "file:";
	public static final int FULL = 100;

	public record Choice(String key, int volume) {
		public Choice {
			key = key == null || key.isBlank() ? NONE : key;
			volume = Math.max(0, Math.min(FULL, volume));
		}

		public boolean isFile() {
			return key.startsWith(FILE);
		}

		public String fileName() {
			return isFile() ? key.substring(FILE.length()) : "";
		}
	}

	private final Map<SoundSlot, Choice> choices = new EnumMap<>(SoundSlot.class);

	public Choice get(SoundSlot slot) {
		return choices.getOrDefault(slot, new Choice(slot.fallback(), FULL));
	}

	public void set(SoundSlot slot, Choice choice) {
		choices.put(slot, choice);
	}

	public String toJson() {
		JsonObject root = new JsonObject();
		for (SoundSlot slot : SoundSlot.values()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("sound", get(slot).key());
			entry.addProperty("volume", get(slot).volume());
			root.add(slot.name().toLowerCase(Locale.ROOT), entry);
		}
		return root.toString();
	}

	// an unknown slot is skipped, a bad file gives the defaults
	public static SoundChoices fromJson(String json) {
		try {
			return parse(json);
		} catch (RuntimeException e) {
			return new SoundChoices();
		}
	}

	// same but a bad file throws, so the caller can keep a copy of it
	public static SoundChoices parse(String json) {
		SoundChoices choices = new SoundChoices();
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
			for (SoundSlot slot : SoundSlot.values()) {
				if (slot.name().equalsIgnoreCase(entry.getKey()) && entry.getValue().isJsonObject()) {
					JsonObject value = entry.getValue().getAsJsonObject();
					choices.set(slot, new Choice(value.has("sound") ? value.get("sound").getAsString() : slot.fallback(),
							value.has("volume") ? value.get("volume").getAsInt() : FULL));
				}
			}
		}
		return choices;
	}
}
