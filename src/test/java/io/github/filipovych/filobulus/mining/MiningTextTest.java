package io.github.filipovych.filobulus.mining;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class MiningTextTest {
	@Test
	void readsTheSidebarAndTheTabList() {
		assertEquals(23, MiningText.cold(List.of("Glacite Tunnels", "Cold: -23❄")).getAsInt());
		assertEquals(1234, MiningText.pity(List.of(" Glacite Mineshafts: 1,234/2,000")).orElseThrow().now());
		assertEquals(3410, MiningText.miningSpeed(List.of(" Mining Speed: ⸕3,410")).getAsInt());
	}

	@Test
	void readsChat() {
		assertEquals("0123456789abcdef0123456789abcdef",
				MiningText.profileId("Profile ID: 01234567-89ab-cdef-0123-456789abcdef").orElseThrow());
	}

	@Test
	void coldRisesTowardsAHundred() {
		ColdRate rate = new ColdRate();
		rate.read(10, 0);
		rate.read(20, 20_000);
		assertEquals(0.5, rate.perSecond(), 0.001);
		assertEquals(160_000, rate.untilDeadly(20_000));
		rate.read(0, 30_000);
		assertEquals(0, rate.perSecond());
	}
}
