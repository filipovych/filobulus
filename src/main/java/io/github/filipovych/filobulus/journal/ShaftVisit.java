package io.github.filipovych.filobulus.journal;

import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.Optional;

// one shaft the player was in, enteredAt is wall clock milliseconds and also what tells two visits apart
// code is empty when the type was never read, spent and run are 0 while they are not known
public record ShaftVisit(long enteredAt, String code, long spentMillis, long runMillis) {
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

	public ShaftVisit withCode(String code) {
		return new ShaftVisit(enteredAt, code, spentMillis, runMillis);
	}

	public ShaftVisit withSpent(long spentMillis) {
		return new ShaftVisit(enteredAt, code, Math.max(0, spentMillis), runMillis);
	}

	public ShaftVisit withRun(long runMillis) {
		return new ShaftVisit(enteredAt, code, spentMillis, runMillis);
	}
}
