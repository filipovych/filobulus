package io.github.filipovych.filobulus.client.journal;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.JournalConfig;
import io.github.filipovych.filobulus.client.detect.GameText;
import io.github.filipovych.filobulus.corpse.CorpseTabReader;
import io.github.filipovych.filobulus.corpse.CorpseType;
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
	private static final int CORPSE_INTERVAL = 20;

	private final VisitTracker visits = new VisitTracker();
	private int ticks;

	public ShaftLog(JournalStore store) {
		this.store = store;
	}

	public void tick(Minecraft minecraft) {
		Location location = FilobulusClient.tracker().location();
		boolean inside = JournalConfig.journal && location.area() == Area.MINESHAFT;
		String code = location.shaft().map(ShaftType::code).orElse("");
		ShaftVisit before = visits.open();
		write(visits.update(inside, code, minecraft.level, System.currentTimeMillis()));
		if (before != null && visits.open() == null) {
			// we are out, the card says how it went
			RecapHud.show(before.enteredAt());
		}
		// the corpse lines of the tab list say how many the shaft has and which ones we opened
		if (inside && ticks++ % CORPSE_INTERVAL == 0) {
			write(visits.corpses(CorpseTabReader.read(GameText.tabLines(minecraft))));
		}
	}

	// a corpse was opened in the shaft we are in, coins is its loot with the key taken off
	public void looted(CorpseType type, long coins) {
		write(visits.looted(type, coins));
	}

	// what was mined in the shaft we are in is worth this much so far
	public void mined(long coins) {
		write(visits.mined(coins, System.currentTimeMillis()));
	}

	// the shaft we left last, null before the first one
	public ShaftVisit last() {
		return visits.last();
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
