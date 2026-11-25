package io.github.filipovych.filobulus.client.journal;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.config.JournalConfig;
import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.HudPanel.Content;
import io.github.filipovych.filobulus.client.hud.HudPanel.Row;
import io.github.filipovych.filobulus.client.hud.HudWidget;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.corpse.CoinText;
import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.journal.JournalText;
import io.github.filipovych.filobulus.journal.ShaftRecap;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// a card that shows for a few seconds after a shaft: how long it took, its corpses and what it paid
// the numbers come from the journal, so a sack message that is a moment late still lands on the card
public final class RecapHud implements HudWidget {
	// argb
	private static final int GOLD = 0xFFFFAA00;
	private static final int GOOD = 0xFF7CE38B;
	private static final int BAD = 0xFFFF6B6B;
	private static final int ROUTE = 0xFF7FD4FF;
	private static final int FAINT = 0xFFA9B6C2;

	// the shaft the card is about, by the moment it was entered, and when the card came up
	private static long visitAt;
	private static long shownAt;

	// called when a shaft is left, and by /filo recap for the last one
	public static void show(long enteredAt) {
		visitAt = enteredAt;
		shownAt = System.currentTimeMillis();
	}

	@Override
	public String name() {
		return "Shaft recap";
	}

	@Override
	public boolean enabled() {
		return JournalConfig.recap;
	}

	@Override
	public void setEnabled(boolean on) {
		JournalConfig.recap = on;
	}

	@Override
	public Optional<HudDrawable> content() {
		if (!JournalConfig.recap || System.currentTimeMillis() - shownAt > JournalConfig.recapSeconds * 1000L) {
			return Optional.empty();
		}
		List<ShaftVisit> journal = FilobulusClient.journal().journal().visits();
		// nearly always the newest one
		for (int i = journal.size() - 1; i >= 0 && journal.get(i).enteredAt() >= visitAt; i--) {
			if (journal.get(i).enteredAt() == visitAt) {
				return Optional.of(card(ShaftRecap.of(journal.get(i), journal)));
			}
		}
		return Optional.empty();
	}

	@Override
	public HudDrawable sample() {
		ShaftVisit visit = new ShaftVisit(1, "JASP_1", 191_000, 178_000, new CorpseTally("LTU", "LT"), 2_400_000, 14_200_000);
		return card(new ShaftRecap(visit, 12_900_000, 8, 9, 14));
	}

	private static Content card(ShaftRecap recap) {
		ShaftVisit visit = recap.visit();
		List<Row> rows = new ArrayList<>();
		rows.add(new Row("Time", visit.spentMillis() > 0 ? JournalText.duration(visit.spentMillis()) : "-"));
		if (visit.runMillis() > 0) {
			rows.add(new Row("Route", JournalText.duration(visit.runMillis()), ROUTE));
		}
		CorpseTally corpses = visit.corpses();
		if (!corpses.isEmpty()) {
			rows.add(new Row("Corpses", corpses.looted().length() + "/" + corpses.seen().length()));
		}
		if (visit.corpseCoins() != 0) {
			rows.add(new Row("Corpse profit", CoinText.signed(visit.corpseCoins()), visit.corpseCoins() < 0 ? BAD : GOOD));
		}
		if (visit.minedCoins() != 0) {
			rows.add(new Row("Mined", CoinText.signed(visit.minedCoins()), GOLD));
		}
		if (visit.corpseCoins() != 0 && visit.minedCoins() != 0) {
			rows.add(new Row("Total", CoinText.signed(visit.coins()), visit.coins() < 0 ? BAD : GOLD));
		}
		if (visit.coins() > 0 && recap.perHour() > 0) {
			rows.add(new Row("Per hour", CoinText.amount(recap.perHour()), GOLD));
		}
		if (recap.hasAverage()) {
			// what this kind of shaft usually pays, to see how this one did
			rows.add(new Row("Your average", CoinText.signed(recap.average()), FAINT));
		}
		rows.add(visit.isJasper()
				? new Row("Dry before", String.valueOf(recap.dry()), ShaftColors.of(ShaftMaterial.JASPER))
				: new Row("Since jasper", String.valueOf(recap.dry())));
		Optional<ShaftType> type = visit.type();
		String title = type.map(ShaftType::displayName).orElse("Mineshaft") + " #" + recap.number();
		return new Content(ShaftIcons.of(type.map(ShaftType::material).orElse(ShaftMaterial.LITTLEFOOT)), title,
				type.map(known -> ShaftColors.of(known.material())).orElse(ShaftColors.GLACITE), rows);
	}

	@Override
	public HudPlacement placement() {
		return new HudPlacement(HudConfig.recapAnchorX, HudConfig.recapX,
				HudConfig.recapAnchorY, HudConfig.recapY, HudConfig.recapScale);
	}

	@Override
	public void place(HudPlacement placement) {
		HudConfig.recapAnchorX = placement.anchorX();
		HudConfig.recapX = placement.x();
		HudConfig.recapAnchorY = placement.anchorY();
		HudConfig.recapY = placement.y();
		HudConfig.recapScale = placement.scale();
	}

	@Override
	public HudPlacement defaultPlacement() {
		// top middle, where the corpse reveal plays: that one only shows inside a shaft and this one only after it
		return new HudPlacement(HudAnchor.CENTRE, 0, HudAnchor.START, 4, 1f);
	}
}
