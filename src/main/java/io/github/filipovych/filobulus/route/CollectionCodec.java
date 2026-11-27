package io.github.filipovych.filobulus.route;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

// our own format for a whole set of routes: the prefix, then base64 of gzipped json
// {"name":"..","routes":[{"shaft":"JASP_1","name":"..","author":"..","stops":[[x,y,z],..]}]}
// a stop can carry a label and after it an aim, [x,y,z,"label",[x,y,z]]
public final class CollectionCodec {
	public static final String PREFIX = "[Filobulus-Routes-V1]";

	private static final int MAX_ROUTES = 200;
	private static final int MAX_STOPS = 2000;
	private static final int MAX_INFLATED_BYTES = 4 * 1024 * 1024;

	private CollectionCodec() {
	}

	public static String export(RouteCollection collection) {
		byte[] json = toJson(collection).toString().getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream packed = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(packed)) {
			gzip.write(json);
		} catch (IOException e) {
			// cannot happen when writing to memory
			throw new IllegalStateException(e);
		}
		return PREFIX + Base64.getEncoder().encodeToString(packed.toByteArray());
	}

	// takes the export string or the plain json inside it
	public static RouteCollection parse(String text) throws RouteFormatException {
		String trimmed = text.strip();
		if (!trimmed.startsWith(PREFIX)) {
			return fromJson(trimmed);
		}
		byte[] packed;
		try {
			packed = Base64.getDecoder().decode(trimmed.substring(PREFIX.length()).strip());
		} catch (IllegalArgumentException e) {
			throw new RouteFormatException("The route collection is damaged (bad Base64).", e);
		}
		byte[] json;
		try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(packed))) {
			json = gzip.readNBytes(MAX_INFLATED_BYTES + 1);
		} catch (IOException e) {
			throw new RouteFormatException("The route collection is damaged (cannot be unpacked).", e);
		}
		if (json.length > MAX_INFLATED_BYTES) {
			throw new RouteFormatException("The route collection is too large.");
		}
		return fromJson(new String(json, StandardCharsets.UTF_8));
	}

	public static JsonObject toJson(RouteCollection collection) {
		JsonObject root = new JsonObject();
		root.addProperty("name", collection.name());
		JsonArray routes = new JsonArray();
		for (SharedRoute route : collection.routes()) {
			routes.add(toJson(route));
		}
		root.add("routes", routes);
		return root;
	}

	public static JsonObject toJson(SharedRoute route) {
		JsonObject entry = new JsonObject();
		entry.addProperty("shaft", route.shaft());
		entry.addProperty("name", route.name());
		if (!route.author().isEmpty()) {
			entry.addProperty("author", route.author());
		}
		entry.add("stops", stopsToJson(route.stops()));
		return entry;
	}

	public static JsonArray stopsToJson(List<Waypoint> route) {
		JsonArray stops = new JsonArray();
		for (int i = 0; i < route.size(); i++) {
			Waypoint stop = route.get(i);
			JsonArray point = new JsonArray();
			point.add(stop.x());
			point.add(stop.y());
			point.add(stop.z());
			// most routes are just numbered in order, only a label of its own is written, and the aim after it
			if (!stop.label().equals(Integer.toString(i + 1)) || stop.aim().isPresent()) {
				point.add(stop.label());
			}
			stop.aim().ifPresent(aim -> {
				JsonArray target = new JsonArray();
				target.add(aim.x());
				target.add(aim.y());
				target.add(aim.z());
				point.add(target);
			});
			stops.add(point);
		}
		return stops;
	}

	private static RouteCollection fromJson(String json) throws RouteFormatException {
		JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (JsonParseException e) {
			throw new RouteFormatException("The route collection is not valid JSON.", e);
		}
		if (!root.isJsonObject() || !root.getAsJsonObject().has("routes")
				|| !root.getAsJsonObject().get("routes").isJsonArray()) {
			throw new RouteFormatException("Not a route collection.");
		}
		JsonArray entries = root.getAsJsonObject().getAsJsonArray("routes");
		if (entries.size() > MAX_ROUTES) {
			throw new RouteFormatException("The route collection has too many routes.");
		}
		List<SharedRoute> routes = new ArrayList<>();
		for (JsonElement entry : entries) {
			// a route for a shaft this version does not know is left out, the rest still comes in
			if (entry.isJsonObject()) {
				routeFromJson(entry.getAsJsonObject()).ifPresent(routes::add);
			}
		}
		if (routes.isEmpty()) {
			throw new RouteFormatException("The route collection has no routes this version can use.");
		}
		return new RouteCollection(text(root.getAsJsonObject().get("name")), routes);
	}

	// empty when the shaft is not one we know or there are no stops
	public static Optional<SharedRoute> routeFromJson(JsonObject entry) throws RouteFormatException {
		Optional<ShaftLayout> layout = ShaftLayout.byKey(text(entry.get("shaft")));
		JsonElement points = entry.get("stops");
		if (layout.isEmpty() || points == null || !points.isJsonArray() || points.getAsJsonArray().isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new SharedRoute(
				layout.get().key(), text(entry.get("name")), text(entry.get("author")), stopsFromJson(points)));
	}

	// empty when there is nothing to read
	public static List<Waypoint> stopsFromJson(JsonElement points) throws RouteFormatException {
		if (points == null || !points.isJsonArray()) {
			return List.of();
		}
		JsonArray array = points.getAsJsonArray();
		if (array.size() > MAX_STOPS) {
			throw new RouteFormatException("A route in the collection has too many stops.");
		}
		List<Waypoint> stops = new ArrayList<>(array.size());
		for (int i = 0; i < array.size(); i++) {
			JsonElement point = array.get(i);
			if (!point.isJsonArray() || point.getAsJsonArray().size() < 3) {
				throw new RouteFormatException("A stop in the collection has no position.");
			}
			JsonArray xyz = point.getAsJsonArray();
			String label = xyz.size() > 3 ? text(xyz.get(3)) : Integer.toString(i + 1);
			Optional<Waypoint.Aim> aim = Optional.empty();
			if (xyz.size() > 4 && xyz.get(4).isJsonArray() && xyz.get(4).getAsJsonArray().size() == 3) {
				JsonArray target = xyz.get(4).getAsJsonArray();
				aim = Optional.of(new Waypoint.Aim(whole(target.get(0)), whole(target.get(1)), whole(target.get(2))));
			}
			stops.add(new Waypoint(whole(xyz.get(0)), whole(xyz.get(1)), whole(xyz.get(2)), label, aim));
		}
		return stops;
	}

	private static int whole(JsonElement value) throws RouteFormatException {
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()
				|| Math.abs(value.getAsDouble()) > 30_000_000) {
			throw new RouteFormatException("A stop in the collection has a bad coordinate.");
		}
		return (int) Math.floor(value.getAsDouble());
	}

	private static String text(JsonElement value) {
		return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
	}
}
