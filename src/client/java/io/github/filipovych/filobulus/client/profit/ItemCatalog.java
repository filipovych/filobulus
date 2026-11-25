package io.github.filipovych.filobulus.client.profit;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.net.PublicApi;
import io.github.filipovych.filobulus.detect.LocationParser;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicBoolean;

// hypixel's public list of every skyblock item: what an item is called and what an npc pays for it
// it needs no key and is fetched once per game start, on its own thread
public final class ItemCatalog {
	private static final URI ITEMS = URI.create("https://api.hypixel.net/v2/resources/skyblock/items");
	private static final long RETRY_MILLIS = 5 * 60_000;

	private record Catalog(Map<String, String> idByName, Map<String, String> nameById, Map<String, Double> npc) {
	}

	private final AtomicBoolean running = new AtomicBoolean();
	private volatile Catalog catalog = new Catalog(Map.of(), Map.of(), Map.of());
	private volatile long nextFetch;
	private volatile boolean loaded;
	private boolean failed;

	// cheap enough to call every tick, it does nothing once the list is in
	public void refresh() {
		if (loaded || System.currentTimeMillis() < nextFetch || !running.compareAndSet(false, true)) {
			return;
		}
		nextFetch = System.currentTimeMillis() + RETRY_MILLIS;
		Thread thread = new Thread(this::fetch, "Filobulus items");
		thread.setDaemon(true);
		thread.start();
	}

	// the id of an item by the name the game shows for it
	public Optional<String> id(String name) {
		return Optional.ofNullable(catalog.idByName().get(plain(name)));
	}

	public Optional<String> name(String id) {
		return Optional.ofNullable(catalog.nameById().get(id));
	}

	public OptionalDouble npc(String id) {
		Double price = catalog.npc().get(id);
		return price == null || price <= 0 ? OptionalDouble.empty() : OptionalDouble.of(price);
	}

	private void fetch() {
		try {
			Catalog found = PublicApi.get(ITEMS, ItemCatalog::parse);
			if (found.nameById().isEmpty()) {
				throw new IOException("no items in the answer");
			}
			catalog = found;
			loaded = true;
		} catch (IOException | InterruptedException | RuntimeException e) {
			if (!failed) {
				failed = true;
				FilobulusClient.LOGGER.warn("Could not load the item list: {}", e.toString());
			}
		} finally {
			running.set(false);
		}
	}

	// a few megabytes, read as a stream, three fields of every item are kept
	private static Catalog parse(Reader text) throws IOException {
		Map<String, String> idByName = new HashMap<>();
		Map<String, String> nameById = new HashMap<>();
		Map<String, Double> npc = new HashMap<>();
		JsonReader reader = new JsonReader(text);
		reader.beginObject();
		while (reader.hasNext()) {
			if (!reader.nextName().equals("items")) {
				reader.skipValue();
				continue;
			}
			reader.beginArray();
			while (reader.hasNext()) {
				String id = "";
				String name = "";
				double price = 0;
				reader.beginObject();
				while (reader.hasNext()) {
					String field = reader.nextName();
					if (field.equals("id") && reader.peek() == JsonToken.STRING) {
						id = reader.nextString();
					} else if (field.equals("name") && reader.peek() == JsonToken.STRING) {
						name = LocationParser.stripColor(reader.nextString()).strip();
					} else if (field.equals("npc_sell_price") && reader.peek() == JsonToken.NUMBER) {
						price = reader.nextDouble();
					} else {
						reader.skipValue();
					}
				}
				reader.endObject();
				if (id.isEmpty() || name.isEmpty()) {
					continue;
				}
				nameById.put(id, name);
				// two items can share a name, the first one stays
				idByName.putIfAbsent(plain(name), id);
				if (price > 0) {
					npc.put(id, price);
				}
			}
			reader.endArray();
		}
		reader.endObject();
		return new Catalog(Map.copyOf(idByName), Map.copyOf(nameById), Map.copyOf(npc));
	}

	// lower case and without the small icon some names have in front
	private static String plain(String name) {
		return name == null ? "" : name.replaceFirst("^[^A-Za-z0-9]+", "").strip().toLowerCase(Locale.ROOT);
	}
}
