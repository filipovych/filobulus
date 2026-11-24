package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.journal.JournalStore;
import io.github.filipovych.filobulus.journal.JournalPeriod;
import io.github.filipovych.filobulus.journal.JournalStats;
import io.github.filipovych.filobulus.journal.JournalStats.Group;
import io.github.filipovych.filobulus.journal.JournalText;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
	private static final int TILES_TOP = 28;
	private static final int TILE_HEIGHT = 32;
	private static final int BODY_TOP = TILES_TOP + TILE_HEIGHT + 10;
	private static final int FOOTER = 26;
	// argb
	private static final int CHIP_ON = 0xFF2F86B3;

	// one line of the log: a visit, or the day that the visits under it share
	private record Entry(ShaftVisit visit, String day, int shafts) {
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

		int bodyTop = top + BODY_TOP;
		int bodyBottom = bottom - FOOTER;
		if (store.journal().visits().isEmpty()) {
			int middle = (bodyTop + bodyBottom) / 2;
			graphics.centeredText(font, "No shafts yet.", width / 2, middle - 10, MUTED);
			graphics.centeredText(font, "Every mineshaft you enter is written down here by itself.", width / 2, middle + 2, FAINT);
		}

		int y = bottom - BUTTON_HEIGHT - 6;
		int doneWidth = buttonWidth("Done");
		button(graphics, right - PAD - doneWidth, y, doneWidth, "Done", ACCENT, mouseX, mouseY, this::onClose);
		graphics.text(font, clip(summary(now), right - PAD * 3 - doneWidth - left), left + PAD, y + 3, FAINT, true);
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

	private static String shafts(int count) {
		return count == 1 ? "1 shaft" : count + " shafts";
	}

	@Override
	public void onClose() {
		GameCompat.setScreen(minecraft, parent);
	}
}
