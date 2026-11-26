package io.github.filipovych.filobulus.client.profit;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.config.TrackerConfig;
import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.HudPanel;
import io.github.filipovych.filobulus.client.hud.HudPanel.Content;
import io.github.filipovych.filobulus.client.hud.HudPanel.Row;
import io.github.filipovych.filobulus.client.hud.HudWidget;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.corpse.CoinText;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.journal.JournalText;
import io.github.filipovych.filobulus.profit.ProfitValue;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// the profit tracker on screen: the items gained with what each sells for, then the total, the rate and the time
public final class ProfitHud implements HudWidget {
	// argb
	private static final int GOLD = 0xFFFFAA00;
	private static final int TIME = 0xFF55FFFF;
	private static final int UNPRICED = 0xFF6B7782;
	private static final int LOSS = 0xFFFF6B6B;

	@Override
	public String name() {
		return "Profit tracker";
	}

	@Override
	public boolean enabled() {
		return TrackerConfig.profit;
	}

	@Override
	public void setEnabled(boolean on) {
		TrackerConfig.profit = on;
	}

	@Override
	public Optional<HudDrawable> content() {
		ProfitWatch watch = FilobulusClient.profit();
		if (!watch.active() || TrackerConfig.hideEmpty && watch.totals().isEmpty()) {
			return Optional.empty();
		}
		ProfitValue value = watch.value();
		List<Row> rows = new ArrayList<>();
		boolean everything = TrackerConfig.items == TrackerConfig.Items.EVERYTHING;
		// with everything counted the things nobody buys are left out of the list
		List<ProfitValue.Line> lines = value.lines().stream().filter(line -> line.priced() || !everything).toList();
		for (int i = 0; i < lines.size() && i < TrackerConfig.lines; i++) {
			ProfitValue.Line line = lines.get(i);
			rows.add(new Row(ProfitIcons.of(line.id()), brief(watch.name(line.id())), amount(line.amount()),
					line.priced() ? CoinText.amount(line.coins()) : "?", line.priced() ? GOLD : UNPRICED));
		}
		if (lines.size() > TrackerConfig.lines && TrackerConfig.lines > 0) {
			rows.add(new Row("", (lines.size() - TrackerConfig.lines) + " more", UNPRICED));
		}
		if (watch.totals().corpses() > 0) {
			double corpses = watch.totals().corpseCoins();
			rows.add(new Row(ShaftIcons.of(ShaftMaterial.TUNGSTEN), "Corpses", amount(watch.totals().corpses()),
					CoinText.signed(corpses), corpses < 0 ? LOSS : GOLD));
		}
		long active = watch.activeMillis();
		rows.add(HudPanel.SPACER);
		rows.addAll(summary(value.coins(), value.perHour(active), active));
		return Optional.of(panel(rows));
	}

	@Override
	public HudDrawable sample() {
		List<Row> rows = new ArrayList<>();
		rows.add(new Row(ShaftIcons.of(ShaftMaterial.JASPER), "Flawed Jasper", "x2,304", "9.4M", GOLD));
		rows.add(new Row(ShaftIcons.of(ShaftMaterial.JASPER), "Fine Jasper", "x12", "3.9M", GOLD));
		rows.add(new Row(ShaftIcons.of(ShaftMaterial.LITTLEFOOT), "Glacite", "x4,512", "310k", GOLD));
		rows.add(new Row(ShaftIcons.of(ShaftMaterial.TUNGSTEN), "Corpses", "x3", "+2.4M", GOLD));
		rows.add(HudPanel.SPACER);
		rows.addAll(summary(13_610_000, 41_200_000, 19 * 60_000 + 22_000));
		return panel(rows);
	}

	private static List<Row> summary(double coins, double perHour, long active) {
		return List.of(
				new Row("Profit", CoinText.amount(coins), GOLD),
				// the first minute says too little to give a rate
				new Row("Per hour", perHour == 0 ? "..." : CoinText.amount(perHour), GOLD),
				new Row("Time", JournalText.duration(active), TIME));
	}

	private static Content panel(List<Row> rows) {
		boolean mining = TrackerConfig.items == TrackerConfig.Items.MINING;
		String period = TrackerConfig.period == TrackerConfig.Period.ALL_TIME ? ", all time" : "";
		return new Content(ProfitIcons.coins(), (mining ? "Mining profit" : "Profit") + period, GOLD, rows);
	}

	// the sacks say Flawed Jasper Gemstone, on a narrow panel the last word is only in the way
	private static String brief(String name) {
		return name.endsWith(" Gemstone") ? name.substring(0, name.length() - " Gemstone".length()) : name;
	}

	private static String amount(long amount) {
		return "x" + String.format(Locale.ROOT, "%,d", amount);
	}

	@Override
	public HudPlacement placement() {
		return new HudPlacement(HudConfig.profitAnchorX, HudConfig.profitX,
				HudConfig.profitAnchorY, HudConfig.profitY, HudConfig.profitScale);
	}

	@Override
	public void place(HudPlacement placement) {
		HudConfig.profitAnchorX = placement.anchorX();
		HudConfig.profitX = placement.x();
		HudConfig.profitAnchorY = placement.anchorY();
		HudConfig.profitY = placement.y();
		HudConfig.profitScale = placement.scale();
	}

	@Override
	public HudPlacement defaultPlacement() {
		// right edge, a bit above the middle, clear of the sidebar which sits lower
		return new HudPlacement(HudAnchor.END, -4, HudAnchor.START, 40, 1f);
	}
}
