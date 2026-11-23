package io.github.filipovych.filobulus.route;

// one run of a route: when each stop was reached, counted from the first stop
// the clock starts at the first stop and not on entering the shaft, so how fast the shaft was noticed does not matter
public final class RouteSplits {
	private final long[] times;
	private int reached;
	private long startedAt;

	public RouteSplits(int stops) {
		times = new long[stops];
	}

	// call when the route moves on to its next stop, now is in milliseconds
	public void reached(long now) {
		if (reached == times.length) {
			return;
		}
		if (reached == 0) {
			startedAt = now;
		}
		times[reached++] = Math.max(0, now - startedAt);
	}

	public boolean started() {
		return reached > 0;
	}

	public boolean finished() {
		return reached > 0 && reached == times.length;
	}

	// time on the route so far, stands still once it is done
	public long elapsed(long now) {
		if (!started()) {
			return 0;
		}
		return finished() ? times[reached - 1] : Math.max(0, now - startedAt);
	}

	public long[] times() {
		return times.clone();
	}

	// how far behind the best run we were at the last stop, negative means ahead
	public long behind(long[] best) {
		return started() ? times[reached - 1] - best[reached - 1] : 0;
	}

	// time still to go if the rest goes like the best run did
	public long left(long[] best, long now) {
		return Math.max(0, best[best.length - 1] + behind(best) - elapsed(now));
	}
}
