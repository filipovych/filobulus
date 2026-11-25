package io.github.filipovych.filobulus.profit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.Function;

// totals turned into coins, the most valuable item first
// unpriced is how many items had no price and count as nothing
public record ProfitValue(List<Line> lines, double coins, int unpriced) {
	public record Line(String id, long amount, double coins, boolean priced) {
	}

	public ProfitValue {
		lines = List.copyOf(lines);
	}

	public static ProfitValue of(Map<String, Long> items, Function<String, OptionalDouble> price) {
		List<Line> lines = new ArrayList<>();
		double coins = 0;
		int unpriced = 0;
		for (Map.Entry<String, Long> item : items.entrySet()) {
			OptionalDouble each = price.apply(item.getKey());
			double value = each.orElse(0) * item.getValue();
			lines.add(new Line(item.getKey(), item.getValue(), value, each.isPresent()));
			coins += value;
			if (each.isEmpty()) {
				unpriced++;
			}
		}
		lines.sort(Comparator.comparingDouble(Line::coins).reversed().thenComparing(Line::id));
		return new ProfitValue(lines, coins, unpriced);
	}

	// coins per hour over the time the player was at it, 0 while that is too short to say
	public double perHour(long activeMillis) {
		return activeMillis < 10_000 ? 0 : coins * 3_600_000d / activeMillis;
	}

	// what only the items gained since an earlier moment are worth
	public static ProfitValue since(Map<String, Long> now, Map<String, Long> before, Function<String, OptionalDouble> price) {
		Map<String, Long> gained = new LinkedHashMap<>();
		now.forEach((id, amount) -> {
			long more = amount - before.getOrDefault(id, 0L);
			if (more > 0) {
				gained.put(id, more);
			}
		});
		return of(gained, price);
	}
}
