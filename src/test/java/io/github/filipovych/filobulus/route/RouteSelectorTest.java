package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.filipovych.filobulus.route.RouteSelector.LoadResult;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RouteSelectorTest {
	private static final ShaftType JASPER = ShaftType.parse("JASP_1").orElseThrow();
	private static final ShaftType JASPER_CRYSTAL = ShaftType.parse("JASP_C").orElseThrow();
	private static final ShaftType RUBY = ShaftType.parse("RUBY_1").orElseThrow();
	private static final ShaftType TUNGSTEN = ShaftType.parse("TUNG_1").orElseThrow();

	private static final class FakeSettings implements RouteSettings {
		Set<ShaftMaterial> autoLoad = EnumSet.of(ShaftMaterial.JASPER, ShaftMaterial.TUNGSTEN);
		String preferred = "";
		Optional<String> tunnels = Optional.empty();

		@Override
		public boolean autoLoad(ShaftType type) {
			return autoLoad.contains(type.material());
		}

		@Override
		public String preferredRouteId(ShaftType type) {
			return preferred;
		}

		@Override
		public Optional<String> tunnelsRouteId() {
			return tunnels;
		}
	}

	private FakeSettings settings;
	private RouteSelector selector;

	@BeforeEach
	void setUp() throws Exception {
		List<LibraryRoute> routes = new java.util.ArrayList<>(RouteLibrary.loadBundled().all());
		routes.add(new LibraryRoute("my_tunnels", "My tunnels route", "TUNNELS", List.of(new Waypoint(0, 100, 0, "1"))));
		settings = new FakeSettings();
		selector = new RouteSelector(new RouteLibrary(routes), settings);
	}

	private String activeId() {
		return selector.active().map(LibraryRoute::id).orElse(null);
	}

	@Test
	void nothingIsShownAtTheStart() {
		assertEquals(null, activeId());
	}

	@Test
	void tunnelsShowTheSelectedTunnelsRoute() {
		settings.tunnels = Optional.of("my_tunnels");
		selector.onLocation(Location.glaciteTunnels());
		assertEquals("my_tunnels", activeId());
	}

	@Test
	void tunnelsShowNothingWithoutASelection() {
		selector.onLocation(Location.glaciteTunnels());
		assertEquals(null, activeId());
	}

	@Test
	void enteringAShaftWithAutoLoadOnLoadsItsRoute() {
		settings.tunnels = Optional.of("my_tunnels");
		selector.onLocation(Location.glaciteTunnels());
		Optional<LibraryRoute> loaded = selector.onLocation(Location.mineshaft(JASPER));
		assertEquals("jasper", loaded.orElseThrow().id());
		assertEquals("jasper", activeId());
	}

	@Test
	void enteringAShaftWithAutoLoadOffShowsNothing() {
		settings.tunnels = Optional.of("my_tunnels");
		selector.onLocation(Location.glaciteTunnels());
		assertEquals(Optional.empty(), selector.onLocation(Location.mineshaft(RUBY)));
		assertEquals(null, activeId());
	}

	@Test
	void crystalShaftsLoadTheCrystalRoute() {
		selector.onLocation(Location.mineshaft(JASPER_CRYSTAL));
		assertEquals("crystal", activeId());
	}

	@Test
	void thePreferredAlternativeIsUsed() {
		settings.preferred = "jasper_failsayfe";
		selector.onLocation(Location.mineshaft(JASPER));
		assertEquals("jasper_failsayfe", activeId());
	}

	@Test
	void theTypeMayArriveAfterTheShaft() {
		assertEquals(Optional.empty(), selector.onLocation(Location.mineshaft()));
		assertEquals(null, activeId());
		assertEquals("jasper", selector.onLocation(Location.mineshaft(JASPER)).orElseThrow().id());
	}

	@Test
	void theRouteIsAnnouncedOnlyOnce() {
		assertEquals("jasper", selector.onLocation(Location.mineshaft(JASPER)).orElseThrow().id());
		assertEquals(Optional.empty(), selector.onLocation(Location.mineshaft(JASPER)));
		assertEquals("jasper", activeId());
	}

	@Test
	void autoLoadOnButNoRouteInThePackShowsNothing() {
		assertEquals(Optional.empty(), selector.onLocation(Location.mineshaft(TUNGSTEN)));
		assertEquals(null, activeId());
	}

	@Test
	void loadingByCommandReplacesTheRoute() {
		selector.onLocation(Location.mineshaft(JASPER));
		LoadResult result = selector.load("jasper_failsayfe");
		assertInstanceOf(LoadResult.Loaded.class, result);
		assertEquals("jasper_failsayfe", activeId());
	}

	@Test
	void aCommandLoadIsNotOverriddenWhenTheTypeArrivesLate() {
		selector.onLocation(Location.mineshaft());
		selector.load("ruby_1");
		assertEquals(Optional.empty(), selector.onLocation(Location.mineshaft(JASPER)));
		assertEquals("ruby_1", activeId());
	}

	@Test
	void loadingByCommandOutsideAShaftIsRefused() {
		assertInstanceOf(LoadResult.NotInMineshaft.class, selector.load("jasper"));
		selector.onLocation(Location.glaciteTunnels());
		assertInstanceOf(LoadResult.NotInMineshaft.class, selector.load("jasper"));
		assertEquals(null, activeId());
	}

	@Test
	void loadingAnUnknownNameChangesNothing() {
		selector.onLocation(Location.mineshaft(JASPER));
		assertInstanceOf(LoadResult.UnknownRoute.class, selector.load("nope"));
		assertEquals("jasper", activeId());
	}

	@Test
	void unloadHidesTheRouteUntilTheNextShaft() {
		selector.onLocation(Location.mineshaft(JASPER));
		selector.unload();
		assertEquals(null, activeId());
		selector.onLocation(Location.mineshaft(JASPER));
		assertEquals(null, activeId());
		selector.onLocation(Location.glaciteTunnels());
		selector.onLocation(Location.mineshaft(JASPER));
		assertEquals("jasper", activeId());
	}

	@Test
	void leavingTheShaftClearsItsRouteAndBringsTheTunnelsRouteBack() {
		settings.tunnels = Optional.of("my_tunnels");
		selector.onLocation(Location.glaciteTunnels());
		selector.onLocation(Location.mineshaft(JASPER));
		selector.load("ruby_1");
		selector.onLocation(Location.glaciteTunnels());
		assertEquals("my_tunnels", activeId());
		// the command load died with the shaft, next jasper shaft auto-loads again
		selector.onLocation(Location.mineshaft(JASPER));
		assertEquals("jasper", activeId());
	}

	@Test
	void nothingIsShownAnywhereElse() {
		settings.tunnels = Optional.of("my_tunnels");
		selector.onLocation(Location.mineshaft(JASPER));
		selector.onLocation(Location.elsewhere());
		assertEquals(null, activeId());
	}

	@Test
	void goingStraightFromOneShaftToAnotherStartsFresh() {
		selector.onLocation(Location.mineshaft(JASPER));
		selector.load("ruby_1");
		selector.onLocation(Location.mineshaft(JASPER_CRYSTAL));
		assertEquals("crystal", activeId());
	}
}
