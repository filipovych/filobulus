package io.github.filipovych.filobulus.corpse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.filipovych.filobulus.corpse.CorpseLoot.Reward;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

class CorpseVerdictTest {
	// a price list out of two maps
	private static final class Prices implements PriceSource {
		private final Map<String, Double> sell = new HashMap<>();
		private final Map<String, Double> buy = new HashMap<>();

		Prices with(String id, double sellPrice, double buyPrice) {
			sell.put(id, sellPrice);
			buy.put(id, buyPrice);
			return this;
		}

		@Override
		public OptionalDouble sell(String id) {
			return sell.containsKey(id) ? OptionalDouble.of(sell.get(id)) : OptionalDouble.empty();
		}

		@Override
		public OptionalDouble buy(String id) {
			return buy.containsKey(id) ? OptionalDouble.of(buy.get(id)) : OptionalDouble.empty();
		}
	}

	// round numbers: a tungsten key is a million and a jewel is a hundredth of it, every key has its own price
	private static Prices prices() {
		return new Prices()
				.with("TUNGSTEN_KEY", 900_000, 1_000_000)
				.with("UMBER_KEY", 1_800_000, 2_000_000)
				.with("SKELETON_KEY", 39_000_000, 40_000_000)
				.with("GLACITE_JEWEL", 10_000, 12_000)
				.with("FINE_ONYX_GEM", 37_500, 38_000);
	}

	private static CorpseVerdict jewels(long amount, boolean keySaved) {
		CorpseLoot loot = new CorpseLoot(CorpseType.TUNGSTEN, List.of(
				new Reward("Glacite Jewel", amount),
				new Reward("Glacite Powder", 7412),
				new Reward("HOTM Experience", 1000)));
		return CorpseVerdict.of(loot, keySaved, prices()).orElseThrow();
	}

	@Test
	void tiersGoByWhatTheLootIsWorthAgainstTheKey() {
		assertEquals(CrackTier.DIRT, CrackTier.of(0));
		assertEquals(CrackTier.DIRT, jewels(34, false).tier());
		assertEquals(CrackTier.GLACITE, jewels(35, false).tier());
		assertEquals(CrackTier.GLACITE, jewels(69, false).tier());
		assertEquals(CrackTier.RUBY, jewels(70, false).tier());
		assertEquals(CrackTier.RUBY, jewels(99, false).tier());
		assertEquals(CrackTier.AQUAMARINE, jewels(100, false).tier());
		assertEquals(CrackTier.AQUAMARINE, jewels(249, false).tier());
		assertEquals(CrackTier.JASPER, jewels(250, false).tier());
		assertEquals(CrackTier.JASPER, CrackTier.of(170));
		assertEquals("AQUAMARINE", CrackTier.AQUAMARINE.title());
	}

	@Test
	void lootIsValuedAtSellAndTheKeyAtBuy() {
		CorpseLoot loot = new CorpseLoot(CorpseType.UMBER, List.of(
				new Reward("Umber Key", 1),
				new Reward("Fine Onyx Gemstone", 8)));
		CorpseVerdict verdict = CorpseVerdict.of(loot, false, prices()).orElseThrow();
		assertEquals(CorpseType.UMBER, verdict.type());
		assertEquals(2_100_000, verdict.value());
		assertEquals(2_000_000, verdict.keyCost());
		assertEquals(100_000, verdict.profit());
		assertEquals(1.05, verdict.roi());
		assertEquals(CrackTier.AQUAMARINE, verdict.tier());
		assertEquals("", verdict.rare());
		assertEquals(0, verdict.unknown());
		assertFalse(verdict.keySaved());
	}

	@Test
	void everyCorpseIsChargedItsOwnKey() {
		List<Reward> rewards = List.of(new Reward("Glacite Jewel", 2));
		assertEquals(1_000_000, CorpseVerdict.of(new CorpseLoot(CorpseType.TUNGSTEN, rewards), false, prices()).orElseThrow().keyCost());
		assertEquals(2_000_000, CorpseVerdict.of(new CorpseLoot(CorpseType.UMBER, rewards), false, prices()).orElseThrow().keyCost());
		assertEquals(40_000_000, CorpseVerdict.of(new CorpseLoot(CorpseType.VANGUARD, rewards), false, prices()).orElseThrow().keyCost());
		assertEquals(Optional.empty(), CorpseType.LAPIS.keyId());
	}
}
