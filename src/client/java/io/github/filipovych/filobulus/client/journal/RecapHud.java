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

// a small card that shows for a few seconds after a shaft when auto recap is on: what it paid, the rate, the time and the corpses
// the numbers come from the journal, so a sack message that is a moment late still lands on the card
public final class RecapHud implements HudWidget {
	// argb
	private static final int GOLD = 0xFFFFAA00;
	private static final int GOOD = 0xFF7CE38B;
	private static final int BAD = 0xFFFF6B6B;

	// the shaft the card is about, by the moment it was entered, and when the card came up
	private static long visitAt;
	private static long shownAt;

	// called when a shaft is left
	public static void show(long enteredAt) {
		visitAt = enteredAt;
		shownAt = System.currentTimeMillis();
	}

	@Override
	public String name() {
		return "Auto recap";
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

	// kept small, the full recap is /filo recap
	private static Content card(ShaftRecap recap) {
		ShaftVisit visit = recap.visit();
		List<Row> rows = new ArrayList<>();
		if (visit.coins() != 0) {
			rows.add(new Row("Profit", CoinText.signed(visit.coins()), visit.coins() < 0 ? BAD : GOLD));
		}
		if (visit.coins() > 0 && recap.perHour() > 0) {
			rows.add(new Row("Per hour", CoinText.amount(recap.perHour()), GOLD));
		}
		rows.add(new Row("Time", visit.spentMillis() > 0 ? JournalText.duration(visit.spentMillis()) : "-"));
		CorpseTally corpses = visit.corpses();
		if (!corpses.isEmpty()) {
			rows.add(new Row("Corpses", corpses.looted().length() + "/" + corpses.seen().length()));
		}
		if (recap.hasAverage()) {
			long better = visit.coins() - recap.average();
			rows.add(new Row("Vs average", CoinText.signed(better), better < 0 ? BAD : GOOD));
		}
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
