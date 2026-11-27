package io.github.filipovych.filobulus.route;

// one run of a route: when each stop was reached, counted from when the run began
// in a shaft that is the moment it was entered, so the time on the panel and the best run are one clock
public final class RouteSplits {
	private final long[] times;
	private final long startedAt;
	private int reached;
	private boolean skipped;

	public RouteSplits(int stops, long startedAt) {
		times = new long[stops];
		this.startedAt = startedAt;
	}

	// call when the route moves on to its next stop, now is in milliseconds
	public void reached(long now) {
		if (reached == times.length) {
			return;
		}
		times[reached++] = Math.max(0, now - startedAt);
	}

	// the route moved on by several stops at once, the ones that were passed over get the same time
	public void reached(long now, int stops) {
		skipped |= stops > 1;
		for (int i = 0; i < stops; i++) {
			reached(now);
		}
	}

	// false once a stop was skipped, such a run is not one to keep as a best
	public boolean clean() {
		return !skipped;
	}

	// true once the first stop was reached, before that there is nothing to compare with a best
	public boolean started() {
		return reached > 0;
	}

	public boolean finished() {
		return reached > 0 && reached == times.length;
	}

	// time on the route so far, stands still once it is done
	public long elapsed(long now) {
		return finished() ? times[reached - 1] : Math.max(0, now - startedAt);
	}

	public long[] times() {
		return times.clone();
	}

	// how far behind the best run we were at the last stop, negative means ahead
	public long behind(long[] best) {
		return started() ? times[reached - 1] - best[reached - 1] : 0;
	}
}
