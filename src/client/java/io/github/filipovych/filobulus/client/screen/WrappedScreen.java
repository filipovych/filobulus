package io.github.filipovych.filobulus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.journal.MonthlyWrapped;
import io.github.filipovych.filobulus.journal.MonthlyWrapped.Card;
import io.github.filipovych.filobulus.journal.MonthlyWrapped.Unit;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

// /filo wrapped: a month of the journal one card at a time, a title card first and everything on one card at the end
public final class WrappedScreen extends FlatScreen {
	private static final int WIDTH = 300;
	private static final int HEIGHT = 200;
	private static final int PAD = 10;
	private static final long COUNT_MILLIS = 600;
	private static final int SLIDE = 36;
	private static final float BIG = 3f;
	private static final int ROW = 11;
	private static final int DOT = 4;
	private static final int COINS = 0xFFFFAA00;
	private static final int TRACK = 0x16FFFFFF;
	private static final int[] CORPSE_COLORS = {0xFF4A7BFF, 0xFF8C9AA6, 0xFFC8743A, 0xFFFFD75E};

	private final Screen parent;
	private final MonthlyWrapped wrapped;
	private final String monthTitle;
	// 0 is the title card, then the month's cards, the last one is the summary
	private int index;
	private long shownAt = System.currentTimeMillis();
	// which way the card slides in, 1 from the right
	private int direction = 1;

	public WrappedScreen(Screen parent, YearMonth month) {
		super(Component.literal("Filobulus wrapped"));
		this.parent = parent;
		this.wrapped = MonthlyWrapped.of(FilobulusClient.journal().journal().visits(), month, System.currentTimeMillis(),
				ZoneId.systemDefault());
		this.monthTitle = MonthlyWrapped.monthName(month) + " " + month.getYear();
	}

	private int count() {
		return wrapped.cards().isEmpty() ? 1 : wrapped.cards().size() + 2;
	}

	private void go(int step) {
		int next = Math.max(0, Math.min(count() - 1, index + step));
		if (next != index) {
			direction = step > 0 ? 1 : -1;
			index = next;
			shownAt = System.currentTimeMillis();
		}
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int panelWidth = Math.min(width - 16, WIDTH);
		int panelHeight = Math.min(height - 16, HEIGHT);
		int left = (width - panelWidth) / 2;
		int top = (height - panelHeight) / 2;
		int right = left + panelWidth;
		int bottom = top + panelHeight;
		// a click anywhere goes on, a right click goes back, the buttons drawn later sit on top of this
		clickable(0, 0, width, height, () -> go(1), () -> go(-1));
		panel(graphics, left, top, panelWidth, panelHeight);

		Component header = Component.literal(monthTitle + " Wrapped").withStyle(ChatFormatting.BOLD);
		graphics.text(font, header, left + PAD, top + 8, ACCENT, true);
		if (count() > 1) {
			String where = (index + 1) + "/" + count();
			graphics.text(font, where, right - PAD - font.width(where), top + 8, FAINT, true);
		}

		// eased so it settles softly, the number counts up over the same time
		double t = Math.min(1, (System.currentTimeMillis() - shownAt) / (double) COUNT_MILLIS);
		double eased = 1 - Math.pow(1 - t, 3);
		int alpha = (int) Math.round(255 * eased);
		int shift = (int) Math.round((1 - eased) * SLIDE * direction);
		int middle = width / 2 + shift;
		int bodyTop = top + 26;
		int bodyBottom = bottom - BUTTON_HEIGHT - 16;

		if (wrapped.cards().isEmpty()) {
			centered(graphics, "Nothing to wrap", middle, (bodyTop + bodyBottom) / 2 - 22, MUTED, alpha);
			big(graphics, "0 shafts", middle, (bodyTop + bodyBottom) / 2 - 10, FAINT, alpha);
			centered(graphics, "No shafts in " + monthTitle + " yet.", middle, (bodyTop + bodyBottom) / 2 + 22, FAINT, alpha);
		} else if (index == 0) {
			centered(graphics, "Your mineshaft month", middle, bodyTop + 22, MUTED, alpha);
			big(graphics, MonthlyWrapped.monthName(wrapped.month()), middle, bodyTop + 36, ACCENT, alpha);
			wrap(graphics, wrapped.shafts() + (wrapped.shafts() == 1 ? " shaft" : " shafts")
					+ " in your journal. Click or press space to begin.", middle, bodyTop + 72, panelWidth - PAD * 4, FAINT, alpha);
		} else if (index == count() - 1) {
			summary(graphics, left, bodyTop, panelWidth, bodyBottom, shift, t);
		} else {
			card(graphics, wrapped.cards().get(index - 1), middle, bodyTop, panelWidth, eased, alpha);
		}

		dots(graphics, width / 2, bottom - BUTTON_HEIGHT - 10);
		footer(graphics, left, right, bottom, mouseX, mouseY);
	}

	private void card(GuiGraphicsExtractor graphics, Card card, int middle, int top, int panelWidth, double eased, int alpha) {
		Optional<ShaftMaterial> material = material(card.code());
		int titleWidth = font.width(card.title());
		// an item can not be faded, so its icon only comes once the card is nearly in
		if (alpha > 200 && !card.code().isEmpty()) {
			ItemStack icon = ShaftType.parse(card.code()).map(type -> ShaftIcons.of(type.material()))
					.or(() -> ShaftLayout.byKey(card.code()).map(ShaftIcons::of))
					.orElse(ItemStack.EMPTY);
			graphics.item(icon, middle - titleWidth / 2 - 20, top + 14);
		}
		centered(graphics, card.title(), middle, top + 18, MUTED, alpha);
		big(graphics, card.value(eased), middle, top + 40, color(card, material), alpha);
		int y = wrap(graphics, card.line(), middle, top + 76, panelWidth - PAD * 4, FAINT, alpha);
		if (card.unit() == Unit.COUNT && card.parts().size() == 4) {
			corpseBar(graphics, card, middle, y + 6, Math.min(160, panelWidth - PAD * 4), eased);
		}
	}

	private static Optional<ShaftMaterial> material(String code) {
		return ShaftType.parse(code).map(ShaftType::material).or(() -> ShaftLayout.byKey(code).flatMap(ShaftLayout::material));
	}

	private static int color(Card card, Optional<ShaftMaterial> material) {
		return switch (card.unit()) {
			case COINS -> card.number() < 0 ? BAD : COINS;
			case PERCENT -> card.number() < 0 ? BAD : GOOD;
			default -> material.map(ShaftColors::of).orElse(card.code().isEmpty() ? TEXT : ACCENT);
		};
	}

	// the corpses by type as one bar that fills from the left
	private void corpseBar(GuiGraphicsExtractor graphics, Card card, int middle, int y, int barWidth, double eased) {
		int left = middle - barWidth / 2;
		graphics.fill(left, y, left + barWidth, y + DOT, TRACK);
		int filled = (int) Math.round(barWidth * eased);
		int x = left;
		for (int type = 0; type < 4; type++) {
			int part = (int) Math.round(barWidth * card.parts().get(type) / (double) card.number());
			int end = Math.min(left + filled, x + part);
			if (end > x) {
				graphics.fill(x, y, end, y + DOT, CORPSE_COLORS[type]);
			}
			x += part;
		}
	}

	// every card on one, the rows come in one after the other
	private void summary(GuiGraphicsExtractor graphics, int left, int top, int panelWidth, int bottom, int shift, double t) {
		centered(graphics, monthTitle + " in short", width / 2 + shift, top + 4, MUTED, (int) Math.round(255 * Math.min(1, t * 3)));
		int rowLeft = left + PAD * 2;
		int rowRight = left + panelWidth - PAD * 2;
		int y = top + 20;
		List<Card> cards = wrapped.cards();
		for (int i = 0; i < cards.size() && y + ROW <= bottom; i++, y += ROW) {
			double rowT = Math.max(0, Math.min(1, t * 2 - i * 0.08));
			int alpha = (int) Math.round(255 * rowT);
			if (alpha < 8) {
				continue;
			}
			Card card = cards.get(i);
			String value = card.value();
			int valueWidth = font.width(value);
			graphics.text(font, clip(card.title(), rowRight - rowLeft - valueWidth - 8), rowLeft, y, fade(FAINT, alpha), true);
			graphics.text(font, value, rowRight - valueWidth, y, fade(color(card, material(card.code())), alpha), true);
		}
	}

	private void dots(GuiGraphicsExtractor graphics, int middle, int y) {
		int count = count();
		if (count < 2) {
			return;
		}
		int step = DOT + 3;
		int x = middle - (count * step - 3) / 2;
		for (int i = 0; i < count; i++, x += step) {
			if (i == index) {
				graphics.fill(x, y, x + DOT, y + DOT, ACCENT);
			} else {
				graphics.outline(x, y, DOT, DOT, FAINT);
			}
		}
	}

	private void footer(GuiGraphicsExtractor graphics, int left, int right, int bottom, int mouseX, int mouseY) {
		int y = bottom - BUTTON_HEIGHT - 6;
		int doneWidth = buttonWidth("Done");
		button(graphics, right - PAD - doneWidth, y, doneWidth, "Done", ACCENT, mouseX, mouseY, this::onClose);
		int small = buttonWidth(">");
		if (index > 0) {
			button(graphics, left + PAD, y, small, "<", TEXT, mouseX, mouseY, () -> go(-1));
		}
		if (index < count() - 1) {
			button(graphics, left + PAD + small + 4, y, small, ">", TEXT, mouseX, mouseY, () -> go(1));
		}
	}

	private void centered(GuiGraphicsExtractor graphics, String text, int middle, int y, int color, int alpha) {
		if (alpha >= 8) {
			graphics.centeredText(font, text, middle, y, fade(color, alpha));
		}
	}

	// the big number, smaller when it would not fit
	private void big(GuiGraphicsExtractor graphics, String text, int middle, int y, int color, int alpha) {
		if (alpha < 8) {
			return;
		}
		float room = Math.min(width - 16, WIDTH) - PAD * 4;
		float scale = Math.max(1f, Math.min(BIG, room / Math.max(1, font.width(text))));
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(middle, y + (BIG - scale) * 4);
			graphics.pose().scale(scale, scale);
			graphics.text(font, text, -font.width(text) / 2, 0, fade(color, alpha), true);
		} finally {
			graphics.pose().popMatrix();
		}
	}

	// centred lines that wrap, returns where the next thing goes
	private int wrap(GuiGraphicsExtractor graphics, String text, int middle, int y, int maxWidth, int color, int alpha) {
		for (FormattedCharSequence line : font.split(Component.literal(text), maxWidth)) {
			if (alpha >= 8) {
				graphics.text(font, line, middle - font.width(line) / 2, y, fade(color, alpha), true);
			}
			y += 10;
		}
		return y;
	}

	private static int fade(int color, int alpha) {
		return (alpha << 24) | (color & 0xFFFFFF);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isRight() || event.key() == InputConstants.KEY_SPACE || event.key() == InputConstants.KEY_RETURN) {
			go(1);
		} else if (event.isLeft()) {
			go(-1);
		} else {
			return super.keyPressed(event);
		}
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY == 0) {
			return false;
		}
		go(scrollY > 0 ? -1 : 1);
		return true;
	}

	@Override
	public void onClose() {
		GameCompat.setScreen(minecraft, parent);
	}
}
