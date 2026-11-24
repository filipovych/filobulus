package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.client.journal.JournalStore;
import io.github.filipovych.filobulus.journal.JournalPeriod;
import io.github.filipovych.filobulus.journal.JournalStats;
import io.github.filipovych.filobulus.journal.JournalStats.Group;
import io.github.filipovych.filobulus.journal.JournalText;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// /filo journal: what the logged shafts add up to, a count per shaft layout that also filters, and the log itself
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

	// one line of the log: a visit, or the day that the visits under it share
	private record Entry(ShaftVisit visit, String day, int shafts) {
	}

	// brief is the label for when the tile is too narrow for the full one
	private record Tile(String value, int color, String note, String label, String brief, String help) {
	}

	// the period stays as it was left while the game runs
	private static JournalPeriod period = JournalPeriod.ALL;

	private final Screen parent;
	private final JournalStore store = FilobulusClient.journal();
	private final ZoneId zone = ZoneId.systemDefault();
	// layout keys the log is narrowed to, empty shows every shaft
	private final Set<String> picked = new LinkedHashSet<>();
	private int groupScroll;
	private int logScroll;
	private int logLeft;
	private String status = "";

	// counted again only when something changed
	private int countedVersion = -1;
	private JournalPeriod countedPeriod;
	private Set<String> countedPicked = Set.of();
	private long countedMinute;
	private JournalStats stats = JournalStats.of(List.of());
	private List<ShaftVisit> shown = List.of();
	private List<Entry> log = List.of();

	public JournalScreen(Screen parent) {
		super(Component.literal("Filobulus journal"));
		this.parent = parent;
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		long now = System.currentTimeMillis();
		count(now);
		int panelWidth = Math.min(width - MARGIN * 2, MAX_WIDTH);
		int panelHeight = Math.min(height - MARGIN * 2, MAX_HEIGHT);
		int left = (width - panelWidth) / 2;
		int top = (height - panelHeight) / 2;
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		panel(graphics, left, top, panelWidth, panelHeight);

		graphics.text(font, Component.literal("Shaft journal").withStyle(ChatFormatting.BOLD), left + PAD, top + 9, ACCENT, true);
		int x = right - PAD;
		JournalPeriod[] periods = JournalPeriod.values();
		for (int i = periods.length - 1; i >= 0; i--) {
			JournalPeriod each = periods[i];
			x -= buttonWidth(each.title());
			chip(graphics, x, top + 6, each.title(), each == period, mouseX, mouseY, () -> period = each);
			x -= 2;
		}
		drawTiles(graphics, left + PAD, top + TILES_TOP, panelWidth - PAD * 2, mouseX, mouseY);

		int bodyTop = top + BODY_TOP;
		int bodyBottom = bottom - FOOTER;
		if (store.journal().visits().isEmpty()) {
			int middle = (bodyTop + bodyBottom) / 2;
			graphics.centeredText(font, "No shafts yet.", width / 2, middle - 10, MUTED);
			graphics.centeredText(font, "Every mineshaft you enter is written down here by itself.", width / 2, middle + 2, FAINT);
		} else {
			int groupsWidth = Math.min(215, (panelWidth - PAD * 3) * 9 / 20);
			logLeft = left + PAD + groupsWidth + PAD;
			drawGroups(graphics, left + PAD, bodyTop, groupsWidth, bodyBottom, mouseX, mouseY);
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
		graphics.text(font, clip(status.isEmpty() ? summary(now) : status, copyLeft - left - PAD * 2),
				left + PAD, y + 3, status.isEmpty() ? FAINT : GOOD, true);
	}

	private void drawTiles(GuiGraphicsExtractor graphics, int left, int top, int width, int mouseX, int mouseY) {
		String average = stats.timed() == 0 ? "-" : JournalText.duration(stats.averageTime());
		List<Tile> tiles = List.of(
				new Tile(String.valueOf(stats.shafts()), TEXT, "", "Shafts", "Shafts",
						"Mineshafts you entered in this period."),
				new Tile(String.valueOf(stats.jasper()), ShaftColors.of(ShaftMaterial.JASPER),
						stats.shafts() == 0 ? "" : JournalText.percent(stats.jasperRate()), "Jasper", "Jasper",
						"Jasper shafts in this period and their share of all shafts. Jasper crystal shafts do not count."),
				new Tile(String.valueOf(store.journal().sinceJasper()), TEXT, "", "Since jasper", "Dry streak",
						"Shafts entered since your last jasper shaft. This one always counts up to now, whatever the period."),
				new Tile(String.valueOf(stats.longestDry()), TEXT, "", "Longest dry", "Max dry",
						"The most shafts in a row without a jasper one in this period."),
				new Tile(JournalText.duration(stats.timeInside()), TEXT, "", "Time inside", "Inside",
						"Time spent inside shafts in this period."),
				new Tile(average, TEXT, "", "Avg time", "Avg", "Average time inside one shaft."));
		int tileWidth = (width - GAP * (tiles.size() - 1)) / tiles.size();
		for (int i = 0; i < tiles.size(); i++) {
			Tile tile = tiles.get(i);
			int x = left + i * (tileWidth + GAP);
			graphics.fill(x, top, x + tileWidth, top + TILE_HEIGHT, BUTTON);
			// the number is drawn big when the tile has room for it
			int noteWidth = tile.note().isEmpty() ? 0 : font.width(tile.note()) + 4;
			float scale = font.width(tile.value()) * BIG + noteWidth <= tileWidth - 10 ? BIG : 1f;
			graphics.pose().pushMatrix();
			graphics.pose().translate(x + 5, top + (scale == BIG ? 5 : 8));
			graphics.pose().scale(scale, scale);
			graphics.text(font, tile.value(), 0, 0, tile.color(), true);
			graphics.pose().popMatrix();
			graphics.text(font, tile.note(), x + 9 + Math.round(font.width(tile.value()) * scale), top + 9, MUTED, true);
			String label = font.width(tile.label()) <= tileWidth - 8 ? tile.label() : tile.brief();
			graphics.text(font, label, x + 5, top + 20, FAINT, true);
			if (mouseX >= x && mouseX < x + tileWidth && mouseY >= top && mouseY < top + TILE_HEIGHT) {
				tooltip(graphics, tile.help(), mouseX, mouseY);
			}
		}
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
			graphics.text(font, "Nothing in this period.", left, listTop + 4, FAINT, true);
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
		String head = picked.isEmpty() ? "Log" : "Log: " + String.join(", ", picked.stream().map(JournalScreen::title).toList());
		int headRoom = atRight - 30 - left - (picked.isEmpty() ? 0 : buttonWidth("Clear") + GAP);
		graphics.text(font, clip(head, headRoom), left, top, FAINT, true);
		if (!picked.isEmpty()) {
			int clearLeft = left + Math.min(font.width(head), headRoom) + GAP;
			button(graphics, clearLeft, top - 3, buttonWidth("Clear"), "Clear", MUTED, mouseX, mouseY, picked::clear);
		}
		rightText(graphics, "At", atRight, top, FAINT);
		rightText(graphics, "Inside", insideRight, top, FAINT);
		rightText(graphics, "Route", right, top, FAINT);
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
			}
			type.ifPresent(known -> smallItem(graphics, known, left + 2, y + 1));
			graphics.text(font, clip(type.map(ShaftType::displayName).orElse("Unknown"), atRight - 34 - left - 15),
					left + 15, y + 3, type.map(known -> ShaftColors.of(known.material())).orElse(MUTED), true);
			rightText(graphics, JournalText.clock(visit.enteredAt(), zone), atRight, y + 3, MUTED);
			boolean inside = open != null && open.enteredAt() == visit.enteredAt();
			String spent = inside ? "inside" : visit.spentMillis() > 0 ? JournalText.duration(visit.spentMillis()) : "-";
			rightText(graphics, spent, insideRight, y + 3, inside ? GOOD : TEXT);
			rightText(graphics, visit.runMillis() > 0 ? JournalText.duration(visit.runMillis()) : "", right, y + 3, ACCENT);
		}
		graphics.disableScissor();
		scrollbar(graphics, left + width, listTop, bottom, content, logScroll);
	}

	private void smallItem(GuiGraphicsExtractor graphics, ShaftType type, int x, int y) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(10 / 16f, 10 / 16f);
		graphics.item(ShaftIcons.of(type.material()), 0, 0);
		graphics.pose().popMatrix();
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
				&& now / 60_000 == countedMinute) {
			return;
		}
		if (period != countedPeriod) {
			groupScroll = 0;
			logScroll = 0;
			status = "";
		}
		List<ShaftVisit> inPeriod = List.copyOf(store.journal().since(period.start(now, store.sessionStart(), zone)));
		stats = JournalStats.of(inPeriod);
		// a shaft that is not in this period can not stay picked, the log would be empty with no row to click
		picked.retainAll(stats.groups().stream().map(Group::key).toList());
		shown = inPeriod.stream().filter(visit -> picked.isEmpty() || picked.contains(visit.key())).toList();
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
