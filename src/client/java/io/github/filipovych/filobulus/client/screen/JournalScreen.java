package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.config.JournalConfig;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.client.hud.RouteIcons;
import io.github.filipovych.filobulus.client.journal.JournalStore;
import io.github.filipovych.filobulus.corpse.CoinText;
import io.github.filipovych.filobulus.corpse.CorpseTally;
import io.github.filipovych.filobulus.corpse.CorpseType;
import io.github.filipovych.filobulus.journal.JournalDays;
import io.github.filipovych.filobulus.journal.JournalPeriod;
import io.github.filipovych.filobulus.journal.JournalStats;
import io.github.filipovych.filobulus.journal.JournalStats.Group;
import io.github.filipovych.filobulus.journal.JournalText;
import io.github.filipovych.filobulus.journal.MonthlyWrapped;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// /filo journal: what the logged shafts add up to, a count per shaft layout that also filters, and the log itself
// the corpses tab shows the same shafts by what their corpses were and what they paid, the days tab what each day added up to
// the party tab is the shafts tab with only the shafts someone else found
public final class JournalScreen extends FlatScreen {
	private static final int MAX_WIDTH = 560;
	private static final int MAX_HEIGHT = 420;
	private static final int MARGIN = 8;
	private static final int PAD = 8;
	private static final int GAP = 4;
	private static final int TILES_TOP = 28;
	private static final int TILE_HEIGHT = 32;
	private static final int BODY_TOP = TILES_TOP + TILE_HEIGHT + 10;
	private static final int LIST_HEAD = 13;
	private static final int FOOTER = 26;
	private static final int GROUP_ROW = 20;
	private static final int LOG_ROW = 13;
	private static final int NUMBER_COLUMN = 38;
	private static final float BIG = 1.5f;
	// argb
	private static final int SELECTED = 0x247FD4FF;
	private static final int ROW_HOVER = 0x12FFFFFF;
	private static final int TRACK = 0x16FFFFFF;
	private static final int CHIP_ON = 0xFF2F86B3;
	// one colour per corpse type, in the order lapis, tungsten, umber, vanguard
	private static final int[] CORPSE_COLORS = {0xFF4A7BFF, 0xFF8C9AA6, 0xFFC8743A, 0xFFFFD75E};
	private static final int DOT = 4;
	private static final String PARTY_TAG = "party";

	private enum View {
		SHAFTS, CORPSES, DAYS, PARTY
	}

	// more days than this do not fit as bars anyway
	private static final int MAX_DAYS = 120;
	private static final int COINS = 0xFFFFAA00;

	// one line of the log: a visit, or the day that the visits under it share
	private record Entry(ShaftVisit visit, String day, int shafts) {
	}

	// brief is the label for when the tile is too narrow for the full one
	private record Tile(String value, int color, String note, String label, String brief, String help) {
	}

	// the period and the tab stay as they were left while the game runs
	private static JournalPeriod period = JournalPeriod.ALL;
	private static View view = View.SHAFTS;
	// what the bars of the days tab stand for
	private static boolean dayCoins = true;

	private final Screen parent;
	private final JournalStore store = FilobulusClient.journal();
	private final ZoneId zone = ZoneId.systemDefault();
	// layout keys the log is narrowed to, empty shows every shaft
	private final Set<String> picked = new LinkedHashSet<>();
	// the same for the corpses tab, letters of the corpse types
	private final Set<Character> pickedCorpses = new LinkedHashSet<>();
	private int groupScroll;
	private int logScroll;
	private int logLeft;
	private String status = "";

	// counted again only when something changed
	private int countedVersion = -1;
	private JournalPeriod countedPeriod;
	private Set<String> countedPicked = Set.of();
	private Set<Character> countedCorpses = Set.of();
	private View countedView;
	private long countedMinute;
	private JournalStats stats = JournalStats.of(List.of());
	private List<ShaftVisit> shown = List.of();
	private List<Entry> log = List.of();
	private JournalDays days = new JournalDays(List.of());

	public JournalScreen(Screen parent) {
		super(Component.literal("Filobulus journal"));
		this.parent = parent;
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		long now = System.currentTimeMillis();
		if (view == View.PARTY && !JournalConfig.party) {
			view = View.SHAFTS;
		}
		count(now);
		int panelWidth = Math.min(width - MARGIN * 2, MAX_WIDTH);
		int panelHeight = Math.min(height - MARGIN * 2, MAX_HEIGHT);
		int left = (width - panelWidth) / 2;
		int top = (height - panelHeight) / 2;
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		panel(graphics, left, top, panelWidth, panelHeight);

		Component title = Component.literal("Journal").withStyle(ChatFormatting.BOLD);
		JournalPeriod[] periods = JournalPeriod.values();
		int chipsWidth = 0;
		for (JournalPeriod each : periods) {
			chipsWidth += buttonWidth(each == JournalPeriod.ALL ? "All" : each.title()) + 2;
		}
		int tabsWidth = buttonWidth("Shafts") + buttonWidth("Corpses") + buttonWidth("Days")
				+ (JournalConfig.party ? buttonWidth("Party") : 0);
		// on a narrow screen the title gives way so the tabs do not run into the periods
		boolean titled = PAD + font.width(title) + 8 + tabsWidth <= panelWidth - PAD - chipsWidth;
		if (titled) {
			graphics.text(font, title, left + PAD, top + 9, ACCENT, true);
		}
		int tabLeft = left + PAD + (titled ? font.width(title) + 8 : 0);
		tabLeft += tab(graphics, tabLeft, top + 6, "Shafts", view == View.SHAFTS, mouseX, mouseY, () -> view = View.SHAFTS);
		tabLeft += tab(graphics, tabLeft, top + 6, "Corpses", view == View.CORPSES, mouseX, mouseY, () -> view = View.CORPSES);
		tabLeft += tab(graphics, tabLeft, top + 6, "Days", view == View.DAYS, mouseX, mouseY, () -> view = View.DAYS);
		if (JournalConfig.party) {
			tab(graphics, tabLeft, top + 6, "Party", view == View.PARTY, mouseX, mouseY, () -> view = View.PARTY);
		}
		int x = right - PAD;
		for (int i = periods.length - 1; i >= 0; i--) {
			JournalPeriod each = periods[i];
			// kept short, the tabs need the room
			String label = each == JournalPeriod.ALL ? "All" : each.title();
			x -= buttonWidth(label);
			chip(graphics, x, top + 6, label, each == period, mouseX, mouseY, () -> period = each);
			x -= 2;
		}
		drawTiles(graphics, left + PAD, top + TILES_TOP, panelWidth - PAD * 2, mouseX, mouseY);

		int bodyTop = top + BODY_TOP;
		int bodyBottom = bottom - FOOTER;
		if (store.journal().visits().isEmpty()) {
			int middle = (bodyTop + bodyBottom) / 2;
			graphics.centeredText(font, "No shafts yet.", width / 2, middle - 10, MUTED);
			graphics.centeredText(font, "Every mineshaft you enter is written down here by itself.", width / 2, middle + 2, FAINT);
		} else if (view == View.DAYS) {
			drawDays(graphics, left + PAD, bodyTop, panelWidth - PAD * 2, bodyBottom, mouseX, mouseY, now);
		} else {
			int groupsWidth = Math.min(215, (panelWidth - PAD * 3) * 9 / 20);
			logLeft = left + PAD + groupsWidth + PAD;
			if (view != View.CORPSES) {
				drawGroups(graphics, left + PAD, bodyTop, groupsWidth, bodyBottom, mouseX, mouseY);
			} else {
				drawCorpseTypes(graphics, left + PAD, bodyTop, groupsWidth, mouseX, mouseY);
			}
			graphics.fill(logLeft - PAD / 2, bodyTop, logLeft - PAD / 2 + 1, bodyBottom, TRACK);
			drawLog(graphics, logLeft, bodyTop, right - PAD - logLeft, bodyBottom, mouseX, mouseY, now);
		}

		int y = bottom - BUTTON_HEIGHT - 6;
		int doneWidth = buttonWidth("Done");
		button(graphics, right - PAD - doneWidth, y, doneWidth, "Done", ACCENT, mouseX, mouseY, this::onClose);
		int copyWidth = buttonWidth("Copy CSV");
		int copyLeft = right - PAD - doneWidth - GAP - copyWidth;
		if (button(graphics, copyLeft, y, copyWidth, "Copy CSV", TEXT, mouseX, mouseY, this::copy)) {
			tooltip(graphics, "Copy the log as it is shown here, for a spreadsheet.", mouseX, mouseY);
		}
		int wrappedWidth = buttonWidth("Wrapped");
		copyLeft -= GAP + wrappedWidth;
		if (button(graphics, copyLeft, y, wrappedWidth, "Wrapped", TEXT, mouseX, mouseY, () -> GameCompat.setScreen(minecraft,
				new WrappedScreen(this, MonthlyWrapped.defaultMonth(store.journal().visits(), now, zone))))) {
			tooltip(graphics, "Your last full month of shafts, one card at a time.", mouseX, mouseY);
		}
		graphics.text(font, clip(status.isEmpty() ? summary(now) : status, copyLeft - left - PAD * 2),
				left + PAD, y + 3, status.isEmpty() ? FAINT : GOOD, true);
	}

	private void drawTiles(GuiGraphicsExtractor graphics, int left, int top, int width, int mouseX, int mouseY) {
		List<Tile> tiles = switch (view) {
			case SHAFTS, PARTY -> shaftTiles();
			case CORPSES -> corpseTiles();
			case DAYS -> dayTiles();
		};
		int tileWidth = (width - GAP * (tiles.size() - 1)) / tiles.size();
		for (int i = 0; i < tiles.size(); i++) {
			Tile tile = tiles.get(i);
			int x = left + i * (tileWidth + GAP);
			graphics.fill(x, top, x + tileWidth, top + TILE_HEIGHT, BUTTON);
			// the number is drawn big when the tile has room for it
			int noteWidth = tile.note().isEmpty() ? 0 : font.width(tile.note()) + 4;
			float scale = font.width(tile.value()) * BIG + noteWidth <= tileWidth - 10 ? BIG : 1f;
			graphics.pose().pushMatrix();
			try {
				graphics.pose().translate(x + 5, top + (scale == BIG ? 5 : 8));
				graphics.pose().scale(scale, scale);
				graphics.text(font, tile.value(), 0, 0, tile.color(), true);
			} finally {
				graphics.pose().popMatrix();
			}
			graphics.text(font, tile.note(), x + 9 + Math.round(font.width(tile.value()) * scale), top + 9, MUTED, true);
			String label = font.width(tile.label()) <= tileWidth - 8 ? tile.label() : tile.brief();
			graphics.text(font, label, x + 5, top + 20, FAINT, true);
			if (mouseX >= x && mouseX < x + tileWidth && mouseY >= top && mouseY < top + TILE_HEIGHT) {
				tooltip(graphics, tile.help(), mouseX, mouseY);
			}
		}
	}

	private List<Tile> shaftTiles() {
		String average = stats.timed() == 0 ? "-" : JournalText.duration(stats.averageTime());
		String perHour = stats.perHour() == 0 ? "" : Math.round(stats.perHour()) + "/h";
		return List.of(
				new Tile(String.valueOf(stats.shafts()), TEXT, perHour, "Shafts", "Shafts",
						"Mineshafts you entered in this period, and how many that is per hour of shafting. "
								+ "A gap of more than 20 minutes between two shafts counts as a break and not as shafting."),
				new Tile(String.valueOf(stats.jasper()), ShaftColors.of(ShaftMaterial.JASPER),
						stats.shafts() == 0 ? "" : JournalText.percent(stats.jasperRate()), "Jasper", "Jasper",
						"Jasper shafts in this period, jasper crystal ones included, and their share of all shafts. "
								+ "The wiki's spawn weights put it at about 4.3%, or 5.9% when the last block you mined has no shaft type of its own."),
				new Tile(String.valueOf(store.journal().sinceJasper()), TEXT, "", "Since jasper", "Dry streak",
						"Shafts entered since your last jasper shaft. This one always counts up to now, whatever the period."),
				new Tile(String.valueOf(stats.longestDry()), TEXT, "", "Longest dry", "Max dry",
						"The most shafts in a row without a jasper one in this period."),
				new Tile(JournalText.duration(stats.timeInside()), TEXT, "", "Time inside", "Inside",
						"Time spent inside shafts in this period."),
				new Tile(average, TEXT, "", "Avg time", "Avg", "Average time inside one shaft."));
	}

	private List<Tile> corpseTiles() {
		JournalStats.Corpses corpses = stats.corpses();
		int seen = corpses.seen();
		int looted = corpses.looted();
		String each = corpses.counted() == 0 ? "" : String.format(Locale.ROOT, "%.1f", corpses.perShaft());
		long coins = corpses.coins();
		return List.of(
				new Tile(String.valueOf(seen), TEXT, each, "Corpses", "Corpses",
						"Corpses in the shafts of this period, and how many a shaft has on average. Read from the corpse lines "
								+ "of the tab list, so only shafts entered since the journal reads them count: "
								+ shafts(corpses.counted()) + " of this period."),
				new Tile(String.valueOf(looted), GOOD, seen == 0 ? "" : JournalText.percent((double) looted / seen), "Looted", "Looted",
						"Corpses you opened, and their share of the ones that were there."),
				new Tile(String.valueOf(Math.max(0, seen - looted)), seen > looted ? BAD : TEXT, "", "Left behind", "Left",
						"Corpses you walked out on: " + left(corpses) + "."),
				new Tile(String.valueOf(corpses.keys()), TEXT, "", "Keys used", "Keys",
						"Tungsten, umber and vanguard corpses you opened, each took a key unless the Resourceful perk saved it: "
								+ corpses.lootedByType()[1] + " tungsten, " + corpses.lootedByType()[2] + " umber, " + corpses.lootedByType()[3] + " vanguard."),
				new Tile(CoinText.signed(coins), coins < 0 ? BAD : GOOD, "", "Corpse profit", "Profit",
						"What the loot of your corpses sold for at the time, with the price of the keys taken off."),
				new Tile(corpses.paid() == 0 ? "-" : CoinText.signed(corpses.coinsPerShaft()), TEXT, "", "Per shaft", "Each",
						"Corpse profit per shaft in which you opened at least one corpse."));
	}

	private List<Tile> dayTiles() {
		int played = days.played();
		long coins = days.coins();
		JournalDays.Day best = days.best();
		String perDay = played == 0 ? "-" : CoinText.signed(coins / played);
		String shaftsPerDay = played == 0 ? "-" : String.valueOf(Math.round((double) days.shafts() / played));
		return List.of(
				new Tile(String.valueOf(played), TEXT, "", "Days played", "Days",
						"Days of this period on which you entered at least one shaft. The bars show at most the last " + MAX_DAYS + " days."),
				new Tile(CoinText.signed(coins), coins < 0 ? BAD : COINS, "", "Coins", "Coins",
						"What your shafts paid on those days: corpse profit plus what the profit tracker counted as mined inside."),
				new Tile(perDay, TEXT, "", "Per day", "Per day", "Coins per day you played."),
				new Tile(best == null ? "-" : CoinText.signed(best.coins()), GOOD, "", "Best day", "Best",
						best == null ? "No day yet." : "Your best day was " + dayName(best.start()) + ", with " + shafts(best.shafts()) + "."),
				new Tile(shaftsPerDay, TEXT, "", "Shafts a day", "Shafts", "Shafts entered per day you played."),
				new Tile(days.shafts() == 0 ? "-" : CoinText.signed(coins / days.shafts()), TEXT, "", "Per shaft", "Each",
						"Coins per shaft, over every shaft of those days."));
	}

	// one bar per day, the latest on the right, hovering a bar says what that day was
	private void drawDays(GuiGraphicsExtractor graphics, int left, int top, int width, int bottom,
			int mouseX, int mouseY, long now) {
		graphics.text(font, dayCoins ? "Coins per day" : "Shafts per day", left, top, FAINT, true);
		int chipLeft = left + width - buttonWidth("Shafts");
		chip(graphics, chipLeft, top - 3, "Shafts", !dayCoins, mouseX, mouseY, () -> dayCoins = false);
		chip(graphics, chipLeft - 2 - buttonWidth("Coins"), top - 3, "Coins", dayCoins, mouseX, mouseY, () -> dayCoins = true);
		List<JournalDays.Day> all = days.days();
		// the tiles count every day, the bars only the last ones
		if (all.size() > MAX_DAYS) {
			all = all.subList(all.size() - MAX_DAYS, all.size());
		}
		if (all.isEmpty()) {
			graphics.text(font, "Nothing in this period.", left, top + LIST_HEAD + 4, FAINT, true);
			return;
		}
		int chartTop = top + LIST_HEAD + 8;
		int chartBottom = bottom - 13;
		// as many of the latest days as there is room for
		int slot = Math.max(3, Math.min(48, width / all.size()));
		List<JournalDays.Day> shownDays = all.subList(all.size() - Math.min(all.size(), width / slot), all.size());
		int bar = slot > 4 ? slot - 2 : slot - 1;
		long most = 1;
		for (JournalDays.Day day : shownDays) {
			most = Math.max(most, dayCoins ? day.coins() : day.shafts());
		}
		graphics.fill(left, chartBottom, left + width, chartBottom + 1, TRACK);
		graphics.text(font, dayCoins ? CoinText.amount(most) : String.valueOf(most), left, chartTop - 9, FAINT, true);
		for (int i = 0; i < shownDays.size(); i++) {
			JournalDays.Day day = shownDays.get(i);
			int x = left + i * slot;
			long value = dayCoins ? day.coins() : day.shafts();
			boolean hovered = mouseX >= x && mouseX < x + slot && mouseY >= chartTop && mouseY < bottom;
			if (hovered) {
				graphics.fill(x, chartTop, x + slot, chartBottom, ROW_HOVER);
				tooltip(graphics, dayHelp(day), mouseX, mouseY);
			}
			if (value > 0) {
				int height = (int) Math.max(1, (chartBottom - chartTop) * value / most);
				graphics.fill(x, chartBottom - height, x + bar, chartBottom, dayCoins ? COINS : ACCENT);
				if (day.jasper() > 0) {
					// a day with a jasper shaft wears its colour on top
					graphics.fill(x, chartBottom - height, x + bar, chartBottom - height + 2, ShaftColors.of(ShaftMaterial.JASPER));
				}
			} else if (value < 0) {
				graphics.fill(x, chartBottom + 1, x + bar, chartBottom + 3, BAD);
			}
		}
		// the first and the last day under their bars, only the last one when the two would run into each other
		String first = dayName(shownDays.get(0).start());
		String last = dayName(shownDays.get(shownDays.size() - 1).start());
		int barsRight = left + shownDays.size() * slot - (slot - bar);
		if (barsRight - left >= font.width(first) + font.width(last) + 8) {
			graphics.text(font, first, left, chartBottom + 4, FAINT, true);
			rightText(graphics, last, barsRight, chartBottom + 4, FAINT);
		} else {
			graphics.text(font, last, left, chartBottom + 4, FAINT, true);
		}
	}

	private String dayName(long start) {
		return JournalText.day(start, System.currentTimeMillis(), zone);
	}

	private String dayHelp(JournalDays.Day day) {
		String name = dayName(day.start());
		if (day.shafts() == 0) {
			return name + ": no shafts.";
		}
		return name + ": " + shafts(day.shafts()) + (day.jasper() > 0 ? ", " + day.jasper() + " jasper" : "") + ". Corpses "
				+ CoinText.signed(day.corpseCoins()) + ", mined " + CoinText.signed(day.minedCoins()) + ", total "
				+ CoinText.signed(day.coins()) + ". " + JournalText.duration(day.timeInside()) + " inside.";
	}

	private static String left(JournalStats.Corpses corpses) {
		List<String> parts = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			parts.add(corpses.seenByType()[i] - corpses.lootedByType()[i] + " " + CorpseType.values()[i].displayName().toLowerCase(Locale.ROOT));
		}
		return String.join(", ", parts);
	}

	// one row per corpse type, a click narrows the log to the shafts that had one
	private void drawCorpseTypes(GuiGraphicsExtractor graphics, int left, int top, int width, int mouseX, int mouseY) {
		int right = left + width - 5;
		graphics.text(font, "Corpse", left, top, FAINT, true);
		rightText(graphics, "Looted", right, top, FAINT);
		JournalStats.Corpses corpses = stats.corpses();
		if (corpses.counted() == 0) {
			graphics.text(font, "No corpses in this period.", left, top + LIST_HEAD + 4, FAINT, true);
			return;
		}
		for (int i = 0; i < 4; i++) {
			int y = top + LIST_HEAD + i * GROUP_ROW;
			char letter = CorpseTally.ORDER.charAt(i);
			boolean on = pickedCorpses.contains(letter);
			boolean hovered = mouseX >= left && mouseX < left + width && mouseY >= y && mouseY < y + GROUP_ROW;
			if (on) {
				graphics.fill(left, y, right + 3, y + GROUP_ROW - 1, SELECTED);
				graphics.fill(left, y, left + 2, y + GROUP_ROW - 1, ACCENT);
			} else if (hovered) {
				graphics.fill(left, y, right + 3, y + GROUP_ROW - 1, ROW_HOVER);
			}
			int seen = corpses.seenByType()[i];
			int looted = corpses.lootedByType()[i];
			graphics.item(corpseIcon(i), left + 4, y + 1);
			graphics.text(font, CorpseType.values()[i].displayName(), left + 24, y + 4, CORPSE_COLORS[i], true);
			rightText(graphics, looted + "/" + seen, right, y + 4, seen == 0 ? FAINT : TEXT);
			graphics.fill(left + 24, y + 15, right, y + 16, TRACK);
			if (seen > 0) {
				graphics.fill(left + 24, y + 15, left + 24 + Math.max(1, (right - left - 24) * looted / seen), y + 16, CORPSE_COLORS[i]);
			}
			clickable(left, y, width, GROUP_ROW, () -> {
				if (!pickedCorpses.remove(letter)) {
					pickedCorpses.add(letter);
				}
				logScroll = 0;
			}, null);
			if (hovered) {
				tooltip(graphics, CorpseType.values()[i].displayName() + ": " + looted + " of " + seen + " opened. "
						+ (on ? "Click to show every shaft again." : "Click to show only the shafts that had one."), mouseX, mouseY);
			}
		}
	}

	private static ItemStack corpseIcon(int type) {
		return switch (type) {
			case 0 -> RouteIcons.of("lapis_lazuli");
			case 1 -> ShaftIcons.of(ShaftMaterial.TUNGSTEN);
			case 2 -> ShaftIcons.of(ShaftMaterial.UMBER);
			default -> ShaftIcons.of(ShaftMaterial.VANGUARD);
		};
	}

	// a small square per corpse of the shaft, filled once it was opened, drawn leftwards from right
	private int dots(GuiGraphicsExtractor graphics, CorpseTally corpses, int right, int y) {
		int x = right;
		for (int i = corpses.seen().length() - 1; i >= 0; i--) {
			char letter = corpses.seen().charAt(i);
			int color = CORPSE_COLORS[CorpseTally.ORDER.indexOf(letter)];
			// the first ones of a type are the opened ones
			int before = i - corpses.seen().indexOf(letter);
			x -= DOT + 1;
			if (before < corpses.looted(letter)) {
				graphics.fill(x, y, x + DOT, y + DOT, color);
			} else {
				graphics.outline(x, y, DOT, DOT, color);
			}
		}
		return right - x;
	}

	private static String corpseHelp(ShaftVisit visit) {
		CorpseTally corpses = visit.corpses();
		List<String> parts = new ArrayList<>();
		for (int i = 0; i < 4; i++) {
			char letter = CorpseTally.ORDER.charAt(i);
			if (corpses.seen(letter) > 0) {
				parts.add(CorpseType.values()[i].displayName() + " " + corpses.looted(letter) + "/" + corpses.seen(letter));
			}
		}
		String text = parts.isEmpty() ? "" : "Corpses opened: " + String.join(", ", parts) + ".";
		if (visit.corpseCoins() != 0) {
			text += " Corpse profit " + CoinText.signed(visit.corpseCoins()) + ".";
		}
		if (visit.minedCoins() != 0) {
			text += " Mined " + CoinText.signed(visit.minedCoins()) + ".";
		}
		return text.strip();
	}

	// one row per shaft layout that was entered, a click narrows the log to it
	private void drawGroups(GuiGraphicsExtractor graphics, int left, int top, int width, int bottom,
			int mouseX, int mouseY) {
		int right = left + width - 5;
		graphics.text(font, "Shaft", left, top, FAINT, true);
		rightText(graphics, "Shafts", right - NUMBER_COLUMN, top, FAINT);
		rightText(graphics, "Avg", right, top, FAINT);
		int listTop = top + LIST_HEAD;
		List<Group> groups = stats.groups();
		if (groups.isEmpty()) {
			graphics.text(font, view == View.PARTY ? "No joined shafts in this period." : "Nothing in this period.",
					left, listTop + 4, FAINT, true);
			return;
		}
		int content = groups.size() * GROUP_ROW;
		groupScroll = Math.max(0, Math.min(groupScroll, content - (bottom - listTop)));
		boolean inList = mouseX >= left && mouseX < left + width && mouseY >= listTop && mouseY < bottom;
		int most = groups.get(0).shafts();
		graphics.enableScissor(left, listTop, left + width, bottom);
		for (int i = 0; i < groups.size(); i++) {
			int y = listTop + i * GROUP_ROW - groupScroll;
			if (y + GROUP_ROW <= listTop || y >= bottom) {
				continue;
			}
			Group group = groups.get(i);
			String key = group.key();
			Optional<ShaftLayout> layout = ShaftLayout.byKey(key);
			int color = layout.map(JournalScreen::color).orElse(MUTED);
			boolean on = picked.contains(key);
			boolean hovered = inList && mouseY >= y && mouseY < y + GROUP_ROW;
			if (on) {
				graphics.fill(left, y, right + 3, y + GROUP_ROW - 1, SELECTED);
				graphics.fill(left, y, left + 2, y + GROUP_ROW - 1, ACCENT);
			} else if (hovered) {
				graphics.fill(left, y, right + 3, y + GROUP_ROW - 1, ROW_HOVER);
			}
			layout.ifPresent(known -> graphics.item(ShaftIcons.of(known), left + 4, y + 1));
			String title = title(key);
			graphics.text(font, clip(title, right - NUMBER_COLUMN - 34 - left - 24), left + 24, y + 4, color, true);
			rightText(graphics, String.valueOf(group.shafts()), right - NUMBER_COLUMN, y + 4, TEXT);
			rightText(graphics, group.timed() == 0 ? "-" : JournalText.duration(group.averageTime()), right, y + 4, MUTED);
			// how it compares to the shaft entered the most
			graphics.fill(left + 24, y + 15, right, y + 16, TRACK);
			graphics.fill(left + 24, y + 15, left + 24 + Math.max(1, (right - left - 24) * group.shafts() / most), y + 16, color);
			clickable(left, Math.max(y, listTop), width, Math.min(y + GROUP_ROW, bottom) - Math.max(y, listTop), () -> {
				if (!picked.remove(key)) {
					picked.add(key);
				}
				logScroll = 0;
			}, null);
			if (hovered) {
				tooltip(graphics, groupHelp(title, group), mouseX, mouseY);
			}
		}
		graphics.disableScissor();
		scrollbar(graphics, left + width, listTop, bottom, content, groupScroll);
	}

	private String groupHelp(String title, Group group) {
		String text = title + ": " + shafts(group.shafts()) + ", "
				+ JournalText.percent((double) group.shafts() / Math.max(1, stats.shafts())) + " of this period.";
		if (group.timed() > 0) {
			text += " " + JournalText.duration(group.timeInside()) + " inside, "
					+ JournalText.duration(group.averageTime()) + " on average.";
		}
		if (group.bestRun() > 0) {
			text += " Fastest route " + JournalText.duration(group.bestRun()) + ".";
		}
		return text + (picked.contains(group.key()) ? " Click to show every shaft again." : " Click to show only these in the log.");
	}

	private void drawLog(GuiGraphicsExtractor graphics, int left, int top, int width, int bottom,
			int mouseX, int mouseY, long now) {
		int right = left + width - 5;
		int insideRight = right - NUMBER_COLUMN;
		int atRight = insideRight - NUMBER_COLUMN;
		boolean corpseView = view == View.CORPSES;
		List<String> names = corpseView
				? pickedCorpses.stream().map(letter -> CorpseTally.type(letter).displayName()).toList()
				: picked.stream().map(JournalScreen::title).toList();
		String head = names.isEmpty() ? "Log" : "Log: " + String.join(", ", names);
		int headRoom = atRight - 30 - left - (names.isEmpty() ? 0 : buttonWidth("Clear") + GAP);
		graphics.text(font, clip(head, headRoom), left, top, FAINT, true);
		if (!names.isEmpty()) {
			int clearLeft = left + Math.min(font.width(head), headRoom) + GAP;
			button(graphics, clearLeft, top - 3, buttonWidth("Clear"), "Clear", MUTED, mouseX, mouseY,
					corpseView ? pickedCorpses::clear : picked::clear);
		}
		rightText(graphics, "At", atRight, top, FAINT);
		rightText(graphics, corpseView ? "Open" : "Inside", insideRight, top, FAINT);
		rightText(graphics, corpseView ? "Coins" : "Route", right, top, FAINT);
		int listTop = top + LIST_HEAD;
		if (log.isEmpty()) {
			graphics.text(font, "Nothing in this period.", left, listTop + 4, FAINT, true);
			return;
		}
		int content = log.size() * LOG_ROW;
		logScroll = Math.max(0, Math.min(logScroll, content - (bottom - listTop)));
		ShaftVisit open = FilobulusClient.shaftLog().open();
		graphics.enableScissor(left, listTop, left + width, bottom);
		int last = Math.min(log.size(), (logScroll + bottom - listTop) / LOG_ROW + 1);
		for (int i = logScroll / LOG_ROW; i < last; i++) {
			int y = listTop + i * LOG_ROW - logScroll;
			Entry entry = log.get(i);
			if (entry.visit() == null) {
				graphics.text(font, entry.day(), left, y + 3, TEXT, true);
				int dayRight = left + font.width(entry.day());
				graphics.text(font, shafts(entry.shafts()), dayRight + 6, y + 3, FAINT, true);
				graphics.fill(dayRight + 12 + font.width(shafts(entry.shafts())), y + 7, right, y + 8, TRACK);
				continue;
			}
			ShaftVisit visit = entry.visit();
			Optional<ShaftType> type = visit.type();
			if (mouseX >= left && mouseX < left + width && mouseY >= Math.max(y, listTop) && mouseY < Math.min(y + LOG_ROW, bottom)) {
				graphics.fill(left, y, right + 3, y + LOG_ROW, ROW_HOVER);
				String help = corpseHelp(visit);
				if (visit.joined() && JournalConfig.party) {
					help = ("Joined: someone else found this shaft. " + help).strip();
				}
				if (!help.isEmpty()) {
					tooltip(graphics, help, mouseX, mouseY);
				}
			}
			type.ifPresent(known -> smallItem(graphics, known, left + 2, y + 1));
			// the corpses sit between the name and the time, the name gives way to them
			int dotsWidth = dots(graphics, visit.corpses(), atRight - 32, y + 4);
			// on the party tab every shaft is a joined one, the tag would say nothing there
			int nameRoom = atRight - 36 - dotsWidth - left - 15;
			boolean tag = visit.joined() && JournalConfig.party && view != View.PARTY && nameRoom > font.width(PARTY_TAG) + 30;
			String name = clip(type.map(ShaftType::displayName).orElse("Unknown"), nameRoom - (tag ? font.width(PARTY_TAG) + 4 : 0));
			graphics.text(font, name, left + 15, y + 3, type.map(known -> ShaftColors.of(known.material())).orElse(MUTED), true);
			if (tag) {
				graphics.text(font, PARTY_TAG, left + 15 + font.width(name) + 4, y + 3, FAINT, true);
			}
			rightText(graphics, JournalText.clock(visit.enteredAt(), zone), atRight, y + 3, MUTED);
			boolean inside = open != null && open.enteredAt() == visit.enteredAt();
			if (corpseView) {
				CorpseTally corpses = visit.corpses();
				rightText(graphics, corpses.looted().length() + "/" + corpses.seen().length(), insideRight, y + 3,
						inside ? GOOD : TEXT);
				long coins = visit.corpseCoins();
				rightText(graphics, coins == 0 ? "" : CoinText.signed(coins), right, y + 3, coins < 0 ? BAD : GOOD);
				continue;
			}
			String spent = inside ? "inside" : visit.spentMillis() > 0 ? JournalText.duration(visit.spentMillis()) : "-";
			rightText(graphics, spent, insideRight, y + 3, inside ? GOOD : TEXT);
			rightText(graphics, visit.runMillis() > 0 ? JournalText.duration(visit.runMillis()) : "", right, y + 3, ACCENT);
		}
		graphics.disableScissor();
		scrollbar(graphics, left + width, listTop, bottom, content, logScroll);
	}

	private void smallItem(GuiGraphicsExtractor graphics, ShaftType type, int x, int y) {
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(x, y);
			graphics.pose().scale(10 / 16f, 10 / 16f);
			graphics.item(ShaftIcons.of(type.material()), 0, 0);
		} finally {
			graphics.pose().popMatrix();
		}
	}

	private void chip(GuiGraphicsExtractor graphics, int left, int top, String label, boolean on,
			int mouseX, int mouseY, Runnable action) {
		int chipWidth = buttonWidth(label);
		if (on) {
			graphics.fill(left, top, left + chipWidth, top + BUTTON_HEIGHT, CHIP_ON);
			graphics.centeredText(font, label, left + chipWidth / 2, top + 3, TEXT);
		} else {
			button(graphics, left, top, chipWidth, label, MUTED, mouseX, mouseY, action);
		}
	}

	private void rightText(GuiGraphicsExtractor graphics, String text, int right, int y, int color) {
		graphics.text(font, text, right - font.width(text), y, color, true);
	}

	// goes through the journal again when it, the period, the filter or the day could have changed
	private void count(long now) {
		int version = store.journal().version();
		if (version == countedVersion && period == countedPeriod && picked.equals(countedPicked)
				&& pickedCorpses.equals(countedCorpses) && view == countedView && now / 60_000 == countedMinute) {
			return;
		}
		if (period != countedPeriod || view != countedView) {
			groupScroll = 0;
			logScroll = 0;
			status = "";
		}
		List<ShaftVisit> inPeriod = List.copyOf(store.journal().since(period.start(now, store.sessionStart(), zone)));
		if (view == View.PARTY) {
			inPeriod = inPeriod.stream().filter(ShaftVisit::joined).toList();
		}
		stats = JournalStats.of(inPeriod);
		// a shaft that is not in this period can not stay picked, the log would be empty with no row to click
		picked.retainAll(stats.groups().stream().map(Group::key).toList());
		days = JournalDays.of(inPeriod, zone, Integer.MAX_VALUE);
		shown = view == View.DAYS ? inPeriod : view != View.CORPSES
				? inPeriod.stream().filter(visit -> picked.isEmpty() || picked.contains(visit.key())).toList()
				: inPeriod.stream().filter(visit -> !visit.corpses().isEmpty() && (pickedCorpses.isEmpty()
						|| pickedCorpses.stream().anyMatch(letter -> visit.corpses().seen(letter) > 0))).toList();
		List<Entry> entries = new ArrayList<>();
		int header = -1;
		String day = "";
		for (int i = shown.size() - 1; i >= 0; i--) {
			ShaftVisit visit = shown.get(i);
			String visitDay = JournalText.day(visit.enteredAt(), now, zone);
			if (!visitDay.equals(day)) {
				day = visitDay;
				header = entries.size();
				entries.add(new Entry(null, day, 0));
			}
			entries.set(header, new Entry(null, day, entries.get(header).shafts() + 1));
			entries.add(new Entry(visit, day, 0));
		}
		log = entries;
		countedVersion = version;
		countedPeriod = period;
		countedPicked = Set.copyOf(picked);
		countedCorpses = Set.copyOf(pickedCorpses);
		countedView = view;
		countedMinute = now / 60_000;
	}

	private String summary(long now) {
		List<ShaftVisit> all = store.journal().visits();
		if (all.isEmpty()) {
			return "";
		}
		String since = JournalText.day(all.get(0).enteredAt(), now, zone);
		return shafts(all.size()) + " logged since " + Character.toLowerCase(since.charAt(0)) + since.substring(1);
	}

	private void copy() {
		minecraft.keyboardHandler.setClipboard(JournalText.csv(shown, zone));
		status = "Copied " + shafts(shown.size()) + " to the clipboard.";
	}

	private static String shafts(int count) {
		return count == 1 ? "1 shaft" : count + " shafts";
	}

	private static String title(String key) {
		return ShaftLayout.byKey(key).map(ShaftLayout::title).orElse("Unknown");
	}

	private static int color(ShaftLayout layout) {
		return layout.material().map(ShaftColors::of).orElse(ACCENT);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (mouseX < logLeft) {
			groupScroll -= (int) Math.signum(scrollY) * GROUP_ROW * 2;
		} else {
			logScroll -= (int) Math.signum(scrollY) * LOG_ROW * 3;
		}
		return true;
	}

	@Override
	public void onClose() {
		GameCompat.setScreen(minecraft, parent);
	}
}
