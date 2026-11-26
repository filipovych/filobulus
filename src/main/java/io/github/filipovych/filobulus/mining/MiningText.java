package io.github.filipovych.filobulus.mining;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// the bits of mining state the game shows as text, all the patterns in one place
// lines come with their colour codes stripped
public final class MiningText {
	// sidebar, "Cold: -23❄"
	private static final Pattern COLD = Pattern.compile("^Cold: -?(\\d{1,3})\\s*❄?");
	// tab list, pity widget, "Glacite Mineshafts: 1,234/2,000"
	private static final Pattern PITY = Pattern.compile("^Glacite Mineshafts: ([\\d,]{1,7})/([\\d,]{1,7})$");
	private static final Pattern PROFILE = Pattern.compile("^Profile ID: ([0-9a-fA-F-]{32,36})$");
	// tab list stats widget, "Mining Speed: ⸕1,234"
	private static final Pattern SPEED = Pattern.compile("^Mining Speed: [^\\d]{0,3}([\\d,]{1,9})$");

	private MiningText() {
	}

	public static OptionalInt cold(List<String> sidebar) {
		return first(sidebar, COLD);
	}

	// how far the pity counter is, only there when the player shows the pity widget
	public record Pity(int now, int full) {
	}

	public static Optional<Pity> pity(List<String> tab) {
		for (String line : tab) {
			Matcher matcher = PITY.matcher(line.strip());
			if (matcher.matches()) {
				return Optional.of(new Pity(number(matcher.group(1)), number(matcher.group(2))));
			}
		}
		return Optional.empty();
	}

	public static Optional<String> profileId(String chat) {
		Matcher matcher = PROFILE.matcher(chat.strip());
		return matcher.matches() ? Optional.of(matcher.group(1).replace("-", "").toLowerCase()) : Optional.empty();
	}

	public static OptionalInt miningSpeed(List<String> tab) {
		return first(tab, SPEED);
	}

	private static OptionalInt first(List<String> lines, Pattern pattern) {
		for (String line : lines) {
			Matcher matcher = pattern.matcher(line.strip());
			if (matcher.find()) {
				return OptionalInt.of(number(matcher.group(1)));
			}
		}
		return OptionalInt.empty();
	}

	private static int number(String text) {
		return Integer.parseInt(text.replace(",", ""));
	}
}
