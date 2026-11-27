package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

// the routes that ship with the mod
class BundledRoutesTest {
	@Test
	void theGlacitePickobulusSpotsShipWithAnAimEach() throws Exception {
		List<CustomRoute> routes = BundledRoutes.load();
		assertEquals(1, routes.size());
		CustomRoute glacite = routes.get(0);
		assertEquals("glacite_pickobulus", glacite.id());
		assertEquals("Glacite Pickobulus", glacite.name());
		assertEquals("SoSeaL1", glacite.route().author());
		assertEquals(CustomRoute.TUNNELS, glacite.area());
		assertEquals("littlefoot", glacite.icon());
		assertTrue(glacite.loop());
		assertTrue(glacite.bundled());
		assertFalse(glacite.on());
		assertEquals(20, glacite.stops().size());
		for (Waypoint stop : glacite.stops()) {
			assertTrue(stop.aim().isPresent(), stop.label());
			assertFalse(stop.label().isEmpty());
			// the aim is a few blocks from where you stand
			Waypoint.Aim aim = stop.aim().orElseThrow();
			assertTrue(Math.abs(aim.x() - stop.x()) + Math.abs(aim.y() - stop.y()) + Math.abs(aim.z() - stop.z()) <= 12, stop.label());
		}
	}
}
