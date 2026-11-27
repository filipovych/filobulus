package io.github.filipovych.filobulus.route;

import java.util.List;

// a small library of shaft routes for the tests, the mod itself ships none
final class TestRoutes {
	static final List<Waypoint> THREE = List.of(new Waypoint(-176, 12, -183, "1"), new Waypoint(-169, 11, -176, "2"),
			new Waypoint(-167, 11, -184, "3"));

	private TestRoutes() {
	}

	// jasper with an alternative, a few other shafts and the one crystal route, all marked as bundled
	static RouteLibrary library() {
		return new RouteLibrary(List.of(
				new LibraryRoute("jasper", "Jasper", "JASP_1", THREE),
				new LibraryRoute("jasper_failsayfe", "Jasper (Failsayfe)", "JASP_1", THREE.subList(0, 2), "Failsayfe", "Failsayfe", true),
				new LibraryRoute("ruby_1", "Ruby 1", "RUBY_1", THREE),
				new LibraryRoute("ruby_2", "Ruby 2", "RUBY_2", THREE),
				new LibraryRoute("topaz_1", "Topaz 1", "TOPA_1", THREE),
				new LibraryRoute("crystal", "Crystal", "CRYSTAL", THREE)));
	}
}
