package io.github.filipovych.filobulus.journal;

import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.Optional;

// one shaft the player was in, enteredAt is wall clock milliseconds and also what tells two visits apart
// code is empty when the type was never read, spent and run are 0 while they are not known
// corpses are the ones the shaft had and the ones that were opened, corpseCoins what their loot was worth with the keys taken off
// minedCoins is what was mined and picked up inside, as the profit tracker counted it
// joined is a shaft someone else found, the player came in through their portal
public record ShaftVisit(long enteredAt, String code, long spentMillis, long runMillis, CorpseTally corpses,
		long corpseCoins, long minedCoins, boolean joined) {
	public ShaftVisit(long enteredAt, String code, long spentMillis, long runMillis, CorpseTally corpses,
			long corpseCoins, long minedCoins) {
		this(enteredAt, code, spentMillis, runMillis, corpses, corpseCoins, minedCoins, false);
	}

	public ShaftVisit(long enteredAt, String code, long spentMillis, long runMillis) {
		this(enteredAt, code, spentMillis, runMillis, CorpseTally.NONE, 0, 0);
	}

	public static ShaftVisit entered(long at, String code) {
		return new ShaftVisit(at, code, 0, 0);
	}

	public Optional<ShaftType> type() {
		return ShaftType.parse(code);
	}

	// what the stats group by: the type code, or CRYSTAL for every crystal shaft
	public String key() {
		return type().map(ShaftType::routeKey).orElse("");
	}

	// the shaft everyone is after, a jasper crystal shaft is one too and ends a dry streak
	public boolean isJasper() {
		return type().filter(type -> type.material() == ShaftMaterial.JASPER).isPresent();
	}

	// everything the shaft paid that we know of
	public long coins() {
		return corpseCoins + minedCoins;
	}

	public ShaftVisit withCode(String code) {
		return new ShaftVisit(enteredAt, code, spentMillis, runMillis, corpses, corpseCoins, minedCoins, joined);
	}

	public ShaftVisit withSpent(long spentMillis) {
		return new ShaftVisit(enteredAt, code, Math.max(0, spentMillis), runMillis, corpses, corpseCoins, minedCoins, joined);
	}

	public ShaftVisit withRun(long runMillis) {
		return new ShaftVisit(enteredAt, code, spentMillis, runMillis, corpses, corpseCoins, minedCoins, joined);
	}

	public ShaftVisit withCorpses(CorpseTally corpses, long corpseCoins) {
		return new ShaftVisit(enteredAt, code, spentMillis, runMillis, corpses, corpseCoins, minedCoins, joined);
	}

	public ShaftVisit withMined(long minedCoins) {
		return new ShaftVisit(enteredAt, code, spentMillis, runMillis, corpses, corpseCoins, minedCoins, joined);
	}

	public ShaftVisit withJoined(boolean joined) {
		return new ShaftVisit(enteredAt, code, spentMillis, runMillis, corpses, corpseCoins, minedCoins, joined);
	}
}
