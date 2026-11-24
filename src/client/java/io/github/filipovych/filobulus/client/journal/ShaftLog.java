package io.github.filipovych.filobulus.client.journal;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.JournalConfig;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.journal.VisitTracker;
import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.List;
import net.minecraft.client.Minecraft;

// runs every client tick after the route tracker and writes every shaft we are in to the journal
public final class ShaftLog {
	private final JournalStore store;
	private final VisitTracker visits = new VisitTracker();

	public ShaftLog(JournalStore store) {
		this.store = store;
	}

	public void tick(Minecraft minecraft) {
		Location location = FilobulusClient.tracker().location();
		boolean inside = JournalConfig.journal && location.area() == Area.MINESHAFT;
		String code = location.shaft().map(ShaftType::code).orElse("");
		write(visits.update(inside, code, minecraft.level, System.currentTimeMillis()));
	}

	// the route of this shaft was finished in that time
	public void ran(long runMillis) {
		write(visits.ran(runMillis));
	}

	// the game is closing, the shaft we are in gets its time now
	public void close() {
		write(visits.update(false, "", null, System.currentTimeMillis()));
	}

	// the shaft we are in, null outside of one
	public ShaftVisit open() {
		return visits.open();
	}

	private void write(List<ShaftVisit> changed) {
		changed.forEach(store::record);
	}
}
