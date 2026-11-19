package io.github.filipovych.filobulus.shaft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ShaftTypeTest {
	private static final List<String> ALL_CODES = List.of(
			"TOPA_1", "TOPA_2", "SAPP_1", "SAPP_2", "AMET_1", "AMET_2", "AMBE_1", "AMBE_2", "JADE_1", "JADE_2",
			"RUBY_1", "RUBY_2", "RUBY_C", "ONYX_1", "ONYX_2", "ONYX_C", "AQUA_1", "AQUA_2", "AQUA_C",
			"CITR_1", "CITR_2", "CITR_C", "PERI_1", "PERI_2", "PERI_C",
			"JASP_1", "JASP_C", "OPAL_1", "OPAL_C",
			"TITA_1", "UMBE_1", "TUNG_1", "FAIR_1", "LITT_L");

	@Test
	void knowsExactlyThe34Codes() {
		assertEquals(34, ALL_CODES.size());
		assertEquals(ALL_CODES, ShaftType.all().stream().map(ShaftType::code).toList());
		for (String code : ALL_CODES) {
			assertEquals(code, ShaftType.parse(code).orElseThrow().code());
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"JASP_2", "TOPA_C", "TITA_2", "LITT_1", "jasp_1", "JASP1", "JASP_", "", "JASP_10", "m12A"})
	void rejectsCodesThatDoNotExist(String code) {
		assertEquals(Optional.empty(), ShaftType.parse(code));
	}

	@Test
	void rejectsNull() {
		assertEquals(Optional.empty(), ShaftType.parse(null));
		assertEquals(Optional.empty(), ShaftType.fromLastToken(null));
	}

	@Test
	void crystalShaftsShareOneRoute() {
		for (ShaftType type : ShaftType.all()) {
			if (type.isCrystal()) {
				assertEquals("CRYSTAL", type.routeKey());
			} else {
				assertEquals(type.code(), type.routeKey());
			}
		}
		assertEquals(7, ShaftType.all().stream().filter(ShaftType::isCrystal).count());
	}

	@Test
	void variantTwoIsNotCrystal() {
		assertFalse(ShaftType.parse("RUBY_2").orElseThrow().isCrystal());
		assertTrue(ShaftType.parse("RUBY_C").orElseThrow().isCrystal());
	}

	@Test
	void readsTheLastWordOfALine() {
		assertEquals("JASP_1", ShaftType.fromLastToken("11/19/26 m12A JASP_1").orElseThrow().code());
		assertEquals("JASP_C", ShaftType.fromLastToken("  11/19/26 m12A JASP_C  ").orElseThrow().code());
		assertEquals("LITT_L", ShaftType.fromLastToken("LITT_L").orElseThrow().code());
		assertEquals(Optional.empty(), ShaftType.fromLastToken("11/19/26 m12A"));
		assertEquals(Optional.empty(), ShaftType.fromLastToken(""));
	}

	@Test
	void displayNames() {
		assertEquals("Jasper", ShaftType.parse("JASP_1").orElseThrow().displayName());
		assertEquals("Jasper Crystal", ShaftType.parse("JASP_C").orElseThrow().displayName());
		assertEquals("Ruby 2", ShaftType.parse("RUBY_2").orElseThrow().displayName());
		assertEquals("Vanguard", ShaftType.parse("FAIR_1").orElseThrow().displayName());
		assertEquals("Littlefoot's Den", ShaftType.parse("LITT_L").orElseThrow().displayName());
	}
}
