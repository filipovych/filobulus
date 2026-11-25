package io.github.filipovych.filobulus.corpse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

// the corpses of a shaft, from the tab list and from what was opened
class CorpseTallyTest {
	@Test
	void theTabListGivesEveryCorpseAndTheOnesOpened() {
		CorpseTally tally = CorpseTabReader.read(List.of(
				"Area: Mineshaft", "Frozen Corpses:", " Lapis: LOOTED", " Tungsten: NOT LOOTED", " Lapis: NOT LOOTED",
				" Vanguard: LOOTED", "Umber: NOT LOOTED"));
		assertEquals("LLTUV", tally.seen());
		assertEquals("LV", tally.looted());
		assertEquals(2, tally.seen('L'));
		assertEquals(1, tally.looted('L'));
		assertEquals(0, tally.looted('T'));
	}

	@Test
	void otherLinesAreNoCorpses() {
		assertTrue(CorpseTabReader.read(List.of("Area: Mineshaft", "Lapis: 12", "Tungsten: LOOTED soon", "Jasper: LOOTED")).isEmpty());
		assertTrue(CorpseTabReader.read(List.of()).isEmpty());
	}

	@Test
	void lettersAreKeptInOneOrderAndOddOnesDropped() {
		assertEquals(new CorpseTally("LTUV", "LV"), new CorpseTally("VUTLx", "VL?"));
		assertEquals(CorpseTally.NONE, new CorpseTally(null, null));
	}

	@Test
	void anOpenedCorpseCountsEvenWhenTheTabListNeverShowedIt() {
		CorpseTally tally = CorpseTally.NONE.withLooted(CorpseType.TUNGSTEN);
		assertEquals(new CorpseTally("T", "T"), tally);
		assertEquals(CorpseType.TUNGSTEN, CorpseTally.type('T'));
		assertEquals('V', CorpseTally.letter(CorpseType.VANGUARD));
	}

	@Test
	void theTabListAndTheChatAddUpWithoutCountingTwice() {
		CorpseTally fromTab = new CorpseTally("LLTU", "L");
		CorpseTally afterChat = fromTab.withLooted(CorpseType.TUNGSTEN);
		assertEquals(new CorpseTally("LLTU", "LT"), afterChat);
		// the tab list catches up a moment later and says the same
		assertEquals(afterChat, afterChat.merge(new CorpseTally("LLTU", "LT")));
		// or is still behind
		assertEquals(afterChat, afterChat.merge(fromTab));
	}
}
