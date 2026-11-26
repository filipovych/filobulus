package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.hud.FrostPattern;
import io.github.filipovych.filobulus.hud.FrostPattern.Tick;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

// the frosted look of the panels: icy ticks along the edges and now and then a glint of light going over one
// Hud draws it over every panel when frosted panels is on, anything can ask for a glint
public final class Frost {
	// asks every panel for a glint at once
	public static final String ALL = "*";
	// rgb
	private static final int ICE = 0xD8F2FF;
	private static final int ICE_DEEP = 0x8FD0FF;
	private static final int GLINT_WIDTH = 14;
	private static final int MIN_ALPHA = 6;

	private record Pattern(int width, int height, List<Tick> ticks) {
	}

	// held is the moment a glint is frozen on for a picture, -1 while it moves
	private record Glint(int color, long at, long held) {
		long elapsed(long now) {
			return held >= 0 ? held : now - at;
		}
	}

	private static final Map<String, Pattern> PATTERNS = new HashMap<>();
	private static final Map<String, Glint> GLINTS = new HashMap<>();
	private static Glint all;

	private Frost() {
	}

	// a short light sweep over the panel with that widget name, or over all of them with ALL, color is rgb
	public static void glint(String panel, int color) {
		if (HudConfig.frosted) {
			put(panel, new Glint(color & 0xFFFFFF, System.currentTimeMillis(), -1));
		}
	}

	// a glint that stays on one moment of its sweep, to look at it, a glint call takes it away again
	public static void hold(String panel, int color, long elapsed) {
		put(panel, new Glint(color & 0xFFFFFF, System.currentTimeMillis(), elapsed));
	}

	// no glint anywhere, also takes away the held ones
	public static void clear() {
		GLINTS.clear();
		all = null;
	}

	private static void put(String panel, Glint glint) {
		if (ALL.equals(panel)) {
			all = glint;
		} else {
			GLINTS.put(panel, glint);
		}
	}

	// the panel is drawn at 0,0 in its own size, the matrix is already moved there
	static void draw(GuiGraphicsExtractor graphics, String panel, int width, int height) {
		if (!HudConfig.frosted) {
			return;
		}
		long now = System.currentTimeMillis();
		for (Tick tick : pattern(panel, width, height).ticks()) {
			int alpha = Math.round(FrostPattern.shimmer(tick, now) * 200);
			if (alpha < MIN_ALPHA) {
				continue;
			}
			// the long ones are a bit bluer, the tips are near white
			int rgb = tick.width() + tick.height() > 3 ? ICE_DEEP : ICE;
			graphics.fill(tick.x(), tick.y(), tick.x() + tick.width(), tick.y() + tick.height(), alpha << 24 | rgb);
		}
		Glint glint = newer(live(GLINTS.get(panel), now), live(all, now));
		if (glint != null) {
			drawGlint(graphics, width, height, FrostPattern.glint(glint.elapsed(now)), glint.color());
		}
	}

	// a band at 45 degrees that goes from left to right, a soft wide one with a bright thin core
	private static void drawGlint(GuiGraphicsExtractor graphics, int width, int height, float at, int color) {
		// front is where x + y meets the leading edge, it starts left of the panel and ends right of it
		int front = Math.round(at * (width + height + GLINT_WIDTH));
		// bright in the middle of its run, soft as it comes in and goes out
		float strength = (float) Math.sin(at * Math.PI);
		if (strength * 60 < MIN_ALPHA) {
			return;
		}
		int soft = Math.round(strength * 60) << 24 | color;
		int core = Math.round(strength * 120) << 24 | 0xFFFFFF;
		for (int x = Math.max(0, front - GLINT_WIDTH - height); x < Math.min(width, front + 1); x++) {
			// one fill per column, covering the y where x + y is inside the band
			int top = Math.max(0, front - GLINT_WIDTH - x + 1);
			int bottom = Math.min(height, front - x + 1);
			if (top < bottom) {
				graphics.fill(x, top, x + 1, bottom, soft);
			}
			int coreTop = Math.max(0, front - GLINT_WIDTH / 2 - 1 - x);
			int coreBottom = Math.min(height, front - GLINT_WIDTH / 2 + 1 - x);
			if (coreTop < coreBottom) {
				graphics.fill(x, coreTop, x + 1, coreBottom, core);
			}
		}
	}

	private static Glint live(Glint glint, long now) {
		return glint == null || FrostPattern.glint(glint.elapsed(now)) < 0 ? null : glint;
	}

	private static Glint newer(Glint one, Glint other) {
		if (one == null || other == null) {
			return one == null ? other : one;
		}
		return one.at() >= other.at() ? one : other;
	}

	// the ticks are only worked out again when the panel changes size
	private static Pattern pattern(String panel, int width, int height) {
		Pattern pattern = PATTERNS.get(panel);
		if (pattern == null || pattern.width() != width || pattern.height() != height) {
			pattern = new Pattern(width, height, FrostPattern.ticks(panel, width, height));
			PATTERNS.put(panel, pattern);
		}
		return pattern;
	}
}
