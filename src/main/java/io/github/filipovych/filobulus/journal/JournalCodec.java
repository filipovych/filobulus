package io.github.filipovych.filobulus.journal;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
		return line.toString();
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
					json.has("run") ? Math.max(0, json.get("run").getAsLong()) : 0));
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}
}
