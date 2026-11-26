package io.github.filipovych.filobulus.journal;

import io.github.filipovych.filobulus.corpse.CorpseType;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;

// what a warp to someone else's vanguard shaft cost, taken off the corpse profit once per shaft
public final class WarpFee {
	// the shaft that already paid, by when it was entered
	private long paidFor;

	// a corpse was looted in shaft, gives what comes off its coins, 0 when nothing does
	public long take(ShaftVisit shaft, CorpseType looted, long cost) {
		if (shaft == null || !shaft.joined() || cost <= 0 || shaft.enteredAt() == paidFor) {
			return 0;
		}
		// the type code can be missing, a vanguard corpse only lies in a vanguard shaft anyway
		boolean vanguard = looted == CorpseType.VANGUARD
				|| shaft.type().filter(type -> type.material() == ShaftMaterial.VANGUARD).isPresent();
		if (!vanguard) {
			return 0;
		}
		paidFor = shaft.enteredAt();
		return cost;
	}
}
