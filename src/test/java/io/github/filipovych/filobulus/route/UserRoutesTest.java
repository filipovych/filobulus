package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.filipovych.filobulus.shaft.ShaftLayout;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// the player's own routes next to ones marked as bundled
class UserRoutesTest {
	private static final ShaftLayout JASPER = ShaftLayout.byKey("JASP_1").orElseThrow();
	private static final ShaftLayout TUNGSTEN = ShaftLayout.byKey("TUNG_1").orElseThrow();
	private static final List<Waypoint> STOPS = List.of(new Waypoint(1, 2, 3, "1"), new Waypoint(4, 5, 6, "2"));
	private static final List<Waypoint> OTHER = List.of(new Waypoint(9, 9, 9, "1"));

	private RouteLibrary library;

	@BeforeEach
	void setUp() {
		library = TestRoutes.library();
	}

	@Test
	void anAddedRouteSitsAfterTheBundledOnes() {
		LibraryRoute added = library.addUser(JASPER, "My Fast one!", "Me", STOPS, null);
		assertEquals("jasper_my_fast_one", added.id());
		assertEquals("Jasper (My Fast one!)", added.title());
		assertFalse(added.bundled());
		assertEquals(List.of("jasper", "jasper_failsayfe", "jasper_my_fast_one"),
				library.routesFor("JASP_1").stream().map(LibraryRoute::id).toList());
		assertEquals(List.of(added), library.userRoutes());
	}

	@Test
	void theSameStopsAreNotAddedTwice() {
		LibraryRoute first = library.addUser(JASPER, "One", "", STOPS, null);
		assertSame(first, library.addUser(JASPER, "Two", "", STOPS, null));
		// the bundled default counts too
		LibraryRoute bundled = library.byId("jasper").orElseThrow();
		assertSame(bundled, library.addUser(JASPER, "Copy", "", bundled.stops(), null));
	}

	@Test
	void idsNeverClash() {
		assertEquals("jasper_failsayfe_2", library.addUser(JASPER, "Failsayfe", "", STOPS, null).id());
		assertEquals("jasper_custom", library.addUser(JASPER, "???", "", OTHER, null).id());
		assertEquals("Jasper (Imported)", library.addUser(JASPER, " ", "", List.of(new Waypoint(0, 0, 0, "1")), null).title());
	}

	@Test
	void aSavedIdIsKept() {
		assertEquals("my_old_id", library.addUser(JASPER, "Renamed since", "", STOPS, "my_old_id").id());
		// unless something else has it by now
		assertEquals("jasper_x", library.addUser(JASPER, "x", "", OTHER, "jasper").id());
	}

	@Test
	void onlyThePlayersRoutesCanBeRemoved() {
		LibraryRoute added = library.addUser(JASPER, "Mine", "", STOPS, null);
		assertFalse(library.removeUser("jasper"));
		assertTrue(library.removeUser(added.id()));
		assertFalse(library.removeUser(added.id()));
		assertEquals(2, library.routesFor("JASP_1").size());
	}

	@Test
	void aShaftWithoutABundledRouteGetsItsNameFromTheFirstImport() {
		assertFalse(library.find("tungsten").isPresent());
		LibraryRoute added = library.addUser(TUNGSTEN, "Mine", "", STOPS, null);
		assertSame(added, library.find("tungsten").orElseThrow());
		assertSame(added, library.find("TUNG_1").orElseThrow());
	}

	@Test
	void aRenamedRouteKeepsItsIdAndItsPlace() {
		library.addUser(JASPER, "One", "", STOPS, null);
		LibraryRoute second = library.addUser(JASPER, "Two", "", OTHER, null);
		LibraryRoute renamed = library.renameUser("jasper_one", "Fast", "Friend").orElseThrow();
		assertEquals("jasper_one", renamed.id());
		assertEquals("Jasper (Fast)", renamed.title());
		assertEquals("Friend", renamed.author());
		assertEquals(List.of("jasper", "jasper_failsayfe", "jasper_one", "jasper_two"),
				library.routesFor("JASP_1").stream().map(LibraryRoute::id).toList());
		assertSame(renamed, library.byId("jasper_one").orElseThrow());
		assertSame(second, library.byId("jasper_two").orElseThrow());
	}

	@Test
	void aBundledRouteCanNotBeRenamed() {
		assertTrue(library.renameUser("jasper", "Mine", "").isEmpty());
		assertTrue(library.renameUser("nope", "Mine", "").isEmpty());
	}
}
