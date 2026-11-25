package io.github.filipovych.filobulus.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

// made up lines in the shape skyhanni reads them, until there is a capture of a real one
class EffectReaderTest {
	@Test
	void theTabWidgetGivesThePotionAndItsTime() {
		EffectReader.Read read = EffectReader.read(
				List.of("Active Effects: (3)", " Haste III: 2m", " Cold Resistance IV: 12m 30s"), List.of());
		assertTrue(read.widget());
		assertEquals(OptionalLong.of(750_000), read.millis());
		assertEquals(4, read.tier());
	}

	@Test
	void theWidgetWithoutThePotionMeansThereIsNone() {
		EffectReader.Read read = EffectReader.read(List.of("Active Effects: (1)", " Haste III: 2m"), List.of());
		assertTrue(read.widget());
		assertTrue(read.millis().isEmpty());
	}

	@Test
	void withoutTheWidgetNothingIsKnown() {
		EffectReader.Read read = EffectReader.read(List.of("Area: Mineshaft"), List.of("Cold: -20"));
		assertFalse(read.widget());
		assertTrue(read.millis().isEmpty());
	}

	@Test
	void theSidebarCanShowItToo() {
		EffectReader.Read read = EffectReader.read(List.of(), List.of("Cold Resistance III 45s"));
		assertEquals(OptionalLong.of(45_000), read.millis());
		assertEquals(3, read.tier());
	}

	@Test
	void theEffectsMenuHasTheTimeInTheLore() {
		assertTrue(EffectReader.isEffectsMenu("Active Effects"));
		assertTrue(EffectReader.isEffectsMenu("(1/2) Active Effects"));
		assertFalse(EffectReader.isEffectsMenu("Effects"));
		assertEquals(OptionalLong.of(754_000),
				EffectReader.menuItem("Cold Resistance IV", List.of("Grants 10 Cold Resistance", "", "Remaining: 12:34")));
		assertEquals(OptionalLong.of(3_723_000), EffectReader.menuItem("Cold Resistance IV", List.of("Remaining: 1:02:03")));
		assertTrue(EffectReader.menuItem("Haste III", List.of("Remaining: 1:00")).isEmpty());
	}

	@Test
	void durationsAddUp() {
		assertEquals(3_723_000, EffectReader.duration("1h 2m 3s"));
		assertEquals(0, EffectReader.duration(""));
	}
}
