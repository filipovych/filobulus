package io.github.filipovych.filobulus.shaft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ShaftLayoutTest {
	private static ShaftLayout of(String code) {
		return ShaftLayout.of(ShaftType.parse(code).orElseThrow());
	}

	@Test
	void everyTypeHasALayoutAndCrystalIsOnlyOne() {
		// 34 codes, the 7 crystal ones fold into one
		assertEquals(28, ShaftLayout.all().size());
		assertEquals(1, ShaftLayout.all().stream().filter(ShaftLayout::isCrystal).count());
		for (ShaftType type : ShaftType.all()) {
			assertTrue(ShaftLayout.of(type).types().contains(type), type.code());
		}
	}

	@Test
	void allCrystalShaftsAreTheSameLayout() {
		ShaftLayout crystal = of("JASP_C");
		assertEquals("CRYSTAL", crystal.key());
		assertEquals(7, crystal.types().size());
		for (String code : List.of("OPAL_C", "RUBY_C", "ONYX_C", "AQUA_C", "CITR_C", "PERI_C")) {
			assertEquals(crystal, of(code));
		}
		assertEquals(Optional.empty(), crystal.material());
	}

	@Test
	void titlesAndNames() {
		assertEquals("Jasper", of("JASP_1").title());
		assertEquals("jasper", of("JASP_1").name());
		assertEquals("Ruby 2", of("RUBY_2").title());
		assertEquals("ruby_2", of("RUBY_2").name());
		assertEquals("Topaz 1", of("TOPA_1").title());
		assertEquals("Crystal", of("RUBY_C").title());
		assertEquals("crystal", of("RUBY_C").name());
		assertEquals("Littlefoot's Den", of("LITT_L").title());
	}

	@Test
	void jasperAndCrystalComeFirst() {
		assertEquals(List.of("JASP_1", "CRYSTAL", "RUBY_1", "RUBY_2"),
				ShaftLayout.all().stream().limit(4).map(ShaftLayout::key).toList());
	}

	@Test
	void findsALayoutByTheNamesRoutesComeWith() {
		assertEquals("JASP_1", ShaftLayout.named("JASP_1").orElseThrow().key());
		assertEquals("JASP_1", ShaftLayout.named(" jasp_1 ").orElseThrow().key());
		assertEquals("CRYSTAL", ShaftLayout.named("ONYX_C").orElseThrow().key());
		assertEquals("CRYSTAL", ShaftLayout.named("Crystal").orElseThrow().key());
		assertEquals("RUBY_2", ShaftLayout.named("Ruby 2").orElseThrow().key());
		assertEquals(Optional.empty(), ShaftLayout.named("my fast route"));
		assertEquals(Optional.empty(), ShaftLayout.named(null));
	}
}
