package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.route.RouteSelector.LoadResult;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// what the player picked on the routes screen decides what loads
class PickedRouteTest {
	private static final ShaftLayout JASPER = ShaftLayout.byKey("JASP_1").orElseThrow();
	private static final List<Waypoint> STOPS = List.of(new Waypoint(1, 2, 3, "1"), new Waypoint(4, 5, 6, "2"));

	private RouteLibrary library;
	private RouteChoices choices;
	private RouteSelector selector;

	@BeforeEach
	void setUp() throws Exception {
		library = RouteLibrary.loadBundled();
		choices = new RouteChoices();
		selector = new RouteSelector(library, choices);
	}

	private static ShaftType type(String code) {
		return ShaftType.parse(code).orElseThrow();
	}

	@Test
	void jasperAndCrystalAutoLoadByDefault() {
		assertTrue(choices.autoLoad(type("JASP_1")));
		for (ShaftType shaft : ShaftType.all()) {
			if (shaft.isCrystal()) {
				assertTrue(choices.autoLoad(shaft), shaft.code());
			}
		}
		assertFalse(choices.autoLoad(type("RUBY_1")));
		choices.setAutoLoad("CRYSTAL", false);
		assertFalse(choices.autoLoad(type("ONYX_C")));
	}

	@Test
	void thePickedRouteIsTheOneThatAutoLoads() {
		LibraryRoute mine = library.addUser(JASPER, "Mine", "", STOPS, null);
		choices.pick("JASP_1", mine.id());
		assertSame(mine, selector.onLocation(Location.mineshaft(type("JASP_1"))).orElseThrow());
	}

	@Test
	void aPickThatIsGoneFallsBackToTheDefault() {
		choices.pick("JASP_1", "deleted_since");
		assertEquals("jasper", selector.onLocation(Location.mineshaft(type("JASP_1"))).orElseThrow().id());
	}

	@Test
	void loadingByShaftNameGivesThePickedRoute() {
		choices.pick("JASP_1", "jasper_failsayfe");
		choices.setAutoLoad("JASP_1", false);
		selector.onLocation(Location.mineshaft(type("JASP_1")));
		assertEquals("jasper_failsayfe", ((LoadResult.Loaded) selector.load("jasper")).route().id());
		assertEquals("jasper_failsayfe", ((LoadResult.Loaded) selector.load("JASP_1")).route().id());
		assertEquals("ruby_1", ((LoadResult.Loaded) selector.load("ruby_1")).route().id());
	}

	@Test
	void loadHereTakesTheShaftWeAreIn() {
		assertInstanceOf(LoadResult.NotInMineshaft.class, selector.loadHere());
		selector.onLocation(Location.mineshaft());
		assertEquals("", ((LoadResult.NoRouteHere) selector.loadHere()).code());
		selector.onLocation(Location.mineshaft(type("RUBY_2")));
		assertEquals(null, selector.active().orElse(null));
		assertEquals("ruby_2", ((LoadResult.Loaded) selector.loadHere()).route().id());
		assertEquals("ruby_2", selector.active().orElseThrow().id());
		selector.onLocation(Location.elsewhere());
		selector.onLocation(Location.mineshaft(type("TUNG_1")));
		assertEquals("TUNG_1", ((LoadResult.NoRouteHere) selector.loadHere()).code());
	}
}
