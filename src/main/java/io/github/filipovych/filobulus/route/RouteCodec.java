package io.github.filipovych.filobulus.route;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

// coleweight json (read + write) and skyblocker export strings (read only)
public final class RouteCodec {
	public static final String SKYBLOCKER_PREFIX = "[Skyblocker-Waypoint-Data-V1]";

	// pasted text can be anything, dont inflate past this
	private static final int MAX_INFLATED_BYTES = 4 * 1024 * 1024;

	private RouteCodec() {
	}

	public static List<Route> parseAny(String text, String fallbackName) throws RouteFormatException {
		if (text == null || text.isBlank()) {
			throw new RouteFormatException("Nothing to import.");
		}
		String trimmed = stripBom(text).strip();
		if (trimmed.startsWith(SKYBLOCKER_PREFIX)) {
			return parseSkyblocker(trimmed);
		}
		if (trimmed.startsWith("[")) {
			return List.of(new Route(fallbackName, parseColeweight(trimmed)));
		}
		throw new RouteFormatException("Not a route this mod can read. Supported: Coleweight JSON and Skyblocker export strings.");
	}

	public static List<Waypoint> parseColeweight(String json) throws RouteFormatException {
		JsonArray array = parseArray(stripBom(json), "route");
		List<Waypoint> stops = new ArrayList<>(array.size());
		for (int i = 0; i < array.size(); i++) {
			JsonObject entry = asObject(array.get(i), "Stop " + (i + 1));
			String where = "Stop " + (i + 1);
			int x = coordinate(entry.get("x"), where, "x");
			int y = coordinate(entry.get("y"), where, "y");
			int z = coordinate(entry.get("z"), where, "z");
			String label = null;
			Optional<Waypoint.Aim> aim = Optional.empty();
			JsonElement options = entry.get("options");
			if (options != null && options.isJsonObject()) {
				label = label(options.getAsJsonObject().get("name"));
				// our own addition, other mods leave unknown options alone
				JsonElement target = options.getAsJsonObject().get("aim");
				if (target != null && target.isJsonObject()) {
					JsonObject at = target.getAsJsonObject();
					aim = Optional.of(new Waypoint.Aim(coordinate(at.get("x"), where, "aim x"),
							coordinate(at.get("y"), where, "aim y"), coordinate(at.get("z"), where, "aim z")));
				}
			}
			stops.add(new Waypoint(x, y, z, label == null ? Integer.toString(i + 1) : label, aim));
		}
		if (stops.isEmpty()) {
			throw new RouteFormatException("The route has no stops.");
		}
		return stops;
	}

	public static String toColeweight(List<Waypoint> stops) {
		JsonArray array = new JsonArray();
		for (Waypoint stop : stops) {
			JsonObject entry = new JsonObject();
			entry.addProperty("x", stop.x());
			entry.addProperty("y", stop.y());
			entry.addProperty("z", stop.z());
			// format wants a colour, importers ignore it anyway
			entry.addProperty("r", 0.0);
			entry.addProperty("g", 1.0);
			entry.addProperty("b", 0.0);
			JsonObject options = new JsonObject();
			options.addProperty("name", stop.label());
			stop.aim().ifPresent(aim -> {
				JsonObject at = new JsonObject();
				at.addProperty("x", aim.x());
				at.addProperty("y", aim.y());
				at.addProperty("z", aim.z());
				options.add("aim", at);
			});
			entry.add("options", options);
			array.add(entry);
		}
		return array.toString();
	}

	public static List<Route> parseSkyblocker(String export) throws RouteFormatException {
		String trimmed = export.strip();
		if (!trimmed.startsWith(SKYBLOCKER_PREFIX)) {
			throw new RouteFormatException("Not a Skyblocker export string.");
		}
		byte[] compressed;
		try {
			compressed = Base64.getDecoder().decode(trimmed.substring(SKYBLOCKER_PREFIX.length()).strip());
		} catch (IllegalArgumentException e) {
			throw new RouteFormatException("The Skyblocker string is damaged (bad Base64).", e);
		}
		String json;
		try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
			json = new String(readLimited(in), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RouteFormatException("The Skyblocker string is damaged (cannot be unpacked).", e);
		}

		JsonArray groups = parseArray(json, "Skyblocker data");
		List<Route> routes = new ArrayList<>();
		for (int g = 0; g < groups.size(); g++) {
			JsonObject group = asObject(groups.get(g), "Group " + (g + 1));
			String name = label(group.get("name"));
			JsonElement waypoints = group.get("waypoints");
			if (waypoints == null || !waypoints.isJsonArray()) {
				continue;
			}
			JsonArray points = waypoints.getAsJsonArray();
			List<Waypoint> stops = new ArrayList<>(points.size());
			for (int i = 0; i < points.size(); i++) {
				String where = "Group " + (g + 1) + ", stop " + (i + 1);
				JsonObject point = asObject(points.get(i), where);
				JsonElement pos = point.get("pos");
				if (pos == null || !pos.isJsonArray() || pos.getAsJsonArray().size() != 3) {
					throw new RouteFormatException(where + " has no position.");
				}
				JsonArray xyz = pos.getAsJsonArray();
				String label = label(point.get("name"));
				stops.add(new Waypoint(
						coordinate(xyz.get(0), where, "x"),
						coordinate(xyz.get(1), where, "y"),
						coordinate(xyz.get(2), where, "z"),
						label == null ? Integer.toString(i + 1) : label));
			}
			if (!stops.isEmpty()) {
				routes.add(new Route(name == null ? "Route " + (g + 1) : name, stops));
			}
		}
		if (routes.isEmpty()) {
			throw new RouteFormatException("The Skyblocker string has no waypoints.");
		}
		return routes;
	}

	private static String stripBom(String text) {
		return !text.isEmpty() && text.charAt(0) == '\ufeff' ? text.substring(1) : text;
	}

	private static JsonArray parseArray(String json, String what) throws RouteFormatException {
		JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (JsonParseException e) {
			throw new RouteFormatException("The " + what + " is not valid JSON.", e);
		}
		if (!root.isJsonArray()) {
			throw new RouteFormatException("The " + what + " must be a JSON list.");
		}
		return root.getAsJsonArray();
	}

	private static JsonObject asObject(JsonElement element, String where) throws RouteFormatException {
		if (element == null || !element.isJsonObject()) {
			throw new RouteFormatException(where + " is not a waypoint.");
		}
		return element.getAsJsonObject();
	}

	private static int coordinate(JsonElement value, String where, String axis) throws RouteFormatException {
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			throw new RouteFormatException(where + " has no " + axis + " coordinate.");
		}
		double number = value.getAsDouble();
		if (Double.isNaN(number) || Math.abs(number) > 30_000_000) {
			throw new RouteFormatException(where + " has an impossible " + axis + " coordinate.");
		}
		return (int) Math.floor(number);
	}

	// name can be a string or a number, whole numbers without the .0
	private static String label(JsonElement value) {
		if (value == null || !value.isJsonPrimitive()) {
			return null;
		}
		JsonPrimitive primitive = value.getAsJsonPrimitive();
		if (primitive.isNumber()) {
			double number = primitive.getAsDouble();
			if (number == Math.rint(number) && Math.abs(number) < 1e15) {
				return Long.toString((long) number);
			}
		}
		return primitive.getAsString();
	}

	private static byte[] readLimited(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int read;
		while ((read = in.read(buffer)) != -1) {
			if (out.size() + read > MAX_INFLATED_BYTES) {
				throw new IOException("Too large");
			}
			out.write(buffer, 0, read);
		}
		return out.toByteArray();
	}
}
