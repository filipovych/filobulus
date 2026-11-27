package io.github.filipovych.filobulus.mining;

// the swap combo: good ping glides fill a style meter, the meter climbs the ranks, idling lets it fall
// everything is fed with client ticks, nothing here knows the game
public final class SwapCombo {
	// one tick of slack on each side of the glide window
	public static final int TOLERANCE = 1;
	// about 3 seconds without a good swap and the meter starts to drain
	public static final int IDLE_TICKS = 60;
	// a full rank drains in 25 ticks once idle
	static final double IDLE_DRAIN = 0.04;
	// where the meter lands after falling a rank, so the next one down still takes a moment
	static final double FALL_TO = 0.7;
	// a fresh rank starts a little filled, so the decay cannot drop it again right away
	static final double RISE_TO = 0.15;

	public enum Rank {
		FROST("Frost", 0x7FB8FF, 0.34, 0),
		CHILL("Chill", 0x5FD7FF, 0.25, 0),
		BRITTLE("Brittle", 0x7FFFE6, 0.18, 0.001),
		AVALANCHE("Avalanche", 0xB59CFF, 0.13, 0.002),
		SHATTER("Shatter", 0xE08CFF, 0.1, 0.003),
		SUB_ZERO("Sub-Zero", 0xFF8CD2, 0.075, 0.004),
		ABSOLUTE_ZERO("ABSOLUTE ZERO", 0xFFF0FA, 0.06, 0.005);

		private final String title;
		private final int color;
		// how much one good swap fills, less the higher the rank
		private final double gain;
		// what the meter loses every tick even while swapping, so the top ranks need a steady pace
		private final double decay;

		Rank(String title, int color, double gain, double decay) {
			this.title = title;
			this.color = color;
			this.gain = gain;
			this.decay = decay;
		}

		public String title() {
			return title;
		}

		// rgb, no alpha
		public int color() {
			return color;
		}

		public boolean top() {
			return this == ABSOLUTE_ZERO;
		}

		Rank up() {
			return top() ? this : values()[ordinal() + 1];
		}

		Rank down() {
			return this == FROST ? this : values()[ordinal() - 1];
		}
	}

	// when the player left a block, measured against the glide window of that block
	public enum Timing {
		// no window known, an instamine or a block we do not know
		NONE,
		EARLY,
		GLIDE,
		LATE
	}

	public enum Event {
		NONE,
		GOOD,
		RANK_UP,
		RANK_DOWN,
		BROKEN,
		ENDED
	}

	private Rank rank = Rank.FROST;
	private double meter;
	private int count;
	private int best;
	private int lastGood;
	private boolean active;
	private boolean newBest;

	public SwapCombo(int best) {
		this.best = Math.max(0, best);
	}

	// elapsed is how long the block was mined, ticks is when the server breaks it and end when it shows gone
	public static Timing timing(int elapsed, int ticks, int end) {
		if (ticks <= 1) {
			return Timing.NONE;
		}
		if (elapsed < ticks - TOLERANCE) {
			return Timing.EARLY;
		}
		return elapsed <= end + TOLERANCE ? Timing.GLIDE : Timing.LATE;
	}

	// a glided block is gone, the swap counts
	public Event good(int tick) {
		active = true;
		lastGood = tick;
		count++;
		if (count > best) {
			best = count;
			newBest = true;
		}
		meter += rank.gain;
		if (meter >= 1) {
			if (rank.top()) {
				meter = 1;
			} else {
				rank = rank.up();
				meter = Math.max(RISE_TO, meter - 1);
				return Event.RANK_UP;
			}
		}
		return Event.GOOD;
	}

	// left a block before its window and it did not break
	public Event broken() {
		if (!active) {
			return Event.NONE;
		}
		clear();
		return Event.BROKEN;
	}

	// ends it without a fuss, for leaving the tunnels or turning it off
	public void stop() {
		clear();
	}

	public Event tick(int tick) {
		if (!active) {
			return Event.NONE;
		}
		meter -= rank.decay;
		if (tick - lastGood > IDLE_TICKS) {
			meter -= IDLE_DRAIN;
		}
		if (meter > 0) {
			return Event.NONE;
		}
		if (rank == Rank.FROST) {
			clear();
			return Event.ENDED;
		}
		rank = rank.down();
		meter = FALL_TO;
		return Event.RANK_DOWN;
	}

	private void clear() {
		active = false;
		rank = Rank.FROST;
		meter = 0;
		count = 0;
	}

	public boolean active() {
		return active;
	}

	public Rank rank() {
		return rank;
	}

	// how full the current rank is, 0 to 1
	public double meter() {
		return Math.max(0, Math.min(1, meter));
	}

	public int count() {
		return count;
	}

	public int best() {
		return best;
	}

	// true once after the best went up, so the store only saves when it has to
	public boolean takeNewBest() {
		boolean was = newBest;
		newBest = false;
		return was;
	}
}
