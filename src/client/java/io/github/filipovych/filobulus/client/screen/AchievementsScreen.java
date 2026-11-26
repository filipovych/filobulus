package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.achievement.Achievement;
import io.github.filipovych.filobulus.achievement.Achievement.Progress;
import io.github.filipovych.filobulus.achievement.AchievementBook;
import io.github.filipovych.filobulus.achievement.AchievementFacts;
import io.github.filipovych.filobulus.achievement.AchievementFilter;
import io.github.filipovych.filobulus.achievement.AchievementFilter.Status;
import io.github.filipovych.filobulus.achievement.AchievementRarity;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.journal.JournalText;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// /filo achievements: every achievement as a card in the colour of its rarity, hidden ones stay ??? until they are unlocked
public final class AchievementsScreen extends FlatScreen {
	private static final int MAX_WIDTH = 440;
	private static final int MAX_HEIGHT = 320;
	private static final int PAD = 8;
	private static final int GAP = 4;
	private static final int LIST_TOP = 48;
	private static final int FOOTER = 24;
	private static final int CARD_HEIGHT = 40;
	private static final int TWO_COLUMNS = 300;
	// argb
	private static final int CARD = 0x18FFFFFF;
	private static final int CARD_HOVER = 0x26FFFFFF;
	private static final int TRACK = 0x22FFFFFF;

	// the filter stays as it was left while the game runs
	private static AchievementFilter filter = AchievementFilter.ALL;

	private final Screen parent;
	private final ZoneId zone = ZoneId.systemDefault();
	private int scroll;

	public AchievementsScreen(Screen parent) {
		super(Component.literal("Filobulus achievements"));
		this.parent = parent;
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		AchievementBook book = FilobulusClient.achievements().book();
		AchievementFacts facts = FilobulusClient.achievements().facts();
		int panelWidth = Math.min(width - 16, MAX_WIDTH);
		int panelHeight = Math.min(height - 16, MAX_HEIGHT);
		int left = (width - panelWidth) / 2;
		int top = (height - panelHeight) / 2;
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		panel(graphics, left, top, panelWidth, panelHeight);

		// title, how many are unlocked, and a bar for all of them
		int total = Achievement.values().length;
		graphics.text(font, Component.literal("Achievements").withStyle(ChatFormatting.BOLD), left + PAD, top + 8, TEXT, true);
		String counter = book.count() + "/" + total;
		graphics.text(font, counter, right - PAD - font.width(counter), top + 8, ACCENT, true);
		bar(graphics, left + PAD, top + 20, panelWidth - PAD * 2, 2, (double) book.count() / total, ACCENT);

		// unlocked or not on the left, rarity on the right
		int x = left + PAD - 4;
		for (Status status : Status.values()) {
			x += tab(graphics, x, top + 27, status.label(), filter.status() == status, mouseX, mouseY,
					() -> { filter = filter.withStatus(status); scroll = 0; });
		}
		rarityButton(graphics, right - PAD, top + 27, mouseX, mouseY);

		List<Achievement> shown = new ArrayList<>();
		for (Achievement achievement : Achievement.values()) {
			if (filter.shows(achievement, book.has(achievement))) {
				shown.add(achievement);
			}
		}
		int listTop = top + LIST_TOP;
		int listBottom = bottom - FOOTER;
		int listWidth = panelWidth - PAD * 2;
		int columns = listWidth >= TWO_COLUMNS ? 2 : 1;
		int cardWidth = (listWidth - GAP * (columns - 1)) / columns;
		int rows = (shown.size() + columns - 1) / columns;
		int content = rows * (CARD_HEIGHT + GAP) - GAP;
		scroll = Math.max(0, Math.min(scroll, content - (listBottom - listTop)));
		if (shown.isEmpty()) {
			graphics.centeredText(font, "Nothing here yet.", width / 2, (listTop + listBottom) / 2 - 4, FAINT);
		}
		boolean inList = mouseY >= listTop && mouseY < listBottom;
		graphics.enableScissor(left, listTop, right, listBottom);
		Achievement hovered = null;
		for (int i = 0; i < shown.size(); i++) {
			int cardLeft = left + PAD + (i % columns) * (cardWidth + GAP);
			int cardTop = listTop + (i / columns) * (CARD_HEIGHT + GAP) - scroll;
			if (cardTop + CARD_HEIGHT < listTop || cardTop > listBottom) {
				continue;
			}
			boolean over = inList && mouseX >= cardLeft && mouseX < cardLeft + cardWidth
					&& mouseY >= cardTop && mouseY < cardTop + CARD_HEIGHT;
			card(graphics, shown.get(i), book, facts, cardLeft, cardTop, cardWidth, over);
			if (over) {
				hovered = shown.get(i);
			}
		}
		graphics.disableScissor();
		scrollbar(graphics, right - 3, listTop, listBottom, content, scroll);
		if (hovered != null) {
			tooltip(graphics, help(hovered, book), mouseX, mouseY);
		}
		footer(graphics, right, bottom, mouseX, mouseY);
	}

	private void card(GuiGraphicsExtractor graphics, Achievement achievement, AchievementBook book, AchievementFacts facts,
			int left, int top, int width, boolean over) {
		boolean unlocked = book.has(achievement);
		int rarity = argb(achievement.rarity().color());
		graphics.fill(left, top, left + width, top + CARD_HEIGHT, over ? CARD_HOVER : CARD);
		graphics.fill(left, top, left + 2, top + CARD_HEIGHT, unlocked ? rarity : 0x60000000 | achievement.rarity().color());
		int inner = width - 14;
		String rarityName = achievement.rarity().displayName();
		int rarityLeft = left + width - 6 - font.width(rarityName);
		graphics.text(font, rarityName, rarityLeft, top + 5, unlocked ? rarity : FAINT, true);
		String title = clip(achievement.title(unlocked), rarityLeft - left - 14);
		Component name = unlocked ? Component.literal(title).withStyle(ChatFormatting.BOLD) : Component.literal(title);
		graphics.text(font, name, left + 7, top + 5, unlocked ? rarity : MUTED, true);
		graphics.text(font, clip(achievement.description(unlocked), inner), left + 7, top + 16, unlocked ? MUTED : FAINT, true);

		int y = top + 28;
		if (unlocked) {
			long at = book.at(achievement);
			String when = "Unlocked " + JournalText.day(at, System.currentTimeMillis(), zone) + " " + JournalText.clock(at, zone);
			graphics.text(font, clip(when, inner), left + 7, y, GOOD, true);
		} else if (achievement.counts() && !achievement.hidden()) {
			Progress progress = achievement.progress(facts);
			String text = achievement.progressText(progress);
			int barWidth = inner - font.width(text) - 6;
			bar(graphics, left + 7, y + 3, barWidth, 3, progress.fraction(), rarity);
			graphics.text(font, text, left + 7 + barWidth + 6, y, MUTED, true);
		} else {
			graphics.text(font, "Locked", left + 7, y, FAINT, true);
		}
	}

	private void bar(GuiGraphicsExtractor graphics, int left, int top, int width, int height, double fraction, int color) {
		if (width <= 0) {
			return;
		}
		graphics.fill(left, top, left + width, top + height, TRACK);
		int filled = (int) Math.round(width * Math.max(0, Math.min(1, fraction)));
		if (filled > 0) {
			graphics.fill(left, top, left + filled, top + height, color);
		}
	}

	// left click goes to the next rarity, right click back
	private void rarityButton(GuiGraphicsExtractor graphics, int right, int top, int mouseX, int mouseY) {
		AchievementRarity rarity = filter.rarity();
		String label = rarity == null ? "Any rarity" : rarity.displayName();
		int buttonWidth = Math.max(buttonWidth("Legendary"), buttonWidth(label));
		int left = right - buttonWidth;
		boolean over = button(graphics, left, top, buttonWidth, label, rarity == null ? MUTED : argb(rarity.color()),
				mouseX, mouseY, null);
		clickable(left, top, buttonWidth, BUTTON_HEIGHT,
				() -> { filter = filter.nextRarity(false); scroll = 0; },
				() -> { filter = filter.nextRarity(true); scroll = 0; });
		if (over) {
			tooltip(graphics, "Show one rarity only. Left click for the next one, right click to go back.", mouseX, mouseY);
		}
	}

	private static String help(Achievement achievement, AchievementBook book) {
		boolean unlocked = book.has(achievement);
		if (achievement.hidden() && !unlocked) {
			return "A hidden " + achievement.rarity().displayName().toLowerCase(Locale.ROOT) + " achievement. Keep mining to find out.";
		}
		return achievement.title() + ": " + achievement.description() + (achievement.hidden() ? " (hidden)" : "");
	}

	private void footer(GuiGraphicsExtractor graphics, int right, int bottom, int mouseX, int mouseY) {
		int y = bottom - BUTTON_HEIGHT - 5;
		int doneWidth = buttonWidth("Done");
		button(graphics, right - PAD - doneWidth, y, doneWidth, "Done", ACCENT, mouseX, mouseY, this::onClose);
		int journalWidth = buttonWidth("Journal");
		if (button(graphics, right - PAD - doneWidth - GAP - journalWidth, y, journalWidth, "Journal", TEXT, mouseX, mouseY,
				() -> GameCompat.setScreen(minecraft, new JournalScreen(this)))) {
			tooltip(graphics, "Every shaft you were in, the achievements are worked out from it.", mouseX, mouseY);
		}
	}

	private static int argb(int rgb) {
		return 0xFF000000 | rgb;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= (int) Math.signum(scrollY) * (CARD_HEIGHT + GAP);
		return true;
	}

	@Override
	public void onClose() {
		GameCompat.setScreen(minecraft, parent);
	}
}
