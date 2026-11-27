package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RouteLibraryTest {
	private final RouteLibrary library = TestRoutes.library();

	private static ShaftType type(String code) {
		return ShaftType.parse(code).orElseThrow();
	}

	@Test
	void anEmptyLibraryHasNothingForAnyShaft() {
		RouteLibrary empty = new RouteLibrary(List.of());
		assertTrue(empty.all().isEmpty());
		for (ShaftType shaft : ShaftType.all()) {
			assertTrue(empty.routesFor(shaft).isEmpty(), shaft.code());
		}
		assertFalse(empty.find("jasper").isPresent());
	}

	@Test
	void idsAndTitles() {
		assertEquals(6, library.all().size());
		assertEquals("Jasper", library.byId("jasper").orElseThrow().title());
		assertEquals("Jasper (Failsayfe)", library.byId("jasper_failsayfe").orElseThrow().title());
		assertEquals("Ruby 2", library.byId("ruby_2").orElseThrow().title());
		assertTrue(library.ids().containsAll(List.of("ruby_1", "ruby_2", "topaz_1", "crystal")));
	}

	@Test
	void everyCrystalShaftUsesTheCrystalRoute() {
		for (ShaftType shaft : ShaftType.all()) {
			if (shaft.isCrystal()) {
				assertEquals("crystal", library.routesFor(shaft).get(0).id(), shaft.code());
			}
		}
	}

	@Test
	void jasperHasADefaultAndAnAlternative() {
		assertEquals(List.of("jasper", "jasper_failsayfe"),
				library.routesFor(type("JASP_1")).stream().map(LibraryRoute::id).toList());
		assertEquals("jasper", library.routeFor(type("JASP_1"), "").orElseThrow().id());
		assertEquals("jasper_failsayfe", library.routeFor(type("JASP_1"), "jasper_failsayfe").orElseThrow().id());
		assertEquals("jasper", library.routeFor(type("JASP_1"), "ruby_1").orElseThrow().id());
	}

	@Test
	void shaftsWithoutARouteHaveNone() {
		for (String code : List.of("TOPA_2", "TITA_1", "UMBE_1", "TUNG_1", "FAIR_1", "LITT_L")) {
			assertTrue(library.routesFor(type(code)).isEmpty(), code);
			assertEquals(Optional.empty(), library.routeFor(type(code), ""), code);
		}
	}

	@Test
	void findsRoutesByWhatAPlayerTypes() {
		assertEquals("jasper", library.find("jasper").orElseThrow().id());
		assertEquals("jasper", library.find(" Jasper ").orElseThrow().id());
		assertEquals("jasper", library.find("JASP_1").orElseThrow().id());
		assertEquals("crystal", library.find("jasper_crystal").orElseThrow().id());
		assertEquals("crystal", library.find("ruby_c").orElseThrow().id());
		assertEquals("ruby_2", library.find("RUBY_2").orElseThrow().id());
		assertEquals("topaz_1", library.find("topaz").orElseThrow().id());
		assertFalse(library.find("ruby").isPresent());
		assertFalse(library.find("tungsten").isPresent());
		assertFalse(library.find("").isPresent());
		assertFalse(library.find(null).isPresent());
	}
}
