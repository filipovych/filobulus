package io.github.filipovych.filobulus.client.hud;

import java.util.function.Supplier;

// what a panel shows is worked out at most once per client tick, the frames in between draw the same thing
public final class TickCache<T> {
	private long tick = -1;
	private T value;

	public T get(Supplier<T> build) {
		if (Hud.ticks() != tick) {
			tick = Hud.ticks();
			value = build.get();
		}
		return value;
	}
}
