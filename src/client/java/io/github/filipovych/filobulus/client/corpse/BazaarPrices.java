package io.github.filipovych.filobulus.client.corpse;

import com.google.gson.stream.JsonReader;
import io.github.filipovych.filobulus.corpse.CorpseItems;
import io.github.filipovych.filobulus.corpse.PriceSource;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;

// bazaar prices of what a corpse can give, from hypixel's public list that needs no key
public final class BazaarPrices implements PriceSource {
	private record Price(double buy, double sell) {
	}

	private volatile Map<String, Price> prices = Map.of();

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
