package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

// the player's routes for outside the shafts
class CustomRoutesTest {
	private static final List<Waypoint> STOPS = List.of(new Waypoint(1, 2, 3, "1"), new Waypoint(4, 5, 6, "2"));

	private static CustomRoute add(CustomRoutes routes, String name, String area) {
		return routes.add(name, "", "coal", area, true, STOPS, null);
	}

	private static Optional<String> activeIn(CustomRoutes routes, String area) {
		return routes.activeIn(area).map(CustomRoute::id);
	}

	@Test
	void idsComeFromTheNameAndNeverRepeat() {
		CustomRoutes routes = new CustomRoutes();
		assertEquals("custom_coal_loop", add(routes, "Coal loop!", "Dwarven Mines").id());
		assertEquals("custom_coal_loop_2", add(routes, "Coal loop", "Dwarven Mines").id());
		assertEquals("custom_route", add(routes, "  ", "").id());
		assertEquals("Route", routes.byId("custom_route").orElseThrow().name());
		assertEquals("custom_kept", routes.add("Other", "", "", "", true, STOPS, "custom_kept").id());
	}

	@Test
	void aRouteOnlyShowsOnceItIsSwitchedOn() {
		CustomRoutes routes = new CustomRoutes();
		CustomRoute ruby = add(routes, "Ruby", "Crystal Hollows");
		assertEquals(Optional.empty(), activeIn(routes, "Crystal Hollows"));
		routes.use(ruby.id(), true);
		assertEquals(Optional.of(ruby.id()), activeIn(routes, "crystal hollows"));
		assertEquals(Optional.empty(), activeIn(routes, "Dwarven Mines"));
		assertEquals(Optional.empty(), activeIn(routes, ""));
		routes.use(ruby.id(), false);
		assertEquals(Optional.empty(), activeIn(routes, "Crystal Hollows"));
	}

	@Test
	void onlyOneRoutePerIslandIsInUse() {
		CustomRoutes routes = new CustomRoutes();
		CustomRoute ruby = add(routes, "Ruby", "Crystal Hollows");
		CustomRoute topaz = add(routes, "Topaz", "Crystal Hollows");
		CustomRoute coal = add(routes, "Coal", "Dwarven Mines");
		routes.use(ruby.id(), true);
		routes.use(coal.id(), true);
		routes.use(topaz.id(), true);
		assertEquals(Optional.of(topaz.id()), activeIn(routes, "Crystal Hollows"));
		assertFalse(routes.byId(ruby.id()).orElseThrow().on());
		assertTrue(routes.byId(coal.id()).orElseThrow().on());
	}

	@Test
	void aRouteForTheIslandWinsOverOneForEverywhere() {
		CustomRoutes routes = new CustomRoutes();
		CustomRoute any = add(routes, "Any", "");
		CustomRoute ruby = add(routes, "Ruby", "Crystal Hollows");
		routes.use(any.id(), true);
		routes.use(ruby.id(), true);
		assertEquals(Optional.of(ruby.id()), activeIn(routes, "Crystal Hollows"));
		assertEquals(Optional.of(any.id()), activeIn(routes, "Hub"));
		assertEquals(Optional.of(any.id()), activeIn(routes, ""));
	}

	@Test
	void switchingARouteOnKeepsItsRouteObject() {
		CustomRoutes routes = new CustomRoutes();
		CustomRoute ruby = add(routes, "Ruby", "");
		routes.use(ruby.id(), true);
		assertSame(ruby.route(), routes.byId(ruby.id()).orElseThrow().route());
	}

	@Test
	void anEditKeepsTheIdAndASwitchedOnRouteStaysOn() {
		CustomRoutes routes = new CustomRoutes();
		CustomRoute ruby = add(routes, "Ruby", "");
		routes.use(ruby.id(), true);
		CustomRoute on = routes.byId(ruby.id()).orElseThrow();
		routes.replace(on.edited("Ruby fast", "Me", "ruby", "Crystal Hollows", false));
		CustomRoute edited = routes.byId(ruby.id()).orElseThrow();
		assertEquals("Ruby fast", edited.name());
		assertEquals("Me", edited.route().author());
		assertEquals("Crystal Hollows", edited.area());
		assertFalse(edited.loop());
		assertTrue(edited.on());
		assertTrue(routes.remove(ruby.id()));
		assertFalse(routes.remove(ruby.id()));
	}
}
