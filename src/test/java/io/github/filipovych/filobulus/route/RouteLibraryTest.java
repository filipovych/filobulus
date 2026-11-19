package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RouteLibraryTest {
	private static RouteLibrary library;

	@BeforeAll
	static void load() throws Exception {
		library = RouteLibrary.loadBundled();
	}

	private static ShaftType type(String code) {
		return ShaftType.parse(code).orElseThrow();
	}

	@Test
	void loadsTheWholePack() {
		// 17 type routes + crystal + failsayfe
		assertEquals(19, library.all().size());
		assertEquals(47, library.byId("jasper").orElseThrow().stops().size());
		assertEquals(39, library.byId("jasper_failsayfe").orElseThrow().stops().size());
		assertEquals(43, library.byId("crystal").orElseThrow().stops().size());
	}

	@Test
	void everyRouteIsNumberedInOrder() {
		for (LibraryRoute route : library.all()) {
			for (int i = 0; i < route.stops().size(); i++) {
				assertEquals(Integer.toString(i + 1), route.stops().get(i).label(), route.id());
			}
		}
	}

	@Test
	void idsAndTitles() {
		assertEquals("Jasper", library.byId("jasper").orElseThrow().title());
		assertEquals("Jasper (Failsayfe)", library.byId("jasper_failsayfe").orElseThrow().title());
		assertEquals("Ruby 2", library.byId("ruby_2").orElseThrow().title());
		assertEquals("Crystal", library.byId("crystal").orElseThrow().title());
		assertTrue(library.ids().containsAll(List.of("opal", "ruby_1", "ruby_2", "topaz_1", "peridot_1", "peridot_2")));
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
		for (String code : List.of("TOPA_2", "SAPP_2", "AMET_2", "AMBE_2", "JADE_2", "TITA_1", "UMBE_1", "TUNG_1", "FAIR_1", "LITT_L")) {
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
