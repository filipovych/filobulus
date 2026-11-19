package io.github.filipovych.filobulus.route;

import java.util.List;
import java.util.Optional;

// walks a route one stop at a time, the line points at next()
public final class RouteProgress {
	private final List<Waypoint> stops;
	private final double reach;
	private int index;

	public RouteProgress(List<Waypoint> stops, double reach) {
		this.stops = List.copyOf(stops);
		this.reach = reach;
	}

	// empty once the last stop was reached, the route is done then
	public Optional<Waypoint> next() {
		return finished() ? Optional.empty() : Optional.of(stops.get(index));
	}

	public boolean finished() {
		return index >= stops.size();
	}

	// moves on when the player is within reach of the next stop, any direction, height counts
	// only ever looks at the next stop, so walking past a later one does not skip ahead
	public boolean update(double x, double y, double z) {
		if (finished()) {
			return false;
		}
		Waypoint stop = stops.get(index);
		double dx = stop.x() + 0.5 - x;
		double dy = stop.y() + 0.5 - y;
		double dz = stop.z() + 0.5 - z;
		if (dx * dx + dy * dy + dz * dz > reach * reach) {
			return false;
		}
		index++;
		return true;
	}
}
