package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RouteCodecTest {
	private static final String TWO_STOPS = """
			[{"x":-176,"y":12,"z":-183,"r":0.0,"g":1.0,"b":0.0,"options":{"name":"1"}},
			 {"x":-170,"y":14,"z":-180,"r":0.0,"g":1.0,"b":0.0,"options":{"name":"2"}}]""";

	@Test
	void readsColeweightJson() throws Exception {
		List<Waypoint> stops = RouteCodec.parseColeweight(TWO_STOPS);
		assertEquals(List.of(new Waypoint(-176, 12, -183, "1"), new Waypoint(-170, 14, -180, "2")), stops);
	}

	@Test
	void acceptsNumberNamesAndMissingNames() throws Exception {
		String json = """
				[{"x":1,"y":2,"z":3,"options":{"name":7}},
				 {"x":1,"y":2,"z":4,"options":{"name":2.5}},
				 {"x":1,"y":2,"z":5,"options":{}},
				 {"x":1,"y":2,"z":6}]""";
		List<Waypoint> stops = RouteCodec.parseColeweight(json);
		assertEquals(List.of("7", "2.5", "3", "4"), stops.stream().map(Waypoint::label).toList());
	}

	@Test
	void floorsFractionalCoordinates() throws Exception {
		List<Waypoint> stops = RouteCodec.parseColeweight("[{\"x\":-0.5,\"y\":10.9,\"z\":3.0}]");
		assertEquals(new Waypoint(-1, 10, 3, "1"), stops.get(0));
	}

	@Test
	void ignoresAByteOrderMark() throws Exception {
		assertEquals(2, RouteCodec.parseColeweight('﻿' + TWO_STOPS).size());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"[]",
			"{}",
			"not json",
			"[1,2,3]",
			"[{\"x\":1,\"y\":2}]",
			"[{\"x\":\"a\",\"y\":2,\"z\":3}]",
			"[{\"x\":1e300,\"y\":2,\"z\":3}]"})
	void rejectsBrokenRoutes(String json) {
		assertThrows(RouteFormatException.class, () -> RouteCodec.parseColeweight(json));
	}

	@Test
	void exportReadsBackTheSame() throws Exception {
		List<Waypoint> stops = RouteCodec.parseColeweight(TWO_STOPS);
		assertEquals(stops, RouteCodec.parseColeweight(RouteCodec.toColeweight(stops)));
	}

	@Test
	void readsASkyblockerString() throws Exception {
		// made from the documented layout, swap for a real export when we have one
		String groups = """
				[{"name":"Jasper","island":"mineshaft","ordered":true,"renderThroughWalls":true,
				  "waypoints":[{"pos":[-176,12,-183],"name":"1","colorComponents":[0,1,0],"alpha":0.5,"shouldRender":true},
				               {"pos":[-170,14,-180],"name":"2","colorComponents":[0,1,0],"alpha":0.5,"shouldRender":true}]},
				 {"name":"Empty","island":"mineshaft","waypoints":[]}]""";
		List<Route> routes = RouteCodec.parseSkyblocker(RouteCodec.SKYBLOCKER_PREFIX + gzipBase64(groups));
		assertEquals(1, routes.size());
		assertEquals("Jasper", routes.get(0).name());
		assertEquals(List.of(new Waypoint(-176, 12, -183, "1"), new Waypoint(-170, 14, -180, "2")), routes.get(0).stops());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"[Skyblocker-Waypoint-Data-V1]",
			"[Skyblocker-Waypoint-Data-V1]!!!not base64!!!",
			"[Skyblocker-Waypoint-Data-V1]aGVsbG8gd29ybGQ="})
	void rejectsDamagedSkyblockerStrings(String export) {
		assertThrows(RouteFormatException.class, () -> RouteCodec.parseSkyblocker(export));
	}

	@Test
	void parseAnyPicksTheFormat() throws Exception {
		List<Route> fromJson = RouteCodec.parseAny("  " + TWO_STOPS + "\n", "My route");
		assertEquals("My route", fromJson.get(0).name());
		assertEquals(2, fromJson.get(0).stops().size());

		String export = RouteCodec.SKYBLOCKER_PREFIX + gzipBase64("[{\"name\":\"A\",\"waypoints\":[{\"pos\":[1,2,3]}]}]");
		assertEquals("A", RouteCodec.parseAny(export, "ignored").get(0).name());

		assertThrows(RouteFormatException.class, () -> RouteCodec.parseAny("", "x"));
		assertThrows(RouteFormatException.class, () -> RouteCodec.parseAny("WP:abc", "x"));
	}

	private static String gzipBase64(String text) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
			gzip.write(text.getBytes(StandardCharsets.UTF_8));
		}
		return Base64.getEncoder().encodeToString(bytes.toByteArray());
	}
}
