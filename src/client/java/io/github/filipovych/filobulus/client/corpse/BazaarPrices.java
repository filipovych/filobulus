package io.github.filipovych.filobulus.client.corpse;

import com.google.gson.stream.JsonReader;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.corpse.CorpseItems;
import io.github.filipovych.filobulus.corpse.PriceSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

// bazaar prices of what a corpse can give, from hypixel's public list that needs no key
// the download runs on its own thread, the game only ever looks a price up in the map
public final class BazaarPrices implements PriceSource {
	private static final URI BAZAAR = URI.create("https://api.hypixel.net/v2/skyblock/bazaar");
	private static final Duration TIMEOUT = Duration.ofSeconds(8);
	// for the whole answer, the timeout of the request stops counting once the headers are in
	private static final long ANSWER_SECONDS = 20;
	private static final long FRESH_MILLIS = 5 * 60_000;
	private static final long RETRY_MILLIS = 60_000;

	private record Price(double buy, double sell) {
	}

	private final AtomicBoolean running = new AtomicBoolean();
	private volatile Map<String, Price> prices = Map.of();
	private volatile long nextFetch;
	private boolean failed;

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
		HttpRequest request = HttpRequest.newBuilder(BAZAAR)
				.timeout(TIMEOUT)
				.header("User-Agent", "Filobulus")
				.header("Accept-Encoding", "gzip")
				.build();
		try (HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()) {
			HttpResponse<byte[]> response;
			try {
				response = http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray()).get(ANSWER_SECONDS, TimeUnit.SECONDS);
			} catch (TimeoutException | ExecutionException e) {
				// or closing the client would wait for the dead request
				http.shutdownNow();
				throw new IOException(e instanceof TimeoutException ? "no answer in time" : String.valueOf(e.getCause()));
			}
			if (response.statusCode() != 200) {
				throw new IOException("status " + response.statusCode());
			}
			boolean gzip = response.headers().firstValue("Content-Encoding").orElse("").equalsIgnoreCase("gzip");
			InputStream body = new ByteArrayInputStream(response.body());
			try (Reader text = new InputStreamReader(gzip ? new GZIPInputStream(body) : body, StandardCharsets.UTF_8)) {
				Map<String, Price> found = parse(text);
				if (found.isEmpty()) {
					throw new IOException("no prices in the answer");
				}
				return found;
			}
		}
	}

	// the answer is a few megabytes, so it is read as a stream and all but our products are skipped
	private static Map<String, Price> parse(Reader text) throws IOException {
		Set<String> wanted = CorpseItems.pricedIds();
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
				String id = reader.nextName();
				if (wanted.contains(id)) {
					found.put(id, product(reader));
				} else {
					reader.skipValue();
				}
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
