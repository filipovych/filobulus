package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.filipovych.filobulus.shaft.ShaftLayout;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;

class RouteImportTest {
	private static final String COLEWEIGHT = "[{\"x\":1,\"y\":2,\"z\":3,\"options\":{\"name\":\"1\"}}]";
	private static final ShaftLayout RUBY_2 = ShaftLayout.byKey("RUBY_2").orElseThrow();

	private static String skyblocker(String... groupNames) throws Exception {
		StringBuilder json = new StringBuilder("[");
		for (String name : groupNames) {
			json.append(json.length() > 1 ? "," : "").append("{\"name\":\"").append(name)
					.append("\",\"island\":\"mineshaft\",\"waypoints\":[{\"pos\":[1,2,3],\"name\":\"1\"}]}");
		}
		ByteArrayOutputStream packed = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(packed)) {
			gzip.write(json.append("]").toString().getBytes(StandardCharsets.UTF_8));
		}
		return RouteCodec.SKYBLOCKER_PREFIX + Base64.getEncoder().encodeToString(packed.toByteArray());
	}

	@Test
	void aColeweightRouteDoesNotKnowItsShaft() throws Exception {
		RouteCollection read = RouteImport.read(COLEWEIGHT);
		assertEquals(1, read.routes().size());
		assertEquals("", read.routes().get(0).shaft());
		assertEquals("", read.routes().get(0).name());
	}

	@Test
	void aSkyblockerGroupNamedAfterATypeCodeKnowsItsShaft() throws Exception {
		List<SharedRoute> routes = RouteImport.read(skyblocker("JASP_1", "ONYX_C", "my fast one")).routes();
		assertEquals(List.of("JASP_1", "CRYSTAL", ""), routes.stream().map(SharedRoute::shaft).toList());
		assertEquals(List.of("", "", "my fast one"), routes.stream().map(SharedRoute::name).toList());
	}

	@Test
	void ourOwnCollectionIsRead() throws Exception {
		RouteCollection collection = new RouteCollection("x", List.of(
				new SharedRoute("JASP_1", "Fast", "", List.of(new Waypoint(1, 2, 3, "1")))));
		assertEquals(collection, RouteImport.read(CollectionCodec.export(collection)));
	}

	@Test
	void aLooseRouteGoesToTheTarget() throws Exception {
		List<SharedRoute> assigned = RouteImport.assign(RouteImport.read(COLEWEIGHT), RUBY_2);
		assertEquals(List.of("RUBY_2"), assigned.stream().map(SharedRoute::shaft).toList());
	}

	@Test
	void aRouteThatNamesItsShaftKeepsIt() throws Exception {
		List<SharedRoute> assigned = RouteImport.assign(RouteImport.read(skyblocker("JASP_1", "loose")), RUBY_2);
		assertEquals(List.of("JASP_1", "RUBY_2"), assigned.stream().map(SharedRoute::shaft).toList());
		assertEquals(List.of("JASP_1"),
				RouteImport.assign(RouteImport.read(skyblocker("JASP_1")), null).stream().map(SharedRoute::shaft).toList());
	}

	@Test
	void refusesWhatItCannotPlace() {
		assertThrows(RouteFormatException.class, () -> RouteImport.assign(RouteImport.read(COLEWEIGHT), null));
		assertThrows(RouteFormatException.class,
				() -> RouteImport.assign(RouteImport.read(skyblocker("one", "two")), RUBY_2));
		assertThrows(RouteFormatException.class, () -> RouteImport.read(""));
		assertThrows(RouteFormatException.class, () -> RouteImport.read(null));
		assertThrows(RouteFormatException.class, () -> RouteImport.read("hello"));
	}
}
