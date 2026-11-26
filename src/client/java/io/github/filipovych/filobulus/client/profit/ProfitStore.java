package io.github.filipovych.filobulus.client.profit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.profit.ProfitTotals;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

// the all time totals of the profit tracker and the names of the items in them, config/filobulus/profit.json
public final class ProfitStore {
	private final Path file;

	public ProfitStore(Path file) {
		this.file = file;
	}

	// the all time total of one skyblock profile has a file of its own
	public ProfitStore forProfile(String id) {
		return new ProfitStore(file.resolveSibling("profit-" + id + ".json"));
	}

	public boolean exists() {
		return Files.exists(file);
	}

	// names gets the saved item names put into it, a file that can not be read gives empty totals
	public ProfitTotals load(Map<String, String> names) {
		if (!Files.exists(file)) {
			return new ProfitTotals();
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			Map<String, Long> items = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> item : root.getAsJsonObject("items").entrySet()) {
				items.put(item.getKey(), item.getValue().getAsLong());
			}
			if (root.has("names")) {
				for (Map.Entry<String, JsonElement> name : root.getAsJsonObject("names").entrySet()) {
					names.put(name.getKey(), name.getValue().getAsString());
				}
			}
			return new ProfitTotals(items, root.has("active") ? root.get("active").getAsLong() : 0,
					root.has("corpses") ? root.get("corpses").getAsInt() : 0,
					root.has("corpseCoins") ? root.get("corpseCoins").getAsDouble() : 0);
		} catch (IOException | RuntimeException e) {
			FilobulusClient.LOGGER.error("Could not read {}, it is kept as profit.json.broken", file, e);
			try {
				Files.move(file, file.resolveSibling("profit.json.broken"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
				// nothing more we can do about it
			}
			return new ProfitTotals();
		}
	}

	public void save(ProfitTotals totals, Map<String, String> names, long activeMillis) {
		JsonObject items = new JsonObject();
		JsonObject kept = new JsonObject();
		totals.items().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(item -> {
			items.addProperty(item.getKey(), item.getValue());
			String name = names.get(item.getKey());
			if (name != null) {
				kept.addProperty(item.getKey(), name);
			}
		});
		JsonObject root = new JsonObject();
		root.addProperty("version", 1);
		root.addProperty("active", activeMillis);
		root.addProperty("corpses", totals.corpses());
		root.addProperty("corpseCoins", totals.corpseCoins());
		root.add("items", items);
		root.add("names", kept);
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling("profit.json.tmp");
			Files.writeString(temp, root + "\n", StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not save {}", file, e);
		}
	}
}
