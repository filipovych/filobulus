package io.github.filipovych.filobulus.journal;

// tells a shaft we found from one we joined, only the finder gets the portal line in chat
// so a shaft entered without that line shortly before is someone else's, a party shaft or a paid warp
public final class ShaftJoin {
	// the portal stays 30 seconds, this leaves room for a slow walk and a lag spike
	public static final long PORTAL_MILLIS = 120_000;

	private long portalAt;

	// the portal line was in chat
	public void portal(long now) {
		portalAt = now;
	}

	// after a tick of the visit tracker, true when open is a new shaft that someone else found
	// a visit taken up again after a disconnect is not new, it keeps what it was
	public boolean entered(ShaftVisit before, ShaftVisit open, long now) {
		boolean fresh = open != null && open.enteredAt() == now && (before == null || before.enteredAt() != now);
		return fresh && joined(now);
	}

	// a new shaft was entered, one portal line only ever counts for one shaft
	public boolean joined(long now) {
		boolean found = portalAt > 0 && now >= portalAt && now - portalAt <= PORTAL_MILLIS;
		portalAt = 0;
		return !found;
	}
}
