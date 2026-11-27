package io.github.filipovych.filobulus.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// 0.3 kept the sounds switch under alerts, this tells if that old switch was off and the new one was never written
public final class OldSoundSwitch {
	private OldSoundSwitch() {
	}

	public static boolean wasMuted(String jsonc) {
		try {
			JsonObject root = JsonParser.parseString(jsonc).getAsJsonObject();
			if (root.has("sounds")) {
				return false;
			}
			JsonElement alerts = root.get("alerts");
			if (alerts == null || !alerts.isJsonObject()) {
				return false;
			}
			JsonElement old = alerts.getAsJsonObject().get("sounds");
			return old != null && old.isJsonPrimitive() && old.getAsJsonPrimitive().isBoolean() && !old.getAsBoolean();
		} catch (RuntimeException e) {
			return false;
		}
	}
}
