package io.github.filipovych.filobulus.route;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// the routes that ship with the mod, none of them for a shaft: today only the glacite pickobulus spots
// one file each under filobulus/routes, in the same shape the custom routes are saved in
public final class BundledRoutes {
	private static final String FOLDER = "/filobulus/routes/";
	private static final List<String> FILES = List.of("glacite_pickobulus");

	private BundledRoutes() {
	}

	public static List<CustomRoute> load() throws IOException, RouteFormatException {
		List<CustomRoute> routes = new ArrayList<>();
		for (String file : FILES) {
			try (InputStream in = BundledRoutes.class.getResourceAsStream(FOLDER + file + ".json")) {
				if (in == null) {
					throw new IOException("missing " + file + ".json");
				}
				routes.add(read(JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject()));
			}
		}
		return routes;
	}

	private static CustomRoute read(JsonObject entry) throws RouteFormatException {
		List<Waypoint> stops = CollectionCodec.stopsFromJson(entry.get("stops"));
		if (stops.isEmpty()) {
			throw new RouteFormatException("A bundled route has no stops.");
		}
		LibraryRoute route = new LibraryRoute(text(entry, "id"), text(entry, "name"), CustomRoute.KEY, stops,
				text(entry, "name"), text(entry, "author"), true);
		return new CustomRoute(route, text(entry, "icon"), text(entry, "area"),
				!entry.has("loop") || entry.get("loop").getAsBoolean(), false);
	}

	private static String text(JsonObject entry, String key) {
		return entry.has(key) && entry.get(key).isJsonPrimitive() ? entry.get(key).getAsString() : "";
	}
}
