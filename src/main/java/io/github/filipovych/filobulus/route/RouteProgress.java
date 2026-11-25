package io.github.filipovych.filobulus.route;

import java.util.List;
import java.util.Optional;

// walks a route one stop at a time, the line points at next()
public final class RouteProgress {
	private final List<Waypoint> stops;
	private int index;

	public RouteProgress(List<Waypoint> stops) {
		this(stops, 0);
	}

	// start is the stop the line points at first
	public RouteProgress(List<Waypoint> stops, int start) {
		this.stops = List.copyOf(stops);
		this.index = Math.max(0, Math.min(start, stops.size()));
	}

	// the stop closest to a point, for picking a loop up where the player stands
	public static int nearest(List<Waypoint> stops, double x, double y, double z) {
		int best = 0;
		double bestDistance = Double.MAX_VALUE;
		for (int i = 0; i < stops.size(); i++) {
			Waypoint stop = stops.get(i);
			double dx = stop.x() + 0.5 - x;
			double dy = stop.y() + 0.5 - y;
			double dz = stop.z() + 0.5 - z;
			double distance = dx * dx + dy * dy + dz * dz;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = i;
			}
		}
		return best;
	}

	// empty once the last stop was reached, the route is done then
	public Optional<Waypoint> next() {
		return finished() ? Optional.empty() : Optional.of(stops.get(index));
	}

	// the stops still to visit, next one first
	public List<Waypoint> remaining() {
		return stops.subList(Math.min(index, stops.size()), stops.size());
	}

	// how many stops are behind us
	public int reached() {
		return index;
	}

	public int total() {
		return stops.size();
	}

	public boolean finished() {
		return index >= stops.size();
	}

	// moves on when the player is within reach of the next stop, any direction, height counts
	// only ever looks at the next stop, so walking past a later one does not skip ahead
	// reach comes in every time so changing the setting works mid route
	public boolean update(double x, double y, double z, double reach) {
		return update(x, y, z, reach, 0);
	}

	// ahead is how many stops past the next one count as well, reaching one of those skips the stops before it
	public boolean update(double x, double y, double z, double reach, int ahead) {
		for (int i = index; i < stops.size() && i <= index + ahead; i++) {
			Waypoint stop = stops.get(i);
			double dx = stop.x() + 0.5 - x;
			double dy = stop.y() + 0.5 - y;
			double dz = stop.z() + 0.5 - z;
			if (dx * dx + dy * dy + dz * dz <= reach * reach) {
				index = i + 1;
				return true;
			}
		}
		return false;
	}
}
