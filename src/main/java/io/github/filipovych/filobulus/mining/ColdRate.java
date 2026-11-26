package io.github.filipovych.filobulus.mining;

// how fast the cold goes up, from what the sidebar said over time, and when it reaches 100
// only rises count, a drop means a campfire or a new shaft and starts over
public final class ColdRate {
	public static final int DEADLY = 100;
	// below this the rate says too little
	private static final long MIN_MILLIS = 20_000;

	private int first = -1;
	private long firstAt;
	private int last = -1;
	private long lastAt;

	public void read(int cold, long now) {
		if (last < 0 || cold < last) {
			first = cold;
			firstAt = now;
		}
		last = cold;
		lastAt = now;
	}

	public void clear() {
		first = -1;
		last = -1;
	}

	public int cold() {
		return Math.max(0, last);
	}

	public boolean known() {
		return last >= 0;
	}

	// cold per second, 0 while it is too early to say
	public double perSecond() {
		long spent = lastAt - firstAt;
		return last < 0 || spent < MIN_MILLIS || last <= first ? 0 : (last - first) * 1000d / spent;
	}

	// milliseconds until 100 at this rate, -1 while it can not be said
	public long untilDeadly(long now) {
		double rate = perSecond();
		if (rate <= 0) {
			return -1;
		}
		long left = Math.round((DEADLY - last) / rate * 1000) - (now - lastAt);
		return Math.max(0, left);
	}
}
