package io.github.filipovych.filobulus.journal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.corpse.CorpseTally;
import java.util.Optional;

// a visit as one line of the journal file
public final class JournalCodec {
	private JournalCodec() {
	}

	public static String write(ShaftVisit visit) {
		JsonObject line = new JsonObject();
		line.addProperty("at", visit.enteredAt());
		line.addProperty("type", visit.code());
		if (visit.spentMillis() > 0) {
			line.addProperty("spent", visit.spentMillis());
		}
		if (visit.runMillis() > 0) {
			line.addProperty("run", visit.runMillis());
		}
		if (!visit.corpses().isEmpty()) {
			line.addProperty("corpses", visit.corpses().seen());
			line.addProperty("looted", visit.corpses().looted());
		}
		if (visit.corpseCoins() != 0) {
			line.addProperty("corpseCoins", visit.corpseCoins());
		}
		if (visit.minedCoins() != 0) {
			line.addProperty("mined", visit.minedCoins());
		}
		if (visit.joined()) {
			line.addProperty("joined", true);
		}
		return line.toString();
	}

	private static String text(JsonObject json, String key) {
		return json.has(key) ? json.get(key).getAsString() : "";
	}

	// empty for a line that is not a visit, one bad line must not cost the rest of the file
	public static Optional<ShaftVisit> read(String line) {
		try {
			JsonObject json = JsonParser.parseString(line).getAsJsonObject();
			long at = json.get("at").getAsLong();
			if (at <= 0) {
				return Optional.empty();
			}
			return Optional.of(new ShaftVisit(at,
					json.has("type") ? json.get("type").getAsString() : "",
					json.has("spent") ? Math.max(0, json.get("spent").getAsLong()) : 0,
					json.has("run") ? Math.max(0, json.get("run").getAsLong()) : 0,
					new CorpseTally(text(json, "corpses"), text(json, "looted")),
					json.has("corpseCoins") ? json.get("corpseCoins").getAsLong() : 0,
					json.has("mined") ? json.get("mined").getAsLong() : 0,
					json.has("joined") && json.get("joined").getAsBoolean()));
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}
}
