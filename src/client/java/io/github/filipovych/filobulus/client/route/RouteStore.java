package io.github.filipovych.filobulus.client.route;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.SafeFile;
import io.github.filipovych.filobulus.client.compat.GameCompat;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;

// the routes the player imported and what they picked on the routes screen, kept in config/filobulus/routes.json
// the routes that ship with the mod are put in on every load, the file only keeps what the player set on them
public final class RouteStore {
	private final Path file;
	private final RouteLibrary library;
	private final List<CustomRoute> bundled;
	private final RouteProfiles profiles = new RouteProfiles();
	private final CustomRoutes custom = new CustomRoutes();
	private boolean readOnly;
	private boolean toldReadOnly;

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

	public RouteStore(Path file, RouteLibrary library, List<CustomRoute> bundled) {
		this.file = file;
		this.library = library;
		this.bundled = List.copyOf(bundled);
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
		if (Files.exists(file)) {
			read();
		}
		bundled.forEach(custom::bundle);
	}

	// the whole file is read before anything goes into the live lists, so a bad file leaves them empty
	// and is kept under another name, one entry that can not be read is skipped and the rest stays
	private void read() {
		List<Runnable> apply = new ArrayList<>();
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			for (JsonElement element : array(root, "routes")) {
				try {
					JsonObject entry = element.getAsJsonObject();
					Optional<SharedRoute> route = CollectionCodec.routeFromJson(entry);
					if (route.isPresent()) {
						ShaftLayout layout = ShaftLayout.byKey(route.get().shaft()).orElseThrow();
						String id = text(entry, "id");
						apply.add(() -> library.addUser(layout, route.get().name(), route.get().author(),
								route.get().stops(), id.isEmpty() ? null : id));
					}
				} catch (RuntimeException | RouteFormatException e) {
					FilobulusClient.LOGGER.warn("Skipped a route in {} that could not be read", file, e);
				}
			}
			Map<String, RouteChoices> saved = new LinkedHashMap<>();
			if (root.has("profiles")) {
				for (JsonElement element : array(root, "profiles")) {
					JsonObject profile = element.getAsJsonObject();
					saved.put(text(profile, "name"), choices(profile));
				}
			} else {
				// a file from before there were profiles
				saved.put(RouteProfiles.FIRST, choices(root));
			}
			String active = root.has("profile") ? text(root, "profile") : RouteProfiles.FIRST;
			apply.add(() -> profiles.restore(saved, active));
			for (JsonElement element : array(root, "custom")) {
				try {
					readCustom(element.getAsJsonObject()).ifPresent(apply::add);
				} catch (RuntimeException | RouteFormatException e) {
					FilobulusClient.LOGGER.warn("Skipped a route in {} that could not be read", file, e);
				}
			}
		} catch (IOException | RuntimeException e) {
			FilobulusClient.LOGGER.error("Could not read {}, a copy is kept next to it", file, e);
			// when it can not be moved away the next save would write over it, so saving stays off
			readOnly = !SafeFile.moveBroken(file);
			return;
		}
		apply.forEach(Runnable::run);
	}

	// true when routes.json could not be read or moved away, changes are not saved so it is not written over
	public boolean readOnly() {
		return readOnly;
	}

	private void tellReadOnly() {
		Minecraft minecraft = Minecraft.getInstance();
		if (toldReadOnly || minecraft.player == null) {
			return;
		}
		toldReadOnly = true;
		GameCompat.say(minecraft, FilobulusClient.chatLine(
				"Route changes can not be saved this session, routes.json could not be read and is left as it was."));
	}

	private static JsonArray array(JsonObject json, String key) {
		return json.has(key) && json.get(key).isJsonArray() ? json.getAsJsonArray(key) : new JsonArray();
	}

	private static RouteChoices choices(JsonObject json) {
		RouteChoices choices = new RouteChoices();
		List<String> autoLoad = new ArrayList<>();
		for (JsonElement key : array(json, "autoLoad")) {
			if (key.isJsonPrimitive()) {
				autoLoad.add(key.getAsString());
			}
		}
		choices.replaceAutoLoad(autoLoad);
		if (json.has("picked") && json.get("picked").isJsonObject()) {
			for (Map.Entry<String, JsonElement> pick : json.getAsJsonObject("picked").entrySet()) {
				if (pick.getValue().isJsonPrimitive()) {
					choices.pick(pick.getKey(), pick.getValue().getAsString());
				}
			}
		}
		return choices;
	}

	private Optional<Runnable> readCustom(JsonObject entry) throws RouteFormatException {
		List<Waypoint> stops = CollectionCodec.stopsFromJson(entry.get("stops"));
		if (stops.isEmpty()) {
			return Optional.empty();
		}
		boolean loop = flag(entry, "loop").orElse(true);
		boolean on = flag(entry, "on").orElse(false);
		return Optional.of(() -> {
			CustomRoute route = custom.add(text(entry, "name"), text(entry, "author"), text(entry, "icon"),
					text(entry, "area"), loop, stops, text(entry, "id"));
			if (on) {
				custom.use(route.id(), true);
			}
		});
	}

	private static String text(JsonObject entry, String key) {
		return entry.has(key) && entry.get(key).isJsonPrimitive() ? entry.get(key).getAsString() : "";
	}

	private static Optional<Boolean> flag(JsonObject entry, String key) {
		return entry.has(key) && entry.get(key).isJsonPrimitive() && entry.get(key).getAsJsonPrimitive().isBoolean()
				? Optional.of(entry.get(key).getAsBoolean())
				: Optional.empty();
	}

	public void save() {
		if (readOnly) {
			tellReadOnly();
			return;
		}
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
			SafeFile.write(file, text);
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

	// the route goes from every profile, the ones that used it fall back to the first one left
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
