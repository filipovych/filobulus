package io.github.filipovych.filobulus.client.mining;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.MiningConfig;
import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.HudWidget;
import io.github.filipovych.filobulus.client.mining.ComboWatch.Shown;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.mining.SwapCombo.Rank;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

// the swap combo on screen: the rank big, a thin meter, the count, and a pop and a shake on every good swap
// all the movement is worked out from the time of the last events, nothing is stored per frame but the meter
public final class ComboHud implements HudWidget {
	private static final int WIDTH = 120;
	private static final int HEIGHT = 38;
	private static final int BAR = 100;
	private static final float NAME_SCALE = 2f;
	private static final long POP_MILLIS = 160;
	private static final long RANK_MILLIS = 320;
	private static final long FADE_MILLIS = 450;
	private static final long BROKEN_MILLIS = 700;
	// rgb
	private static final int WHITE = 0xFFFFFF;
	private static final int FAINT = 0x8C96A3;
	private static final int BROKEN = 0xFF5B6B;
	private static final int BAR_BACK = 0x50000000;
	// text this faint is not worth drawing, and the game shows an alpha of zero as solid
	private static final int MIN_ALPHA = 10;

	// one frame of the combo, every number in it already eased
	private record Frame(Shown shown, float alpha, float pop, float rankPop, float shake, double meter, long now)
			implements HudDrawable {
		@Override
		public int width(Font font) {
			return WIDTH;
		}

		@Override
		public int height() {
			return HEIGHT;
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, Font font) {
			int alpha = Math.round(alpha() * 255);
			if (alpha < MIN_ALPHA) {
				return;
			}
			int a = alpha << 24;
			Rank rank = shown.rank();
			int color = shown.broken() ? BROKEN : mix(nameColor(rank), WHITE, rankPop);
			float shakeX = (float) Math.sin(now / 11d) * shake;
			float shakeY = (float) Math.cos(now / 17d) * shake * 0.6f;

			// the rank name, centred, popping out from its middle
			Component name = Component.literal(rank.title()).withStyle(ChatFormatting.BOLD);
			float scale = Math.min(NAME_SCALE, (WIDTH - 4) / (float) font.width(name))
					* (1 + 0.12f * pop * pop + 0.35f * rankPop * rankPop);
			graphics.pose().pushMatrix();
			try {
				graphics.pose().translate(WIDTH / 2f + shakeX, 9 + shakeY);
				graphics.pose().scale(scale, scale);
				graphics.text(font, name, -font.width(name) / 2, -4, a | color, true);
			} finally {
				graphics.pose().popMatrix();
			}

			int left = (WIDTH - BAR) / 2;
			graphics.fill(left, 21, left + BAR, 23, BAR_BACK);
			int filled = (int) Math.round(BAR * meter);
			if (filled > 0) {
				graphics.fill(left, 21, left + filled, 23, a | color);
			}

			String count = "x" + shown.count();
			float countScale = 1.25f * (1 + 0.25f * pop);
			graphics.pose().pushMatrix();
			try {
				graphics.pose().translate(left + shakeX * 0.5f, 31);
				graphics.pose().scale(countScale, countScale);
				graphics.text(font, count, 0, -4, a | (shown.broken() ? BROKEN : WHITE), true);
			} finally {
				graphics.pose().popMatrix();
			}

			if (shown.best() > 0) {
				String best = "best x" + shown.best();
				graphics.pose().pushMatrix();
				try {
					graphics.pose().translate(left + BAR, 31);
					graphics.pose().scale(0.75f, 0.75f);
					graphics.text(font, best, -font.width(best), -4, (alpha * 3 / 4) << 24 | FAINT, false);
				} finally {
					graphics.pose().popMatrix();
				}
			}
		}

		// the top rank shimmers between pink and white
		private int nameColor(Rank rank) {
			if (!rank.top()) {
				return rank.color();
			}
			return mix(0xFFB0E0, WHITE, (float) (Math.sin(now / 180d) + 1) / 2);
		}
	}

	private double meter;
	private Rank meterRank;
	private long meterAt;
	private int meterCount;

	@Override
	public String name() {
		return "Swap combo";
	}

	@Override
	public boolean enabled() {
		return MiningConfig.swapCombo;
	}

	@Override
	public void setEnabled(boolean on) {
		MiningConfig.swapCombo = on;
	}

	@Override
	public Optional<HudDrawable> content() {
		if (!MiningConfig.swapCombo) {
			return Optional.empty();
		}
		ComboWatch watch = FilobulusClient.combo();
		Optional<Shown> shown = watch.shown();
		if (shown.isEmpty()) {
			return Optional.empty();
		}
		long now = System.currentTimeMillis();
		float alpha = 1;
		float shake = 0;
		Shown state = shown.get();
		if (!watch.active()) {
			long fade = state.broken() ? BROKEN_MILLIS : FADE_MILLIS;
			float gone = (now - watch.endedAt()) / (float) fade;
			if (gone >= 1) {
				return Optional.empty();
			}
			alpha = 1 - gone * gone;
			if (state.broken()) {
				shake = 3 * (1 - gone);
			}
		}
		float pop = ease(now - watch.goodAt(), POP_MILLIS);
		float rankPop = ease(now - watch.rankUpAt(), RANK_MILLIS);
		shake += 3 * rankPop + 0.8f * pop;
		return Optional.of(new Frame(state, alpha, pop, rankPop, shake, smoothMeter(state, now), now));
	}

	// the bar slides to where the meter is, a new rank starts it empty so it fills, a lost rank starts it full so it drains
	private double smoothMeter(Shown shown, long now) {
		// a fresh combo starts the bar empty, whatever the last one ended on
		if (shown.rank() != meterRank || shown.count() < meterCount) {
			meter = meterRank != null && shown.count() >= meterCount && shown.rank().ordinal() < meterRank.ordinal() ? 1 : 0;
			meterRank = shown.rank();
		}
		meterCount = shown.count();
		long dt = Math.min(200, Math.max(0, now - meterAt));
		meterAt = now;
		meter += (shown.meter() - meter) * (1 - Math.exp(-dt / 90d));
		return meter;
	}

	// 1 right after the event, falling to 0 over the time given
	private static float ease(long since, long millis) {
		if (since < 0 || since >= millis) {
			return 0;
		}
		float left = 1 - since / (float) millis;
		return left * left;
	}

	private static int mix(int from, int to, float amount) {
		int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * amount);
		int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * amount);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * amount);
		return r << 16 | g << 8 | b;
	}

	@Override
	public HudDrawable sample() {
		return new Frame(new Shown(Rank.SHATTER, 0.6, 24, 80, false), 1, 0, 0, 0, 0.6, 0);
	}

	@Override
	public HudPlacement placement() {
		return new HudPlacement(MiningConfig.comboHudAnchorX, MiningConfig.comboHudX,
				MiningConfig.comboHudAnchorY, MiningConfig.comboHudY, MiningConfig.comboHudScale);
	}

	@Override
	public void place(HudPlacement placement) {
		MiningConfig.comboHudAnchorX = placement.anchorX();
		MiningConfig.comboHudX = placement.x();
		MiningConfig.comboHudAnchorY = placement.anchorY();
		MiningConfig.comboHudY = placement.y();
		MiningConfig.comboHudScale = placement.scale();
	}

	@Override
	public HudPlacement defaultPlacement() {
		// under the crosshair, below the swap cue and clear of the side panels
		return new HudPlacement(HudAnchor.CENTRE, 0, HudAnchor.CENTRE, 48, 1f);
	}
}
