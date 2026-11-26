package io.github.filipovych.filobulus.profit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class ProfitValueTest {
	private static final Function<String, OptionalDouble> PRICES = id -> switch (id) {
		case "FLAWED_JASPER_GEM" -> OptionalDouble.of(4000);
		case "GLACITE" -> OptionalDouble.of(70);
		default -> OptionalDouble.empty();
	};

	@Test
	void theMostValuableItemComesFirstAndUnpricedOnesCountAsNothing() {
		ProfitValue value = ProfitValue.of(Map.of("GLACITE", 1000L, "FLAWED_JASPER_GEM", 100L, "MYSTERY", 5L), PRICES);
		assertEquals(List.of("FLAWED_JASPER_GEM", "GLACITE", "MYSTERY"), value.lines().stream().map(ProfitValue.Line::id).toList());
		assertEquals(470_000, value.coins(), 0.001);
		assertEquals(1, value.unpriced());
		assertFalse(value.lines().get(2).priced());
	}

	@Test
	void coinsPerHourNeedSomeTimeToGoBy() {
		ProfitValue value = ProfitValue.of(Map.of("FLAWED_JASPER_GEM", 100L), PRICES);
		assertEquals(0, value.perHour(59_000));
		assertEquals(2_400_000, value.perHour(600_000), 0.001);
	}

	@Test
	void whatWasGainedSinceAMomentIsValuedOnItsOwn() {
		Map<String, Long> before = Map.of("FLAWED_JASPER_GEM", 100L, "GLACITE", 50L);
		Map<String, Long> now = Map.of("FLAWED_JASPER_GEM", 130L, "GLACITE", 50L, "MYSTERY", 2L);
		ProfitValue value = ProfitValue.since(now, before, PRICES);
		assertEquals(120_000, value.coins(), 0.001);
		assertEquals(2, value.lines().size());
	}
}
