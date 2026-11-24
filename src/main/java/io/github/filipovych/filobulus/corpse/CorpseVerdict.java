package io.github.filipovych.filobulus.corpse;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;

// what a corpse was worth against the key it cost
// rare is the name of an auction only reward or "", unknown counts the rewards we could not value
public record CorpseVerdict(CorpseType type, CrackTier tier, double value, double keyCost, boolean keySaved, String rare, int unknown) {
	// empty for lapis, which costs no key, and while the key has no price
	// loot is valued at what it sells for at once, the key at what buying the next one costs
	public static Optional<CorpseVerdict> of(CorpseLoot loot, boolean keySaved, PriceSource prices) {
		Optional<String> key = loot.type().keyId();
		if (key.isEmpty()) {
			return Optional.empty();
		}
		OptionalDouble keyPrice = prices.buy(key.get());
		if (keyPrice.isEmpty() || !(keyPrice.getAsDouble() > 0)) {
			return Optional.empty();
		}
		double keyCost = keyPrice.getAsDouble();
		double value = 0;
		String rare = "";
		int unknown = 0;
		for (CorpseLoot.Reward reward : loot.rewards()) {
			if (CorpseItems.rare(reward.name())) {
				if (rare.isEmpty()) {
					rare = reward.name();
				}
				continue;
			}
			if (CorpseItems.worthless(reward.name())) {
				continue;
			}
			OptionalDouble price = CorpseItems.id(reward.name()).map(prices::sell).orElseGet(OptionalDouble::empty);
			if (price.isPresent()) {
				value += price.getAsDouble() * reward.amount();
			} else {
				unknown++;
			}
		}
		CrackTier tier = rare.isEmpty() ? CrackTier.of(roi(value, keyCost, keySaved)) : CrackTier.JASPER;
		return Optional.of(new CorpseVerdict(loot.type(), tier, value, keyCost, keySaved, rare, unknown));
	}

	// a key that was not used up is one we do not have to buy
	public double profit() {
		return keySaved ? value : value - keyCost;
	}

	// a saved key counts as loot here
	public double roi() {
		return roi(value, keyCost, keySaved);
	}

	// the line under the tier: +1.2M  1.4x key, with a ? behind the coins when a reward had no price
	public String summary() {
		String text = rare;
		if (rare.isEmpty()) {
			text = CoinText.signed(profit()) + (unknown > 0 ? "?" : "") + "  " + times(roi()) + " key";
		}
		return keySaved ? text + "  key saved" : text;
	}

	private static double roi(double value, double keyCost, boolean keySaved) {
		return (keySaved ? value + keyCost : value) / keyCost;
	}

	// 0.45x, 1.4x, 12x
	private static String times(double roi) {
		if (roi < 0.995) {
			return String.format(Locale.ROOT, "%.2fx", roi);
		}
		return roi < 9.95 ? String.format(Locale.ROOT, "%.1fx", roi) : Math.round(roi) + "x";
	}
}
