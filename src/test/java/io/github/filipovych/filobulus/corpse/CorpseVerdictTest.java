package io.github.filipovych.filobulus.corpse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void aSavedKeyCountsAsLoot() {
		CorpseVerdict verdict = jewels(40, true);
		assertTrue(verdict.keySaved());
		assertEquals(400_000, verdict.value());
		assertEquals(400_000, verdict.profit());
		assertEquals(1.4, verdict.roi());
		assertEquals(CrackTier.AQUAMARINE, verdict.tier());
		assertEquals(CrackTier.GLACITE, jewels(40, false).tier());
		assertEquals(-600_000, jewels(40, false).profit());
	}

	@Test
	void aRareRewardIsAlwaysTheTopTier() {
		CorpseLoot loot = new CorpseLoot(CorpseType.VANGUARD, List.of(
				new Reward("Glacite Jewel", 2),
				new Reward("Shattered Locket", 1)));
		CorpseVerdict verdict = CorpseVerdict.of(loot, false, prices()).orElseThrow();
		assertEquals(CrackTier.JASPER, verdict.tier());
		assertEquals("Shattered Locket", verdict.rare());
		assertEquals(20_000, verdict.value());
		assertEquals(0, verdict.unknown());
		assertEquals("Shattered Locket", verdict.summary());
		assertEquals("Shattered Locket  key saved", CorpseVerdict.of(loot, true, prices()).orElseThrow().summary());
	}

	@Test
	void noVerdictForLapisOrWithoutAKeyPrice() {
		List<Reward> rewards = List.of(new Reward("Glacite Jewel", 2));
		assertEquals(Optional.empty(), CorpseVerdict.of(new CorpseLoot(CorpseType.LAPIS, rewards), false, prices()));
		Prices noKey = new Prices().with("GLACITE_JEWEL", 10_000, 12_000);
		assertEquals(Optional.empty(), CorpseVerdict.of(new CorpseLoot(CorpseType.TUNGSTEN, rewards), false, noKey));
		assertEquals(Optional.empty(), CorpseVerdict.of(new CorpseLoot(CorpseType.TUNGSTEN, rewards), false, noKey.with("TUNGSTEN_KEY", 0, 0)));
		assertTrue(CorpseVerdict.of(new CorpseLoot(CorpseType.VANGUARD, rewards), false, prices()).isPresent());
	}

	@Test
	void countsWhatItCouldNotValue() {
		CorpseLoot loot = new CorpseLoot(CorpseType.TUNGSTEN, List.of(
				new Reward("Glacite Jewel", 34),
				new Reward("Mystery Thing", 3),
				new Reward("Umber Plate", 1),
				new Reward("Onyx Crystal", 1),
				new Reward("Dead Man's Chestplate", 1)));
		CorpseVerdict verdict = CorpseVerdict.of(loot, false, prices()).orElseThrow();
		assertEquals(2, verdict.unknown());
		assertEquals(340_000, verdict.value());
		assertEquals(CrackTier.DIRT, verdict.tier());
		assertEquals("-660k?  0.34x key", verdict.summary());
	}

	@Test
	void summaryIsTheProfitAndHowManyKeysItWas() {
		assertEquals("+400k  1.4x key", jewels(140, false).summary());
		assertEquals("-550k  0.45x key", jewels(45, false).summary());
		assertEquals("+0  1.0x key", jewels(100, false).summary());
		assertEquals("+11.0M  12x key", jewels(1200, false).summary());
		assertEquals("+400k  1.4x key  key saved", jewels(40, true).summary());
		assertEquals("-1.0M  0.00x key", jewels(0, false).summary());
	}

	@Test
	void coinsAreWrittenShort() {
		assertEquals("0", CoinText.amount(0));
		assertEquals("850", CoinText.amount(850));
		assertEquals("999", CoinText.amount(999.4));
		assertEquals("1.0k", CoinText.amount(999.5));
		assertEquals("1.5k", CoinText.amount(1_500));
		assertEquals("9.9k", CoinText.amount(9_949));
		assertEquals("10k", CoinText.amount(9_960));
		assertEquals("12k", CoinText.amount(12_400));
		assertEquals("850k", CoinText.amount(850_000));
		assertEquals("1.0M", CoinText.amount(999_960));
		assertEquals("1.2M", CoinText.amount(1_200_000));
		assertEquals("12.4M", CoinText.amount(12_400_000));
		assertEquals("99.9M", CoinText.amount(99_949_999));
		assertEquals("100M", CoinText.amount(99_960_000));
		assertEquals("680M", CoinText.amount(680_000_000));
		assertEquals("1.25B", CoinText.amount(1_250_000_000));
		assertEquals("1.8M", CoinText.amount(-1_800_000));
		assertEquals("-1.8M", CoinText.signed(-1_800_000));
		assertEquals("+1.2M", CoinText.signed(1_200_000));
		assertEquals("+0", CoinText.signed(-0.2));
	}
}
