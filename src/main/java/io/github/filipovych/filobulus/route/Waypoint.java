package io.github.filipovych.filobulus.route;

public record Waypoint(int x, int y, int z, String label) {
	public Waypoint {
		label = label == null ? "" : label;
	}
}
