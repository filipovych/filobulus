package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class SplitBestsTest {
	@Test
	void theFirstRunIsTheBest() {
		SplitBests bests = new SplitBests();
		assertTrue(bests.best("jasper", 3).isEmpty());
		assertTrue(bests.offer("jasper", new long[] {0, 4000, 9000}));
		assertArrayEquals(new long[] {0, 4000, 9000}, bests.best("jasper", 3).orElseThrow());
	}

	@Test
	void onlyAFasterRunReplacesIt() {
		SplitBests bests = new SplitBests();
		bests.offer("jasper", new long[] {0, 4000, 9000});
		assertFalse(bests.offer("jasper", new long[] {0, 3000, 9500}));
		assertFalse(bests.offer("jasper", new long[] {0, 3000, 9000}));
		assertTrue(bests.offer("jasper", new long[] {0, 5000, 8000}));
		assertArrayEquals(new long[] {0, 5000, 8000}, bests.best("jasper", 3).orElseThrow());
	}

	@Test
	void aBestForOtherStopsDoesNotCount() {
		SplitBests bests = new SplitBests();
		bests.offer("mine", new long[] {0, 4000, 9000});
		assertTrue(bests.best("mine", 4).isEmpty());
		assertTrue(bests.offer("mine", new long[] {0, 4000, 9000, 20_000}));
		assertEquals(4, bests.best("mine", 4).orElseThrow().length);
	}

	@Test
	void anEmptyRunIsNotKept() {
		SplitBests bests = new SplitBests();
		assertFalse(bests.offer("jasper", new long[0]));
	}

	@Test
	void forgetDropsTheBest() {
		SplitBests bests = new SplitBests();
		bests.offer("mine", new long[] {0, 4000});
		bests.forget("mine");
		assertTrue(bests.best("mine", 2).isEmpty());
	}

	@Test
	void jsonRoundTrip() {
		SplitBests bests = new SplitBests();
		bests.offer("jasper", new long[] {0, 4000, 9000});
		bests.offer("ruby_2", new long[] {0, 1500});
		SplitBests read = new SplitBests();
		read.readJson(JsonParser.parseString(bests.toJson().toString()).getAsJsonObject());
		assertArrayEquals(new long[] {0, 4000, 9000}, read.best("jasper", 3).orElseThrow());
		assertArrayEquals(new long[] {0, 1500}, read.best("ruby_2", 2).orElseThrow());
	}
}
