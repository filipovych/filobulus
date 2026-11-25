package io.github.filipovych.filobulus.detect;

import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// works out where the player is from the tab list and sidebar text, all the game text we match on is here
public final class LocationParser {
	private static final String TAB_AREA = "Area: ";
	private static final String MINESHAFT = "Mineshaft";
	private static final String SIDEBAR_MARK = "⏣";
	private static final Set<String> TUNNELS = Set.of("Glacite Tunnels", "Great Glacite Lake");

	private LocationParser() {
	}

	// lines need their colour codes stripped first
	public static Location parse(List<String> tabLines, List<String> sidebarLines) {
		for (String line : tabLines) {
			String text = line.strip();
			if (text.startsWith(TAB_AREA) && text.substring(TAB_AREA.length()).strip().equals(MINESHAFT)) {
				return shaftType(sidebarLines).map(Location::mineshaft).orElseGet(Location::mineshaft);
			}
		}
		for (String line : sidebarLines) {
			String text = line.strip();
			if (text.startsWith(SIDEBAR_MARK) && TUNNELS.contains(text.substring(SIDEBAR_MARK.length()).strip())) {
				return Location.glaciteTunnels();
			}
		}
		return Location.elsewhere();
	}

	// the island as the tab list names it, empty outside of skyblock
	public static String areaName(List<String> tabLines) {
		for (String line : tabLines) {
			String text = line.strip();
			if (text.startsWith(TAB_AREA)) {
				return text.substring(TAB_AREA.length()).strip();
			}
		}
		return "";
	}

	public static String stripColor(String text) {
		return text.replaceAll("§.", "");
	}

	private static Optional<ShaftType> shaftType(List<String> sidebarLines) {
		for (String line : sidebarLines) {
			Optional<ShaftType> type = ShaftType.fromLastToken(line);
			if (type.isPresent()) {
				return type;
			}
		}
		return Optional.empty();
	}
}
