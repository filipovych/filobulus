package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CollectionCodecTest {
	private static final List<Waypoint> STOPS = List.of(
			new Waypoint(-176, 12, -183, "1"), new Waypoint(-170, 14, -180, "2"), new Waypoint(1, 2, 3, "end"));
	private static final RouteCollection COLLECTION = new RouteCollection("My routes", List.of(
			new SharedRoute("JASP_1", "Fast", "Someone", STOPS),
			new SharedRoute("CRYSTAL", "Loop", "", STOPS.subList(0, 2))));

	@Test
	void whatIsExportedComesBackTheSame() throws Exception {
		String exported = CollectionCodec.export(COLLECTION);
		assertTrue(exported.startsWith("[Filobulus-Routes-V1]"));
		assertEquals(COLLECTION, CollectionCodec.parse(exported));
		assertEquals(COLLECTION, CollectionCodec.parse("  " + exported + "\n"));
	}

	@Test
	void readsThePlainJsonToo() throws Exception {
		assertEquals(COLLECTION, CollectionCodec.parse(CollectionCodec.toJson(COLLECTION).toString()));
	}

	@Test
	void stopsAreNumberedWhenTheyHaveNoLabel() throws Exception {
		RouteCollection read = CollectionCodec.parse(
				"{\"routes\":[{\"shaft\":\"RUBY_2\",\"stops\":[[1,2,3],[4.9,5,-6.1]]}]}");
		SharedRoute route = read.routes().get(0);
		assertEquals("", route.name());
		assertEquals(List.of(new Waypoint(1, 2, 3, "1"), new Waypoint(4, 5, -7, "2")), route.stops());
	}

	@Test
	void aStopKeepsItsAim() throws Exception {
		List<Waypoint> stops = List.of(new Waypoint(45, 119, 280, "180 (346 pity)", java.util.Optional.of(new Waypoint.Aim(45, 118, 280))),
				new Waypoint(1, 2, 3, "2", java.util.Optional.of(new Waypoint.Aim(4, 5, 6))));
		assertEquals("[[45,119,280,\"180 (346 pity)\",[45,118,280]],[1,2,3,\"2\",[4,5,6]]]", CollectionCodec.stopsToJson(stops).toString());
		assertEquals(stops, CollectionCodec.stopsFromJson(CollectionCodec.stopsToJson(stops)));
		// a fifth entry that is not a position is left alone
		assertEquals(List.of(new Waypoint(1, 2, 3, "x")), CollectionCodec.stopsFromJson(
				com.google.gson.JsonParser.parseString("[[1,2,3,\"x\",7]]")));
	}

	@Test
	void routesForUnknownShaftsAreLeftOut() throws Exception {
		RouteCollection read = CollectionCodec.parse("{\"routes\":["
				+ "{\"shaft\":\"NOPE_9\",\"stops\":[[1,2,3]]},{\"shaft\":\"JASP_1\",\"stops\":[[1,2,3]]}]}");
		assertEquals(List.of("JASP_1"), read.routes().stream().map(SharedRoute::shaft).toList());
	}

	@Test
	void rejectsWhatIsNotACollection() {
		for (String text : List.of("{}", "{\"routes\":[]}", "{\"routes\":[{\"shaft\":\"NOPE_9\",\"stops\":[[1,2,3]]}]}",
				"{\"routes\":[{\"shaft\":\"JASP_1\",\"stops\":[[1,2]]}]}",
				"{\"routes\":[{\"shaft\":\"JASP_1\",\"stops\":[[1,\"a\",3]]}]}",
				"[Filobulus-Routes-V1]not base64!", "[Filobulus-Routes-V1]aGVsbG8=", "{broken")) {
			assertThrows(RouteFormatException.class, () -> CollectionCodec.parse(text), text);
		}
	}
}
