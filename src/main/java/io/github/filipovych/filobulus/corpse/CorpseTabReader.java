package io.github.filipovych.filobulus.corpse;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// reads the corpse lines of the tab list inside a shaft, one line per corpse: "Tungsten: NOT LOOTED"
// all the tab text we match on for corpses is here
public final class CorpseTabReader {
	private static final Pattern LINE = Pattern.compile("^(Lapis|Tungsten|Umber|Vanguard): (NOT LOOTED|LOOTED)$");
	private static final String LOOTED = "LOOTED";

	private CorpseTabReader() {
	}

	// lines need their colour codes stripped first, empty when the tab list shows no corpses
	public static CorpseTally read(List<String> tabLines) {
		StringBuilder seen = new StringBuilder();
		StringBuilder looted = new StringBuilder();
		for (String line : tabLines) {
			Matcher matcher = LINE.matcher(line.strip());
			if (!matcher.matches()) {
				continue;
			}
			char letter = matcher.group(1).charAt(0);
			seen.append(letter);
			if (matcher.group(2).equals(LOOTED)) {
				looted.append(letter);
			}
		}
		return new CorpseTally(seen.toString(), looted.toString());
	}
}
