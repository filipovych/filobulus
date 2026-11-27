package io.github.filipovych.filobulus.client.route;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.route.CollectionCodec;
import io.github.filipovych.filobulus.route.CustomRoute;
import io.github.filipovych.filobulus.route.CustomRoutes;
import io.github.filipovych.filobulus.route.LibraryRoute;
import io.github.filipovych.filobulus.route.RouteChoices;
import io.github.filipovych.filobulus.route.RouteCollection;
import io.github.filipovych.filobulus.route.RouteFormatException;
import io.github.filipovych.filobulus.route.RouteLibrary;
import io.github.filipovych.filobulus.route.RouteProfiles;
import io.github.filipovych.filobulus.route.RouteSettings;
import io.github.filipovych.filobulus.route.SharedRoute;
import io.github.filipovych.filobulus.route.Waypoint;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// the routes the player imported and what they picked on the routes screen, kept in config/filobulus/routes.json
public final class RouteStore {
	private final Path file;
	private final RouteLibrary library;
	private final RouteProfiles profiles = new RouteProfiles();
	private final CustomRoutes custom = new CustomRoutes();

	// what the selector asks, always answered by the profile in use
	private final RouteSettings settings = new RouteSettings() {
		@Override
		public boolean autoLoad(ShaftType type) {
			return choices().autoLoad(type);
		}

		@Override
		public String preferredRouteId(String routeKey) {
			return choices().preferredRouteId(routeKey);
		}

		@Override
		public Optional<LibraryRoute> customRoute(List<String> places) {
			return custom.activeIn(places).map(CustomRoute::route);
		}
	};

	public RouteStore(Path file, RouteLibrary library) {
		this.file = file;
		this.library = library;
	}

	public RouteLibrary library() {
		return library;
	}

	public RouteProfiles profiles() {
		return profiles;
	}

	// the switches and picks of the profile in use
	public RouteChoices choices() {
		return profiles.choices();
	}

	public CustomRoutes custom() {
		return custom;
	}

	public RouteSettings settings() {
		return settings;
	}

	public void load() {
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			for (JsonElement element : root.getAsJsonArray("routes")) {
				JsonObject entry = element.getAsJsonObject();
				Optional<SharedRoute> route = CollectionCodec.routeFromJson(entry);
				if (route.isPresent()) {
					ShaftLayout layout = ShaftLayout.byKey(route.get().shaft()).orElseThrow();
					library.addUser(layout, route.get().name(), route.get().author(), route.get().stops(),
							entry.get("id").getAsString());
				}
			}
			Map<String, RouteChoices> saved = new LinkedHashMap<>();
			if (root.has("profiles")) {
				for (JsonElement element : root.getAsJsonArray("profiles")) {
					JsonObject profile = element.getAsJsonObject();
					saved.put(profile.get("name").getAsString(), choices(profile));
				}
			} else {
				// a file from before there were profiles
				saved.put(RouteProfiles.FIRST, choices(root));
			}
			profiles.restore(saved, root.has("profile") ? root.get("profile").getAsString() : RouteProfiles.FIRST);
			if (root.has("custom")) {
				for (JsonElement element : root.getAsJsonArray("custom")) {
					readCustom(element.getAsJsonObject());
				}
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

	private static RouteChoices choices(JsonObject json) {
		RouteChoices choices = new RouteChoices();
		List<String> autoLoad = new ArrayList<>();
		for (JsonElement key : json.getAsJsonArray("autoLoad")) {
			autoLoad.add(key.getAsString());
		}
		choices.replaceAutoLoad(autoLoad);
		for (Map.Entry<String, JsonElement> pick : json.getAsJsonObject("picked").entrySet()) {
			choices.pick(pick.getKey(), pick.getValue().getAsString());
		}
		return choices;
	}

	private void readCustom(JsonObject entry) throws RouteFormatException {
		List<Waypoint> stops = CollectionCodec.stopsFromJson(entry.get("stops"));
		if (stops.isEmpty()) {
			return;
		}
		CustomRoute route = custom.add(text(entry, "name"), text(entry, "author"), text(entry, "icon"),
				text(entry, "area"), !entry.has("loop") || entry.get("loop").getAsBoolean(), stops, text(entry, "id"));
		if (entry.has("on") && entry.get("on").getAsBoolean()) {
			custom.use(route.id(), true);
		}
	}

	private static String text(JsonObject entry, String key) {
		return entry.has(key) && entry.get(key).isJsonPrimitive() ? entry.get(key).getAsString() : "";
	}

	public void save() {
		List<String> saved = new ArrayList<>();
		for (String name : profiles.names()) {
			RouteChoices choices = profiles.choices(name).orElseThrow();
			JsonArray autoLoad = new JsonArray();
			JsonObject picked = new JsonObject();
			// written in screen order so the file does not shuffle between saves
			for (ShaftLayout layout : ShaftLayout.all()) {
				if (choices.autoLoad(layout.key())) {
					autoLoad.add(layout.key());
				}
				String id = choices.preferredRouteId(layout.key());
				if (!id.isEmpty()) {
					picked.addProperty(layout.key(), id);
				}
			}
			JsonObject profile = new JsonObject();
			profile.addProperty("name", name);
			profile.add("autoLoad", autoLoad);
			profile.add("picked", picked);
			saved.add(profile.toString());
		}
		List<String> routes = new ArrayList<>();
		for (LibraryRoute route : library.userRoutes()) {
			JsonObject entry = CollectionCodec.toJson(shared(route));
			entry.addProperty("id", route.id());
			routes.add(entry.toString());
		}
		List<String> own = new ArrayList<>();
		for (CustomRoute route : custom.all()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("id", route.id());
			entry.addProperty("name", route.name());
			entry.addProperty("author", route.route().author());
			entry.addProperty("icon", route.icon());
			entry.addProperty("area", route.area());
			entry.addProperty("loop", route.loop());
			entry.addProperty("on", route.on());
			entry.add("stops", CollectionCodec.stopsToJson(route.stops()));
			own.add(entry.toString());
		}
		JsonObject active = new JsonObject();
		active.addProperty("profile", profiles.active());
		// one profile and one route per line, so the file can still be read and edited by hand
		String text = "{\n\"version\": 2,\n\"profile\": " + active.get("profile")
				+ ",\n\"profiles\": [\n" + String.join(",\n", saved)
				+ "\n],\n\"routes\": [\n" + String.join(",\n", routes)
				+ "\n],\n\"custom\": [\n" + String.join(",\n", own) + "\n]\n}\n";
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling("routes.json.tmp");
			Files.writeString(temp, text, StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not save {}", file, e);
		}
	}

	// adds the route, or finds it when the same stops are already there, and makes it the one its shaft uses
	public LibraryRoute add(SharedRoute route) {
		ShaftLayout layout = ShaftLayout.byKey(route.shaft()).orElseThrow();
		LibraryRoute added = library.addUser(layout, route.name(), route.author(), route.stops(), null);
		choices().pick(layout.key(), added.id());
		return added;
	}

	// the route goes from every profile, the ones that used it fall back to the default
	public void delete(LibraryRoute route) {
		if (library.removeUser(route.id())) {
			profiles.unpick(route.routeKey(), route.id());
		}
	}

	// the route each shaft uses, wherever that is not the bundled default
	public RouteCollection picked(String name) {
		List<SharedRoute> routes = new ArrayList<>();
		for (ShaftLayout layout : ShaftLayout.all()) {
			library.routeFor(layout.key(), choices().preferredRouteId(layout.key()))
					.filter(route -> !route.bundled() || !route.name().equals(LibraryRoute.DEFAULT_NAME))
					.ifPresent(route -> routes.add(shared(route)));
		}
		return new RouteCollection(name, routes);
	}

	private static SharedRoute shared(LibraryRoute route) {
		return new SharedRoute(route.routeKey(), route.name(), route.author(), route.stops());
	}
}
