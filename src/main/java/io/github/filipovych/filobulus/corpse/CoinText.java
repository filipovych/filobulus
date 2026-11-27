package io.github.filipovych.filobulus.corpse;

import java.util.Locale;

// how coins are written on the hud
public final class CoinText {
	private CoinText() {
	}

	// 850, 1.5k, 850k, 1.2M, 12.4M, 680M, 1.25B
	public static String amount(double coins) {
		double abs = Math.abs(coins);
		if (abs < 999.5) {
			return String.valueOf(Math.round(abs));
		}
		if (abs < 999_500) {
			return String.format(Locale.ROOT, abs < 9_950 ? "%.1fk" : "%.0fk", abs / 1e3);
		}
		if (abs < 999_500_000) {
			return String.format(Locale.ROOT, abs < 99_950_000 ? "%.1fM" : "%.0fM", abs / 1e6);
		}
		return String.format(Locale.ROOT, "%.2fB", abs / 1e9);
	}

	// 1.2M or -850k, for a number that can go below zero
	public static String plain(double coins) {
		return (coins <= -0.5 ? "-" : "") + amount(coins);
	}

	// +1.2M or -850k
	public static String signed(double coins) {
		return (coins <= -0.5 ? "-" : "+") + amount(coins);
	}
}
