package io.github.filipovych.filobulus.mining;

// how many ticks a skyblock block takes at a mining speed, the way the wiki and skyhanni work it out
// ticks = round(30 * strength / speed), never under 4 ticks, and 1 tick once the speed is over 60 times the strength
// plain blocks like hard stone instamine at 30 times the strength
public final class Breakpoints {
	public static final int SOFT_CAP = 4;

	private Breakpoints() {
	}

	public static int ticks(int strength, int speed, boolean plain) {
		if (strength <= 0 || speed <= 0) {
			return 0;
		}
		if (speed > instamine(strength, plain) - 1) {
			return 1;
		}
		return Math.max(SOFT_CAP, (int) Math.round(30d * strength / speed));
	}

	// the smallest speed that breaks the block in that many ticks
	public static int speedFor(int strength, int ticks, boolean plain) {
		if (ticks <= 1) {
			return instamine(strength, plain);
		}
		// round gives ticks while 30 * strength / speed is under ticks + 0.5, the cap is the same line at 4
		return (int) Math.floor(30d * strength / (Math.max(SOFT_CAP, ticks) + 0.5)) + 1;
	}

	// the next tick count down from now and the speed it needs, ticks 0 once nothing is left to reach
	public record Next(int ticks, int speed) {
	}

	public static Next next(int strength, int speed, boolean plain) {
		int now = ticks(strength, speed, plain);
		if (now <= 1) {
			return new Next(0, 0);
		}
		int target = now <= SOFT_CAP ? 1 : now - 1;
		return new Next(target, speedFor(strength, target, plain));
	}

	private static int instamine(int strength, boolean plain) {
		return (plain ? 30 : 60) * strength + 1;
	}

	// ping gliding: the server breaks the block that many ticks after it got our start, so a swap sent from
	// tick ticks lands after the break, the block only shows gone about a ping later and that is the end of the window
	public static int glideEnd(int ticks, int pingMillis) {
		return ticks + pingTicks(pingMillis);
	}

	public static int pingTicks(int pingMillis) {
		return (int) Math.ceil(Math.max(0, pingMillis) / 50d);
	}
}
