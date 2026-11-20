package io.github.filipovych.filobulus.detect;

import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.Location.Area;

// one odd read of the tab list must not look like leaving the shaft, that would start the route over
public final class SteadyLocation {
	private static final int READS_TO_LEAVE = 3;

	private Location current = Location.elsewhere();
	private int readsOutside;

	public Location current() {
		return current;
	}

	// everything is believed at once, except leaving a mineshaft, that takes a few reads in a row
	public Location update(Location read) {
		boolean leaving = current.area() == Area.MINESHAFT && read.area() != Area.MINESHAFT;
		if (leaving && ++readsOutside < READS_TO_LEAVE) {
			return current;
		}
		readsOutside = 0;
		current = read;
		return current;
	}

	// for when we know for sure, like a world change
	public void reset() {
		readsOutside = 0;
		current = Location.elsewhere();
	}
}
