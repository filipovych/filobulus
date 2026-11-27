package io.github.filipovych.filobulus.route;

import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// what the player set on the routes screen: which shafts load a route by themselves and which route each one uses
// keys are route keys, so a type code or CRYSTAL
public final class RouteChoices implements RouteSettings {
	public static final List<String> DEFAULT_AUTO_LOAD = List.of("JASP_1", ShaftType.CRYSTAL_ROUTE_KEY);

	private final Set<String> autoLoad = new LinkedHashSet<>(DEFAULT_AUTO_LOAD);
	private final Map<String, String> picked = new LinkedHashMap<>();

	@Override
	public boolean autoLoad(ShaftType type) {
		return autoLoad(type.routeKey());
	}

	public boolean autoLoad(String routeKey) {
		return autoLoad.contains(routeKey);
	}

	public void setAutoLoad(String routeKey, boolean on) {
		if (on) {
			autoLoad.add(routeKey);
		} else {
			autoLoad.remove(routeKey);
		}
	}

	public Set<String> autoLoadKeys() {
		return Set.copyOf(autoLoad);
	}

	public void replaceAutoLoad(Iterable<String> routeKeys) {
		autoLoad.clear();
		routeKeys.forEach(autoLoad::add);
	}

	@Override
	public String preferredRouteId(String routeKey) {
		return picked.getOrDefault(routeKey, "");
	}

	public void pick(String routeKey, String routeId) {
		picked.put(routeKey, routeId);
	}

	public Map<String, String> picks() {
		return Map.copyOf(picked);
	}

	// for a new profile that starts out like this one
	public RouteChoices copy() {
		RouteChoices copy = new RouteChoices();
		copy.replaceAutoLoad(autoLoad);
		copy.picked.putAll(picked);
		return copy;
	}
}
