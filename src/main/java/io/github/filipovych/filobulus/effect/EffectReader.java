package io.github.filipovych.filobulus.effect;

import java.util.List;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// reads the cold resistance potion from the text the game shows, all the effect text we match on is here
// the tab list widget "Active Effects" lists each effect with its time left, the /effects menu has it in the lore of an item
// some players show their effects on the sidebar too, it is read the same way
public final class EffectReader {
	private static final Pattern WIDGET = Pattern.compile("\\s*Active Effects: \\((\\d+)\\)\\s*");
	private static final Pattern COLD = Pattern.compile(".*Cold Resistance (?<tier>[IVX]+)\\b[: ]*(?<time>[dhms0-9 ]*).*");
	private static final Pattern REMAINING = Pattern.compile(".*Remaining: (?<time>[\\d:]+).*");
	private static final Pattern PART = Pattern.compile("(\\d+)([dhms])");

	// widget is true when the tab list has the effects widget, so a missing cold resistance line means there is none
	// millis is the time it has left, empty when no line was found, 0 when one was found without a time
	public record Read(boolean widget, OptionalLong millis, int tier) {
	}

	private EffectReader() {
	}

	// lines need their colour codes stripped first
	public static Read read(List<String> tabLines, List<String> sidebarLines) {
		boolean widget = false;
		for (String line : tabLines) {
			widget |= WIDGET.matcher(line).matches();
		}
		for (List<String> lines : List.of(tabLines, sidebarLines)) {
			for (String line : lines) {
				Matcher cold = COLD.matcher(line);
				if (cold.matches()) {
					return new Read(widget, OptionalLong.of(duration(cold.group("time"))), tier(cold.group("tier")));
				}
			}
		}
		return new Read(widget, OptionalLong.empty(), 0);
	}

	// the lore of an item of the /effects menu, empty when it is not the cold resistance potion
	public static OptionalLong menuItem(String name, List<String> lore) {
		if (!COLD.matcher(name).matches()) {
			return OptionalLong.empty();
		}
		for (String line : lore) {
			Matcher remaining = REMAINING.matcher(line);
			if (remaining.matches()) {
				return OptionalLong.of(clock(remaining.group("time")));
			}
		}
		return OptionalLong.of(0);
	}

	public static boolean isEffectsMenu(String title) {
		return title.matches("(?:\\(\\d+/\\d+\\) )?Active Effects");
	}

	// 2m 30s, 1h 5m, 45s
	static long duration(String text) {
		long millis = 0;
		Matcher part = PART.matcher(text.replace(" ", ""));
		while (part.find()) {
			if (part.group(1).length() > 9) {
				return 0;
			}
			long amount = Long.parseLong(part.group(1));
			millis += amount * switch (part.group(2)) {
				case "d" -> 86_400_000L;
				case "h" -> 3_600_000L;
				case "m" -> 60_000L;
				default -> 1000L;
			};
		}
		return millis;
	}

	// 12:34 or 1:02:03
	static long clock(String text) {
		long seconds = 0;
		for (String part : text.split(":")) {
			if (part.isEmpty() || part.length() > 9) {
				return 0;
			}
			seconds = seconds * 60 + Long.parseLong(part);
		}
		return seconds * 1000;
	}

	private static int tier(String roman) {
		return switch (roman) {
			case "I" -> 1;
			case "II" -> 2;
			case "III" -> 3;
			case "IV" -> 4;
			case "V" -> 5;
			default -> 0;
		};
	}
}
