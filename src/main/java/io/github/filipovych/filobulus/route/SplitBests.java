package io.github.filipovych.filobulus.route;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

// the fastest finished run of every route, with the time at each stop
// version 2 times a run from the shaft entrance, the first version from the first stop, those bests are not comparable and are dropped
public final class SplitBests {
	private static final int VERSION = 2;
	private final Map<String, long[]> bests = new TreeMap<>();

	// empty when the route has other stops now than when the best was set
	public Optional<long[]> best(String routeId, int stops) {
		long[] best = bests.get(routeId);
		return best == null || best.length != stops ? Optional.empty() : Optional.of(best.clone());
	}

	// keeps the run when it is the first one or faster than the best, says whether it was kept
	public boolean offer(String routeId, long[] times) {
		if (times.length == 0) {
			return false;
		}
		long[] best = bests.get(routeId);
		if (best != null && best.length == times.length && best[best.length - 1] <= times[times.length - 1]) {
			return false;
		}
		bests.put(routeId, times.clone());
		return true;
	}

	public void forget(String routeId) {
		bests.remove(routeId);
	}

	public JsonObject toJson() {
		JsonObject runs = new JsonObject();
		for (Map.Entry<String, long[]> entry : bests.entrySet()) {
			JsonArray times = new JsonArray();
			for (long time : entry.getValue()) {
				times.add(time);
			}
			runs.add(entry.getKey(), times);
		}
		JsonObject root = new JsonObject();
		root.addProperty("version", VERSION);
		root.add("bests", runs);
		return root;
	}

	// throws a runtime exception on anything that is not what toJson writes
	public void readJson(JsonObject root) {
		bests.clear();
		if (!root.has("version") || root.get("version").getAsInt() < VERSION) {
			return;
		}
		for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("bests").entrySet()) {
			JsonArray array = entry.getValue().getAsJsonArray();
			long[] times = new long[array.size()];
			for (int i = 0; i < times.length; i++) {
				times[i] = array.get(i).getAsLong();
			}
			if (times.length > 0) {
				bests.put(entry.getKey(), times);
			}
		}
	}
}
