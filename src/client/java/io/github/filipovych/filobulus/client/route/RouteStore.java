package io.github.filipovych.filobulus.client.route;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.route.CollectionCodec;
import io.github.filipovych.filobulus.route.LibraryRoute;
import io.github.filipovych.filobulus.route.RouteChoices;
import io.github.filipovych.filobulus.route.RouteFormatException;
import io.github.filipovych.filobulus.route.RouteLibrary;
import io.github.filipovych.filobulus.route.SharedRoute;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// the routes the player imported and what they picked on the routes screen, kept in config/filobulus/routes.json
public final class RouteStore {
	private final Path file;
	private final RouteLibrary library;
	private final RouteChoices choices = new RouteChoices();

	public RouteStore(Path file, RouteLibrary library) {
		this.file = file;
		this.library = library;
	}

	public RouteLibrary library() {
		return library;
	}

	public RouteChoices choices() {
		return choices;
	}

	public void load() {
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			List<String> autoLoad = new ArrayList<>();
			for (JsonElement key : root.getAsJsonArray("autoLoad")) {
				autoLoad.add(key.getAsString());
			}
			choices.replaceAutoLoad(autoLoad);
			for (JsonElement element : root.getAsJsonArray("routes")) {
				JsonObject entry = element.getAsJsonObject();
				Optional<SharedRoute> route = CollectionCodec.routeFromJson(entry);
				if (route.isPresent()) {
					ShaftLayout layout = ShaftLayout.byKey(route.get().shaft()).orElseThrow();
					library.addUser(layout, route.get().name(), route.get().author(), route.get().stops(),
							entry.get("id").getAsString());
				}
			}
			for (Map.Entry<String, JsonElement> pick : root.getAsJsonObject("picked").entrySet()) {
				choices.pick(pick.getKey(), pick.getValue().getAsString());
			}
		} catch (IOException | RuntimeException | RouteFormatException e) {
			// the bad file is kept under another name so the next save does not wipe what was in it
			FilobulusClient.LOGGER.error("Could not read {}, it is kept as routes.json.broken", file, e);
			try {
				Files.move(file, file.resolveSibling("routes.json.broken"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
				// nothing more we can do about it
			}
		}
	}

	public void save() {
		JsonArray autoLoad = new JsonArray();
		// written in screen order so the file does not shuffle between saves
		for (ShaftLayout layout : ShaftLayout.all()) {
			if (choices.autoLoad(layout.key())) {
				autoLoad.add(layout.key());
			}
		}
		JsonObject picked = new JsonObject();
		for (ShaftLayout layout : ShaftLayout.all()) {
			String id = choices.preferredRouteId(layout.key());
			if (!id.isEmpty()) {
				picked.addProperty(layout.key(), id);
			}
		}
		List<String> routes = new ArrayList<>();
		for (LibraryRoute route : library.userRoutes()) {
			JsonObject entry = CollectionCodec.toJson(shared(route));
			entry.addProperty("id", route.id());
			routes.add(entry.toString());
		}
		// one route per line, so the file can still be read and edited by hand
		String text = "{\n\"version\": 1,\n\"autoLoad\": " + autoLoad + ",\n\"picked\": " + picked
				+ ",\n\"routes\": [\n" + String.join(",\n", routes) + "\n]\n}\n";
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling("routes.json.tmp");
			Files.writeString(temp, text, StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not save {}", file, e);
		}
	}

	private static SharedRoute shared(LibraryRoute route) {
		return new SharedRoute(route.routeKey(), route.name(), route.author(), route.stops());
	}
}
