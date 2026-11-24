package io.github.filipovych.filobulus.alert;

// a will-o'-wisp lasts exactly five minutes from the moment it is placed and the game says nothing when it goes
// so we count along ourselves and say when it is about to go and when it is gone
public final class WispTimer {
	public enum Event {
		NONE,
		SOON,
		GONE
	}

	public static final long LIFETIME = 5 * 60_000;

	private boolean known;
	private long placedAt;
	private boolean toldSoon;
	private boolean toldGone;

	// a new wisp replaces the old one and gets its own five minutes
	public void placed(long now) {
		known = true;
		placedAt = now;
		toldSoon = false;
		toldGone = false;
	}

	// another lobby or a shaft, the wisp did not come along
	public void clear() {
		known = false;
	}

	// false while there is no wisp we know of
	public boolean known() {
		return known;
	}

	// time the wisp still has, 0 once it is gone or when there is none
	public long left(long now) {
		return known ? Math.max(0, placedAt + LIFETIME - now) : 0;
	}

	// what is new since the last call, each thing is said once per wisp
	// lead is how long before the end the first warning comes
	public Event poll(long now, long lead) {
		if (!known) {
			return Event.NONE;
		}
		long left = left(now);
		if (left == 0 && !toldGone) {
			toldGone = true;
			toldSoon = true;
			return Event.GONE;
		}
		if (left > 0 && left <= lead && !toldSoon) {
			toldSoon = true;
			return Event.SOON;
		}
		return Event.NONE;
	}
}
