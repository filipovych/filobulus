package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.corpse.CoinText;
import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.corpse.CorpseType;
import io.github.filipovych.filobulus.journal.JournalText;
import io.github.filipovych.filobulus.journal.ShaftRecap;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// /filo recap: one shaft on its own, what it paid and how it went, the arrows step through the ones before it
public final class RecapScreen extends FlatScreen {
	private static final int WIDTH = 320;
	private static final int PAD = 10;
	private static final int GAP = 4;
	private static final int TILE_HEIGHT = 32;
	private static final int LINE = 12;
	private static final float BIG = 1.5f;
	private static final int TILE = 0x18FFFFFF;
	private static final int TRACK = 0x16FFFFFF;
	private static final int COINS = 0xFFFFAA00;
	private static final int ROUTE = 0xFF7FD4FF;
	private static final int[] CORPSE_COLORS = {0xFF4A7BFF, 0xFF8C9AA6, 0xFFC8743A, 0xFFFFD75E};
	private static final int DOT = 6;

	private record Tile(String value, int color, String label) {
	}

	private record Line(String label, String value, int color) {
	}

	private final Screen parent;
	private final ZoneId zone = ZoneId.systemDefault();
	// which visit of the journal is shown, counted from the oldest
	private int index;

	// enteredAt picks the shaft, 0 for the newest one
	public RecapScreen(Screen parent, long enteredAt) {
		super(Component.literal("Filobulus recap"));
		this.parent = parent;
		List<ShaftVisit> visits = visits();
		index = visits.size() - 1;
		for (int i = 0; i < visits.size(); i++) {
			if (visits.get(i).enteredAt() == enteredAt) {
				index = i;
			}
		}
	}

	private static List<ShaftVisit> visits() {
		return FilobulusClient.journal().journal().visits();
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		List<ShaftVisit> visits = visits();
		index = Math.max(0, Math.min(index, visits.size() - 1));
		int panelWidth = Math.min(width - 16, WIDTH);
		int panelHeight = Math.min(height - 16, visits.isEmpty() ? 90 : contentHeight(visits.get(index), visits));
		int left = (width - panelWidth) / 2;
		int top = (height - panelHeight) / 2;
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		panel(graphics, left, top, panelWidth, panelHeight);
		if (visits.isEmpty()) {
			graphics.centeredText(font, "No shafts yet.", width / 2, top + panelHeight / 2 - 10, MUTED);
			graphics.centeredText(font, "Leave a mineshaft and its recap shows here.", width / 2, top + panelHeight / 2 + 2, FAINT);
			footer(graphics, right, bottom, mouseX, mouseY);
			return;
		}
		ShaftVisit visit = visits.get(index);
		ShaftRecap recap = ShaftRecap.of(visit, visits);
		Optional<ShaftType> type = visit.type();

		// the shaft, which one of its kind and when
		graphics.item(ShaftIcons.of(type.map(ShaftType::material).orElse(ShaftMaterial.LITTLEFOOT)), left + PAD - 2, top + 7);
		int color = type.map(known -> ShaftColors.of(known.material())).orElse(ShaftColors.GLACITE);
		Component title = Component.literal(type.map(ShaftType::displayName).orElse("Mineshaft") + " #" + recap.number())
				.withStyle(ChatFormatting.BOLD);
		graphics.text(font, title, left + PAD + 18, top + 8, color, true);
		long now = System.currentTimeMillis();
		String when = visit.code() + "  " + JournalText.day(visit.enteredAt(), now, zone) + " " + JournalText.clock(visit.enteredAt(), zone);
		graphics.text(font, when, left + PAD + 18, top + 19, FAINT, true);
		arrows(graphics, right, top + 8, visits.size(), mouseX, mouseY);

		int y = top + 34;
		drawTiles(graphics, left + PAD, y, panelWidth - PAD * 2, tiles(visit, recap));
		y += TILE_HEIGHT + 8;

		// the details in two columns, a line under the tiles keeps them apart
		List<Line> lines = lines(visit, recap);
		int columnWidth = (panelWidth - PAD * 3) / 2;
		for (int i = 0; i < lines.size(); i++) {
			Line line = lines.get(i);
			int x = left + PAD + (i % 2) * (columnWidth + PAD);
			int lineY = y + (i / 2) * LINE;
			graphics.text(font, line.label(), x, lineY, FAINT, true);
			int valueColor = line.value().equals("-") ? FAINT : line.color();
			graphics.text(font, line.value(), x + columnWidth - font.width(line.value()), lineY, valueColor, true);
		}
		y += (lines.size() + 1) / 2 * LINE + 6;
		corpses(graphics, left + PAD, y, panelWidth - PAD * 2, visit.corpses());
		footer(graphics, right, bottom, mouseX, mouseY);
	}

	// the panel is as tall as what it shows
	private static int contentHeight(ShaftVisit visit, List<ShaftVisit> visits) {
		int lines = lines(visit, ShaftRecap.of(visit, visits)).size();
		return 34 + TILE_HEIGHT + 8 + (lines + 1) / 2 * LINE + 6 + (visit.corpses().isEmpty() ? 0 : 14) + BUTTON_HEIGHT + 10;
	}

	private List<Tile> tiles(ShaftVisit visit, ShaftRecap recap) {
		List<Tile> tiles = new ArrayList<>();
		tiles.add(new Tile(visit.coins() == 0 ? "-" : CoinText.signed(visit.coins()), visit.coins() < 0 ? BAD : COINS, "Profit"));
		tiles.add(new Tile(recap.perHour() == 0 ? "-" : CoinText.plain(recap.perHour()), recap.perHour() < 0 ? BAD : COINS, "Per hour"));
		tiles.add(new Tile(visit.spentMillis() > 0 ? JournalText.duration(visit.spentMillis()) : "-", TEXT, "Time inside"));
		CorpseTally corpses = visit.corpses();
		tiles.add(new Tile(corpses.isEmpty() ? "-" : corpses.looted().length() + "/" + corpses.seen().length(), TEXT, "Corpses"));
		return tiles;
	}

	private static List<Line> lines(ShaftVisit visit, ShaftRecap recap) {
		List<Line> lines = new ArrayList<>();
		lines.add(new Line("Mined", visit.minedCoins() == 0 ? "-" : CoinText.signed(visit.minedCoins()), COINS));
		lines.add(new Line("Corpse profit", visit.corpseCoins() == 0 ? "-" : CoinText.signed(visit.corpseCoins()),
				visit.corpseCoins() < 0 ? BAD : GOOD));
		lines.add(new Line("Route", visit.runMillis() > 0 ? JournalText.duration(visit.runMillis()) : "-", ROUTE));
		if (recap.hasAverage()) {
			long better = visit.coins() - recap.average();
			lines.add(new Line("Vs your average", CoinText.signed(better), better < 0 ? BAD : GOOD));
		} else {
			lines.add(new Line("Vs your average", "-", FAINT));
		}
		lines.add(visit.isJasper()
				? new Line("Dry before", String.valueOf(recap.dry()), ShaftColors.of(ShaftMaterial.JASPER))
				: new Line("Since jasper", String.valueOf(recap.dry()), TEXT));
		if (recap.hasAverage()) {
			lines.add(new Line("Average paid", CoinText.signed(recap.average()), MUTED));
		}
		return lines;
	}

	private void drawTiles(GuiGraphicsExtractor graphics, int left, int top, int width, List<Tile> tiles) {
		int tileWidth = (width - GAP * (tiles.size() - 1)) / tiles.size();
		for (int i = 0; i < tiles.size(); i++) {
			Tile tile = tiles.get(i);
			int x = left + i * (tileWidth + GAP);
			graphics.fill(x, top, x + tileWidth, top + TILE_HEIGHT, TILE);
			float scale = font.width(tile.value()) * BIG <= tileWidth - 10 ? BIG : 1f;
			graphics.pose().pushMatrix();
			try {
				graphics.pose().translate(x + 5, top + (scale == BIG ? 5 : 8));
				graphics.pose().scale(scale, scale);
				graphics.text(font, tile.value(), 0, 0, tile.value().equals("-") ? FAINT : tile.color(), true);
			} finally {
				graphics.pose().popMatrix();
			}
			graphics.text(font, tile.label(), x + 5, top + 20, FAINT, true);
		}
	}

	// one square per corpse of the shaft, filled once it was opened
	private void corpses(GuiGraphicsExtractor graphics, int left, int y, int width, CorpseTally corpses) {
		if (corpses.isEmpty()) {
			return;
		}
		graphics.fill(left, y - 4, left + width, y - 3, TRACK);
		int x = left;
		List<String> parts = new ArrayList<>();
		for (int type = 0; type < 4; type++) {
			char letter = CorpseTally.ORDER.charAt(type);
			int seen = corpses.seen(letter);
			if (seen == 0) {
				continue;
			}
			int looted = corpses.looted(letter);
			parts.add(CorpseType.values()[type].displayName() + " " + looted + "/" + seen);
			for (int i = 0; i < seen; i++) {
				if (i < looted) {
					graphics.fill(x, y + 1, x + DOT, y + 1 + DOT, CORPSE_COLORS[type]);
				} else {
					graphics.outline(x, y + 1, DOT, DOT, CORPSE_COLORS[type]);
				}
				x += DOT + 2;
			}
			x += 4;
		}
		graphics.text(font, String.join("   ", parts), x + 4, y, MUTED, true);
	}

	// older on the left, newer on the right, with how far back this one is
	private void arrows(GuiGraphicsExtractor graphics, int right, int y, int count, int mouseX, int mouseY) {
		int small = buttonWidth(">");
		int nextLeft = right - PAD - small;
		String where = (index + 1) + "/" + count;
		int whereLeft = nextLeft - 4 - font.width(where);
		int backLeft = whereLeft - 4 - small;
		if (index > 0 && button(graphics, backLeft, y - 3, small, "<", TEXT, mouseX, mouseY, () -> index--)) {
			tooltip(graphics, "The shaft before this one.", mouseX, mouseY);
		}
		graphics.text(font, where, whereLeft, y, FAINT, true);
		if (index < count - 1 && button(graphics, nextLeft, y - 3, small, ">", TEXT, mouseX, mouseY, () -> index++)) {
			tooltip(graphics, "The shaft after this one.", mouseX, mouseY);
		}
	}

	private void footer(GuiGraphicsExtractor graphics, int right, int bottom, int mouseX, int mouseY) {
		int y = bottom - BUTTON_HEIGHT - 6;
		int doneWidth = buttonWidth("Done");
		button(graphics, right - PAD - doneWidth, y, doneWidth, "Done", ACCENT, mouseX, mouseY, this::onClose);
		int journalWidth = buttonWidth("Journal");
		if (button(graphics, right - PAD - doneWidth - GAP - journalWidth, y, journalWidth, "Journal", TEXT, mouseX, mouseY,
				() -> GameCompat.setScreen(minecraft, new JournalScreen(this)))) {
			tooltip(graphics, "Every shaft you were in.", mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		index += scrollY > 0 ? -1 : 1;
		return true;
	}

	@Override
	public void onClose() {
		GameCompat.setScreen(minecraft, parent);
	}
}
