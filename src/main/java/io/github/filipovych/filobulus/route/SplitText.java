package io.github.filipovych.filobulus.route;

import java.util.Locale;

// how times are written on the hud and in chat
public final class SplitText {
	private SplitText() {
	}

	// 2:31
	public static String clock(long millis) {
		long seconds = Math.max(0, millis) / 1000;
		return seconds / 60 + ":" + String.format("%02d", seconds % 60);
	}

	// -3.2s or +1:05, minus is faster than the best
	public static String delta(long millis) {
		String sign = millis < 0 ? "-" : "+";
		long tenths = Math.abs(millis) / 100;
		if (tenths < 600) {
			return sign + String.format(Locale.ROOT, "%d.%ds", tenths / 10, tenths % 10);
		}
		return sign + clock(Math.abs(millis));
	}
}
