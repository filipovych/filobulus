package io.github.filipovych.filobulus.profit;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// reads the chat lines of the bazaar and of npc shops that move items, all the trade text we match on is here
// lines that only tell you an order was filled or an auction sold move nothing and are not read
public final class TradeLines {
	// bought is true for items coming to the player
	public record Trade(String name, long amount, boolean bought) {
	}

	private record Kind(Pattern pattern, boolean bought) {
	}

	private static final String AMOUNT = "(\\d[\\d,]{0,14})";
	private static final List<Kind> KINDS = List.of(
			new Kind(Pattern.compile("^\\[Bazaar] Bought " + AMOUNT + "x (.+) for [\\d,.]+ coins!$"), true),
			new Kind(Pattern.compile("^\\[Bazaar] Claimed " + AMOUNT + "x (.+) worth [\\d,.]+ coins bought for [\\d,.]+ each!$"), true),
			new Kind(Pattern.compile("^(?:\\[Bazaar] )?Cancelled! Refunded " + AMOUNT + "x (.+) from cancelling sell offer!$",
					Pattern.CASE_INSENSITIVE), true),
			new Kind(Pattern.compile("^\\[Bazaar] Sold " + AMOUNT + "x (.+) for [\\d,.]+ coins!$"), false),
			new Kind(Pattern.compile("^\\[Bazaar] Sell Offer Setup! " + AMOUNT + "x (.+) for [\\d,.]+ coins\\.$"), false));
	// the npc line has the amount behind the name
	private static final Pattern NPC_SALE = Pattern.compile("^You sold (.+) x" + AMOUNT + " for [\\d,.]+ Coins!$");
	private static final Pattern ICON = Pattern.compile("^[^A-Za-z0-9]+");

	private TradeLines() {
	}

	// one chat line with its colour codes stripped
	public static Optional<Trade> read(String line) {
		if (line == null) {
			return Optional.empty();
		}
		String text = line.strip();
		for (Kind kind : KINDS) {
			Matcher matcher = kind.pattern().matcher(text);
			if (matcher.matches()) {
				return trade(matcher.group(2), matcher.group(1), kind.bought());
			}
		}
		Matcher npc = NPC_SALE.matcher(text);
		return npc.matches() ? trade(npc.group(1), npc.group(2), false) : Optional.empty();
	}

	private static Optional<Trade> trade(String name, String digits, boolean bought) {
		String plain = ICON.matcher(name).replaceFirst("").strip();
		long amount = Long.parseLong(digits.replace(",", ""));
		return plain.isEmpty() || amount <= 0 ? Optional.empty() : Optional.of(new Trade(plain, amount, bought));
	}
}
