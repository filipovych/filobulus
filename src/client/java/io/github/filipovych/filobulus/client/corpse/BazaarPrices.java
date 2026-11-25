package io.github.filipovych.filobulus.client.corpse;

import com.google.gson.stream.JsonReader;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.net.PublicApi;
import io.github.filipovych.filobulus.corpse.PriceSource;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicBoolean;

// bazaar prices, from hypixel's public list that needs no key
// the download runs on its own thread, the game only ever looks a price up in the map
public final class BazaarPrices implements PriceSource {
	private static final URI BAZAAR = URI.create("https://api.hypixel.net/v2/skyblock/bazaar");
	private static final long FRESH_MILLIS = 5 * 60_000;
	private static final long RETRY_MILLIS = 60_000;

	private record Price(double buy, double sell) {
	}

	private final AtomicBoolean running = new AtomicBoolean();
	private volatile Map<String, Price> prices = Map.of();
	private volatile long nextFetch;
	private boolean failed;

	// false until the first download came in
	public boolean loaded() {
		return !prices.isEmpty();
	}

	// cheap enough to call every tick, it only starts a download when the prices are old
	public void refresh() {
		if (System.currentTimeMillis() < nextFetch || !running.compareAndSet(false, true)) {
			return;
		}
		// a fetch that ends well moves this further on
		nextFetch = System.currentTimeMillis() + RETRY_MILLIS;
		Thread thread = new Thread(this::fetch, "Filobulus bazaar");
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public OptionalDouble sell(String id) {
		Price price = prices.get(id);
		return price == null || price.sell() <= 0 ? OptionalDouble.empty() : OptionalDouble.of(price.sell());
	}

	@Override
	public OptionalDouble buy(String id) {
		Price price = prices.get(id);
		return price == null || price.buy() <= 0 ? OptionalDouble.empty() : OptionalDouble.of(price.buy());
	}

	private void fetch() {
		try {
			prices = download();
			nextFetch = System.currentTimeMillis() + FRESH_MILLIS;
			failed = false;
		} catch (IOException | InterruptedException | RuntimeException e) {
			nextFetch = System.currentTimeMillis() + RETRY_MILLIS;
			// the old prices stay, and one line in the log is enough while it keeps failing
			if (!failed) {
				failed = true;
				FilobulusClient.LOGGER.warn("Could not load the bazaar prices: {}", e.toString());
			}
		} finally {
			running.set(false);
		}
	}

	private static Map<String, Price> download() throws IOException, InterruptedException {
		Map<String, Price> found = PublicApi.get(BAZAAR, BazaarPrices::parse);
		if (found.isEmpty()) {
			throw new IOException("no prices in the answer");
		}
		return found;
	}

	// the answer is a few megabytes, so it is read as a stream and only the two prices of each product are kept
	private static Map<String, Price> parse(Reader text) throws IOException {
		Map<String, Price> found = new HashMap<>();
		JsonReader reader = new JsonReader(text);
		reader.beginObject();
		while (reader.hasNext()) {
			if (!reader.nextName().equals("products")) {
				reader.skipValue();
				continue;
			}
			reader.beginObject();
			while (reader.hasNext()) {
				found.put(reader.nextName(), product(reader));
			}
			reader.endObject();
		}
		reader.endObject();
		return Map.copyOf(found);
	}

	// buyPrice is what buying at once costs, sellPrice what selling at once gives
	private static Price product(JsonReader reader) throws IOException {
		double buy = 0;
		double sell = 0;
		reader.beginObject();
		while (reader.hasNext()) {
			if (!reader.nextName().equals("quick_status")) {
				reader.skipValue();
				continue;
			}
			reader.beginObject();
			while (reader.hasNext()) {
				switch (reader.nextName()) {
					case "buyPrice" -> buy = reader.nextDouble();
					case "sellPrice" -> sell = reader.nextDouble();
					default -> reader.skipValue();
				}
			}
			reader.endObject();
		}
		reader.endObject();
		return new Price(buy, sell);
	}
}
