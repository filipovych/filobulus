package io.github.filipovych.filobulus.profit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// reads what a [Sacks] chat line says went in and out, from the text that shows when the line is hovered
// one item per line there: "  +1,024 Flawed Jasper Gemstone (Gemstones Sack)", all the sack text we match on is here
public final class SackReader {
	private static final String PREFIX = "[Sacks]";
	private static final Pattern LINE = Pattern.compile("^([+-])(\\d[\\d,]{0,14}) (.+?) \\([^()]*\\)$");
	// gemstones can come with a small icon in front of their name
	private static final Pattern ICON = Pattern.compile("^[^A-Za-z0-9]+");

	// amount is negative for what was taken out
	public record Change(String name, long amount) {
	}

	private SackReader() {
	}

	public static boolean isSackLine(String chatLine) {
		return chatLine != null && chatLine.strip().startsWith(PREFIX);
	}

	// the hover text with its colour codes stripped, lines that are not an item are skipped
	public static List<Change> read(String hover) {
		List<Change> changes = new ArrayList<>();
		if (hover == null) {
			return changes;
		}
		for (String line : hover.split("\n")) {
			Matcher matcher = LINE.matcher(line.strip());
			if (!matcher.matches()) {
				continue;
			}
			long amount = Long.parseLong(matcher.group(2).replace(",", ""));
			String name = ICON.matcher(matcher.group(3)).replaceFirst("").strip();
			if (amount > 0 && !name.isEmpty()) {
				changes.add(new Change(name, matcher.group(1).equals("-") ? -amount : amount));
			}
		}
		return changes;
	}
}
