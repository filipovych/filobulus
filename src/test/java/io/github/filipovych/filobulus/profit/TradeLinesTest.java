package io.github.filipovych.filobulus.profit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

// the lines are written the way skysoft matches them, not captured in game yet
class TradeLinesTest {
	@Test
	void buysAndClaimsBringItemsIn() {
		assertEquals(Optional.of(new TradeLines.Trade("Flawed Ruby Gemstone", 1280, true)),
				TradeLines.read("[Bazaar] Bought 1,280x Flawed Ruby Gemstone for 563,200 coins!"));
		assertEquals(Optional.of(new TradeLines.Trade("Enchanted Mithril", 64, true)),
				TradeLines.read("[Bazaar] Claimed 64x Enchanted Mithril worth 51,200 coins bought for 800 each!"));
		assertEquals(Optional.of(new TradeLines.Trade("Fine Jade Gemstone", 3, true)),
				TradeLines.read("[Bazaar] Cancelled! Refunded 3x Fine Jade Gemstone from cancelling sell offer!"));
	}

	@Test
	void salesTakeItemsOut() {
		assertEquals(Optional.of(new TradeLines.Trade("Glacite", 640, false)),
				TradeLines.read("[Bazaar] Sold 640x Glacite for 2,150.4 coins!"));
		assertEquals(Optional.of(new TradeLines.Trade("Flawed Jasper Gemstone", 80, false)),
				TradeLines.read("[Bazaar] Sell Offer Setup! 80x Flawed Jasper Gemstone for 36,000 coins."));
		assertEquals(Optional.of(new TradeLines.Trade("Umber", 12, false)),
				TradeLines.read("You sold Umber x12 for 96 Coins!"));
	}

	@Test
	void linesThatMoveNothingAreNotTrades() {
		assertTrue(TradeLines.read("[Bazaar] Your Buy Order for 64x Flawed Ruby Gemstone was filled!").isEmpty());
		assertTrue(TradeLines.read("[Auction] Someone bought Drill for 1,000,000 coins! CLICK").isEmpty());
		assertTrue(TradeLines.read(null).isEmpty());
	}
}
