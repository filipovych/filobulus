package io.github.filipovych.filobulus.corpse;

import java.util.OptionalDouble;

// bazaar prices by product id, empty while a price is not known
public interface PriceSource {
	// what the player gets selling at once
	OptionalDouble sell(String id);

	// what the player pays buying at once
	OptionalDouble buy(String id);
}
