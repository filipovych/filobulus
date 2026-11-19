package io.github.filipovych.filobulus.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.List;
import org.junit.jupiter.api.Test;

// line shapes come from other mods, not from a real capture yet
class LocationParserTest {
	private static final List<String> SHAFT_TAB = List.of("Info", "Area: Mineshaft", "Server: mini12A");
	private static final List<String> MINES_TAB = List.of("Info", "Area: Dwarven Mines", "Server: mini12A");

	@Test
	void mineshaftWithItsType() {
		List<String> sidebar = List.of("SKYBLOCK", "11/19/26 m12A JASP_1", " ⏣ Glacite Mineshafts");
		assertEquals(Location.mineshaft(ShaftType.parse("JASP_1").orElseThrow()), LocationParser.parse(SHAFT_TAB, sidebar));
	}

	@Test
	void mineshaftBeforeTheSidebarShowsTheType() {
		assertEquals(Location.mineshaft(), LocationParser.parse(SHAFT_TAB, List.of("SKYBLOCK", "11/19/26 m12A")));
	}

	@Test
	void glaciteTunnels() {
		assertEquals(Location.glaciteTunnels(), LocationParser.parse(MINES_TAB, List.of(" ⏣ Glacite Tunnels")));
		assertEquals(Location.glaciteTunnels(), LocationParser.parse(MINES_TAB, List.of("⏣ Great Glacite Lake")));
	}

	@Test
	void restOfTheDwarvenMinesIsElsewhere() {
		assertEquals(Location.elsewhere(), LocationParser.parse(MINES_TAB, List.of(" ⏣ Dwarven Base Camp")));
	}

	@Test
	void nothingToReadIsElsewhere() {
		assertEquals(Location.elsewhere(), LocationParser.parse(List.of(), List.of()));
		assertEquals(Location.elsewhere(), LocationParser.parse(MINES_TAB, List.of("11/19/26 m12A JASP_1")));
	}

	@Test
	void stripsColourCodes() {
		assertEquals(" ⏣ Glacite Tunnels", LocationParser.stripColor(" §7⏣ §bGlacite Tunnels"));
		assertEquals("Area: Mineshaft", LocationParser.stripColor("§b§lArea: §r§7Mineshaft"));
	}
}
