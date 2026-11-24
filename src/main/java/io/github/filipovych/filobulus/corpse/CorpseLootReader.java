package io.github.filipovych.filobulus.corpse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// reads the loot block the game prints after a corpse is opened, all the chat text we match on is here
// it only ever looks at lines, it never changes or hides one
public final class CorpseLootReader {
	private static final Pattern HEADER = Pattern.compile(" {2}([A-Z]+) CORPSE LOOT! *");
	// the gemstone icons are private use characters and used to be a skull, so anything in front of the name goes
	private static final Pattern ICON = Pattern.compile("^[^A-Za-z0-9+ ]+ +");
	private static final Pattern AMOUNT = Pattern.compile("^(.*\\S) +x(\\d[\\d,]{0,14})$");
	private static final Pattern BONUS = Pattern.compile("^\\+(\\d[\\d,]{0,14}) +(.+)$");
	private static final String INDENT = "    ";
	private static final char BAR = '▬';
	private static final int MIN_BAR = 10;

	private CorpseType type;
	private final List<CorpseLoot.Reward> rewards = new ArrayList<>();

	// one line at a time with the colour codes stripped, gives the loot on the line that closes the block
	public Optional<CorpseLoot> read(String line) {
		if (line == null) {
			return Optional.empty();
		}
		Matcher header = HEADER.matcher(line);
		if (header.matches()) {
			Optional<CorpseType> named = CorpseType.byName(header.group(1));
			if (named.isPresent()) {
				reset();
				type = named.get();
				return Optional.empty();
			}
		}
		if (type == null) {
			return Optional.empty();
		}
		if (isBar(line)) {
			Optional<CorpseLoot> loot = rewards.isEmpty() ? Optional.empty() : Optional.of(new CorpseLoot(type, rewards));
			reset();
			return loot;
		}
		if (line.startsWith(INDENT) && line.length() > INDENT.length() && line.charAt(INDENT.length()) != ' ') {
			reward(line.substring(INDENT.length()).stripTrailing());
		}
		return Optional.empty();
	}

	// forget a half read block, for when the player changes worlds
	public void reset() {
		type = null;
		rewards.clear();
	}

	private void reward(String text) {
		// +1,000 HOTM Experience has its amount in front
		Matcher bonus = BONUS.matcher(text);
		if (bonus.matches()) {
			rewards.add(new CorpseLoot.Reward(bonus.group(2), number(bonus.group(1))));
			return;
		}
		String name = ICON.matcher(text).replaceFirst("");
		long amount = 1;
		Matcher counted = AMOUNT.matcher(name);
		if (counted.matches()) {
			name = counted.group(1);
			amount = number(counted.group(2));
		}
		if (!name.isEmpty()) {
			rewards.add(new CorpseLoot.Reward(name, amount));
		}
	}

	private static long number(String digits) {
		return Long.parseLong(digits.replace(",", ""));
	}

	private static boolean isBar(String line) {
		String text = line.strip();
		if (text.length() < MIN_BAR) {
			return false;
		}
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) != BAR) {
				return false;
			}
		}
		return true;
	}
}
