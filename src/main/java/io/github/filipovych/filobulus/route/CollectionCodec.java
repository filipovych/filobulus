package io.github.filipovych.filobulus.route;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

// our own format for a whole set of routes: the prefix, then base64 of gzipped json
// {"name":"..","routes":[{"shaft":"JASP_1","name":"..","author":"..","stops":[[x,y,z],..]}]}
public final class CollectionCodec {
	public static final String PREFIX = "[Filobulus-Routes-V1]";

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
		JsonArray stops = new JsonArray();
		for (int i = 0; i < route.stops().size(); i++) {
			Waypoint stop = route.stops().get(i);
			JsonArray point = new JsonArray();
			point.add(stop.x());
			point.add(stop.y());
			point.add(stop.z());
			// most routes are just numbered in order, only a label of its own is written
			if (!stop.label().equals(Integer.toString(i + 1))) {
				point.add(stop.label());
			}
			stops.add(point);
		}
		entry.add("stops", stops);
		return entry;
	}
}
