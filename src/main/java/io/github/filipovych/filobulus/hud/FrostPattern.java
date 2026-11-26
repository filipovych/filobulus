package io.github.filipovych.filobulus.hud;

import io.github.filipovych.filobulus.alert.JasperTimeline;
import java.util.ArrayList;
import java.util.List;

// the frost along the edge of a panel: little icy ticks that grow inwards from the border, thicker in the corners
// made from the panel name, so a panel always gets the same frost and nothing flickers
public final class FrostPattern {
	public static final long GLINT_MILLIS = 900;

	// a tick is a small rectangle, phase is where its shimmer starts and bright how strong it is at most
	public record Tick(int x, int y, int width, int height, float phase, float bright) {
	}

	// how far apart two ticks are along an edge
	private static final int SPACING = 3;
	// this close to a corner the frost is thicker
	private static final int CORNER = 12;
	// how far the crust in a corner reaches
	private static final int CRUST = 6;

	private FrostPattern() {
	}

	public static List<Tick> ticks(String name, int width, int height) {
		int seed = name.hashCode();
		List<Tick> ticks = new ArrayList<>();
		if (width < 4 || height < 4) {
			return ticks;
		}
		int index = 0;
		for (int x = 0; x < width; x += SPACING) {
			// the top gets the most frost, like it settled there
			add(ticks, seed, index++, x, 0, width, height, Side.TOP, x, width);
			add(ticks, seed, index++, x, height - 1, width, height, Side.BOTTOM, x, width);
		}
		for (int y = SPACING; y < height - SPACING; y += SPACING) {
			add(ticks, seed, index++, 0, y, width, height, Side.LEFT, y, height);
			add(ticks, seed, index++, width - 1, y, width, height, Side.RIGHT, y, height);
		}
		corner(ticks, seed, 0, 0, 1, 1);
		corner(ticks, seed, width - 1, 0, -1, 1);
		corner(ticks, seed, 0, height - 1, 1, -1);
		corner(ticks, seed, width - 1, height - 1, -1, -1);
		return List.copyOf(ticks);
	}

	// a little crust of single pixels in a corner, thinning out away from it
	private static void corner(List<Tick> ticks, int seed, int x, int y, int stepX, int stepY) {
		int salt = 20_000 + x * 7 + y * 13;
		for (int dx = 0; dx < CRUST; dx++) {
			for (int dy = 0; dy < CRUST - dx; dy++) {
				// the edge rows are already frosted by the ticks
				if (dx == 0 || dy == 0) {
					continue;
				}
				int index = salt + dx * CRUST + dy;
				if (JasperTimeline.noise(seed, index) > 0.8f - (dx + dy) * 0.12f) {
					continue;
				}
				float phase = JasperTimeline.noise(seed, index + 7000) * (float) (Math.PI * 2);
				float bright = 0.9f - (dx + dy) * 0.08f;
				ticks.add(new Tick(x + dx * stepX, y + dy * stepY, 1, 1, phase, bright));
			}
		}
	}

	// where a glint is going over its panel, 0 to 1 from the left to the right, -1 when it is over
	public static float glint(long elapsed) {
		if (elapsed < 0 || elapsed >= GLINT_MILLIS) {
			return -1;
		}
		float x = elapsed / (float) GLINT_MILLIS;
		// slow in, fast through the middle, slow out
		return x * x * (3 - 2 * x);
	}

	// how bright a tick is at a moment, a slow sway between a third and all of its brightness
	public static float shimmer(Tick tick, long now) {
		double wave = Math.sin(now / 900.0 + tick.phase()) * 0.5 + 0.5;
		return tick.bright() * (float) (0.35 + 0.65 * wave);
	}

	private enum Side {
		TOP,
		BOTTOM,
		LEFT,
		RIGHT
	}

	private static void add(List<Tick> ticks, int seed, int index, int x, int y, int width, int height, Side side,
			int along, int length) {
		float roll = JasperTimeline.noise(seed, index);
		boolean corner = along < CORNER || along >= length - CORNER;
		// some places stay bare, it is frost and not a frame
		float bare = switch (side) {
			case TOP -> 0.25f;
			case BOTTOM -> 0.55f;
			case LEFT, RIGHT -> 0.5f;
		};
		if (!corner && roll < bare) {
			return;
		}
		int deep = 1 + Math.round(JasperTimeline.noise(seed, index + 5000) * (corner ? 3 : side == Side.TOP ? 2 : 1));
		int thick = JasperTimeline.noise(seed, index + 9000) < 0.3f ? 2 : 1;
		float phase = JasperTimeline.noise(seed, index + 7000) * (float) (Math.PI * 2);
		float bright = 0.45f + 0.55f * JasperTimeline.noise(seed, index + 3000);
		switch (side) {
			case TOP -> ticks.add(new Tick(Math.min(x, width - thick), 0, thick, Math.min(deep, height), phase, bright));
			case BOTTOM -> ticks.add(new Tick(Math.min(x, width - thick), height - deep, thick, deep, phase, bright));
			case LEFT -> ticks.add(new Tick(0, Math.min(y, height - thick), deep, thick, phase, bright));
			case RIGHT -> ticks.add(new Tick(width - deep, Math.min(y, height - thick), deep, thick, phase, bright));
		}
	}
}
