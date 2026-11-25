package io.github.filipovych.filobulus.corpse;

// the corpses of one shaft as letters, one per corpse: L lapis, T tungsten, U umber, V vanguard
// seen is every corpse the shaft has, looted the ones the player opened, both sorted in that order
public record CorpseTally(String seen, String looted) {
	public static final String ORDER = "LTUV";
	public static final CorpseTally NONE = new CorpseTally("", "");

	public CorpseTally {
		seen = sorted(seen);
		looted = sorted(looted);
	}

	public static char letter(CorpseType type) {
		return type.name().charAt(0);
	}

	public static CorpseType type(char letter) {
		return CorpseType.values()[Math.max(0, ORDER.indexOf(letter))];
	}

	public boolean isEmpty() {
		return seen.isEmpty() && looted.isEmpty();
	}

	public int seen(char letter) {
		return count(seen, letter);
	}

	public int looted(char letter) {
		return count(looted, letter);
	}

	// what is known after both were read: a corpse counts once it showed up in either
	// the tab list can be a moment behind the chat, and a looted corpse is one that is there
	public CorpseTally merge(CorpseTally other) {
		StringBuilder allSeen = new StringBuilder();
		StringBuilder allLooted = new StringBuilder();
		for (char letter : ORDER.toCharArray()) {
			int opened = Math.max(looted(letter), other.looted(letter));
			allLooted.repeat(letter, opened);
			allSeen.repeat(letter, Math.max(opened, Math.max(seen(letter), other.seen(letter))));
		}
		return new CorpseTally(allSeen.toString(), allLooted.toString());
	}

	// one more corpse of that kind was opened
	public CorpseTally withLooted(CorpseType type) {
		return merge(new CorpseTally("", looted + letter(type)));
	}

	private static int count(String letters, char letter) {
		int count = 0;
		for (int i = 0; i < letters.length(); i++) {
			if (letters.charAt(i) == letter) {
				count++;
			}
		}
		return count;
	}

	// only our four letters, in our order, so two tallies of the same corpses are equal
	private static String sorted(String letters) {
		StringBuilder sorted = new StringBuilder();
		for (char letter : ORDER.toCharArray()) {
			sorted.repeat(letter, count(letters == null ? "" : letters, letter));
		}
		return sorted.toString();
	}
}
