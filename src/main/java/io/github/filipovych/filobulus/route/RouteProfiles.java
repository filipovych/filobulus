package io.github.filipovych.filobulus.route;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// named sets of what the routes screen holds: the auto-load switches and the route picked for each shaft
// one of them is in use, the imported routes themselves are shared by all of them
public final class RouteProfiles {
	public static final String FIRST = "Main";
	public static final int MAX_NAME = 20;

	private final Map<String, RouteChoices> profiles = new LinkedHashMap<>();
	private String active = FIRST;

	public RouteProfiles() {
		profiles.put(FIRST, new RouteChoices());
	}

	public List<String> names() {
		return List.copyOf(profiles.keySet());
	}

	public String active() {
		return active;
	}

	// what the profile in use holds
	public RouteChoices choices() {
		return profiles.get(active);
	}

	public Optional<RouteChoices> choices(String name) {
		return Optional.ofNullable(profiles.get(name));
	}

	public boolean select(String name) {
		if (!profiles.containsKey(name)) {
			return false;
		}
		active = name;
		return true;
	}

	// a new profile starts as a copy of the one in use and becomes the one in use
	// empty when the name is blank or taken
	public Optional<String> add(String wanted) {
		String name = clean(wanted);
		if (name.isEmpty() || taken(name)) {
			return Optional.empty();
		}
		profiles.put(name, choices().copy());
		active = name;
		return Optional.of(name);
	}

	// the last profile can not go, the first one takes over when the one in use is removed
	public boolean remove(String name) {
		if (profiles.size() < 2 || profiles.remove(name) == null) {
			return false;
		}
		if (active.equals(name)) {
			active = profiles.keySet().iterator().next();
		}
		return true;
	}

	// a route is gone, every profile that used it goes back to the default of that shaft
	public void unpick(String routeKey, String routeId) {
		for (RouteChoices choices : profiles.values()) {
			if (choices.preferredRouteId(routeKey).equals(routeId)) {
				choices.pick(routeKey, "");
			}
		}
	}

	// for reading the saved file: drops what is there, a file without profiles keeps the first one
	public void restore(Map<String, RouteChoices> saved, String wantedActive) {
		if (saved.isEmpty()) {
			return;
		}
		profiles.clear();
		saved.forEach((name, choices) -> profiles.putIfAbsent(clean(name).isEmpty() ? FIRST : clean(name), choices));
		active = profiles.containsKey(wantedActive) ? wantedActive : profiles.keySet().iterator().next();
	}

	private boolean taken(String name) {
		return profiles.keySet().stream().anyMatch(name::equalsIgnoreCase);
	}

	private static String clean(String name) {
		String text = name == null ? "" : name.strip().replaceAll("\\s+", " ");
		return text.length() > MAX_NAME ? text.substring(0, MAX_NAME).strip() : text;
	}
}
