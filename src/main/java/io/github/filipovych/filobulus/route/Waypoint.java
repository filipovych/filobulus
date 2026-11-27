package io.github.filipovych.filobulus.route;

import java.util.Optional;

// a stop of a route, aim is the block to point at from there, like where a pickobulus goes off
public record Waypoint(int x, int y, int z, String label, Optional<Aim> aim) {
	public record Aim(int x, int y, int z) {
	}

	public Waypoint {
		label = label == null ? "" : label;
		aim = aim == null ? Optional.empty() : aim;
	}

	public Waypoint(int x, int y, int z, String label) {
		this(x, y, z, label, Optional.empty());
	}
}
