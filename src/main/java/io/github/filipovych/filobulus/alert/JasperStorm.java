package io.github.filipovych.filobulus.alert;

import java.util.ArrayList;
import java.util.List;

// the snow storm round the jasper moment: flakes blown along the edges of the screen and frost in the corners
// the middle of the screen always stays clear, the storm only ever lives in a frame round it
// positions are 0 to 1 of the screen, the hud turns them into pixels
public final class JasperStorm {
	// frost corners are this many cells along each side
	public static final int CELLS = 16;
	// nothing is ever drawn where edge() is below this, a rounded box round the crosshair
	public static final float CLEAR = 0.5f;
	// the storm pushes in this far from the edges at the height of the gust
	private static final float REACH_CALM = 0.8f;
	private static final float REACH_GUST = 0.6f;
	private static final float SOFT = 0.25f;
	private static final int FLAKES = 170;
	private static final int BIG_FLAKES = 240;
	private static final int STREAKS = 9;
	private static final int BIG_STREAKS = 13;

	public enum Tint {
		WHITE,
		ICE,
		PINK
	}

	// keep is the gust strength under which this flake is gone, so the storm thins out and does not just fade
	public record Flake(float x, float y, float speed, float fall, float wobble, int size, Tint tint, float keep) {
	}

	// a flake or a wind streak where it is right now, tail is how far behind it the streak reaches
	public record Spot(float x, float y, float alpha, float tail, int size, Tint tint) {
	}

	// one bit of corner frost, dx and dy are cells away from the corner, it is there once the frost is past at
	public record Cell(int dx, int dy, float at, float shade, boolean glint) {
	}

	private JasperStorm() {
	}

	public static List<Flake> flakes(int seed, boolean big) {
		List<Flake> flakes = new ArrayList<>();
		int count = big ? BIG_FLAKES : FLAKES;
		for (int i = 0; i < count; i++) {
			float roll = JasperTimeline.noise(seed, i + 500);
			// mostly tiny ones like the game's snow, a few bigger and some jasper pink in the white
			int size = roll < 0.55f ? 1 : roll < 0.88f ? 2 : 3;
			float hue = JasperTimeline.noise(seed, i + 600);
			Tint tint = hue < 0.14f ? Tint.PINK : hue < 0.4f ? Tint.ICE : Tint.WHITE;
			flakes.add(new Flake(JasperTimeline.noise(seed, i), JasperTimeline.noise(seed, i + 100),
					0.55f + 0.8f * JasperTimeline.noise(seed, i + 200), 0.1f + 0.25f * JasperTimeline.noise(seed, i + 300),
					JasperTimeline.noise(seed, i + 400) * (float) (Math.PI * 2), size, tint,
					0.85f * JasperTimeline.noise(seed, i + 700)));
		}
		return List.copyOf(flakes);
	}

	// every flake and streak that shows at this gust and drift, the wind blows from the left
	public static List<Spot> spots(List<Flake> flakes, int seed, float gust, float drift) {
		List<Spot> spots = new ArrayList<>();
		if (gust <= 0) {
			return spots;
		}
		for (Flake flake : flakes) {
			float there = clamp((gust - flake.keep()) / 0.15f, 0, 1);
			if (there <= 0) {
				continue;
			}
			float x = frac(flake.x() + flake.speed() * drift);
			float y = frac(flake.y() + flake.fall() * drift + 0.015f * (float) Math.sin(drift * 9 + flake.wobble()));
			float alpha = there * edge(x, y, gust);
			if (alpha > 0) {
				spots.add(new Spot(x, y, alpha, 0.03f * flake.speed() * gust * gust, flake.size(), flake.tint()));
			}
		}
		// a few long thin streaks of wind along the top and the bottom, only while the gust is strong
		int streaks = flakes.size() > FLAKES ? BIG_STREAKS : STREAKS;
		for (int i = 0; i < streaks; i++) {
			float lane = JasperTimeline.noise(seed, i + 900) * 0.1f;
			float y = i % 2 == 0 ? 0.02f + lane : 0.88f + lane;
			float x = frac(JasperTimeline.noise(seed, i + 1000) + (2.2f + JasperTimeline.noise(seed, i + 1100)) * drift);
			float alpha = 0.6f * clamp((gust - 0.4f) / 0.6f, 0, 1) * edge(x, y, gust);
			if (alpha > 0) {
				spots.add(new Spot(x, y, alpha, 0.1f + 0.1f * JasperTimeline.noise(seed, i + 1200), 1, Tint.WHITE));
			}
		}
		return spots;
	}

	// how much of the storm shows at a point, 0 in the middle and 1 near the edges, more of the screen at a strong gust
	public static float edge(float x, float y, float gust) {
		float r = far(x, y);
		if (r < CLEAR) {
			return 0;
		}
		float inner = Math.max(CLEAR, REACH_CALM + (REACH_GUST - REACH_CALM) * clamp(gust, 0, 1));
		float t = clamp((r - inner) / SOFT, 0, 1);
		return t * t * (3 - 2 * t);
	}

	// how far out a point is, 0 in the middle, 1 on the middle of an edge and more in a corner
	// a rounded box and not a circle, so the clear part follows the shape of the screen
	public static float far(float x, float y) {
		float nx = Math.abs(x - 0.5f) * 2;
		float ny = Math.abs(y - 0.5f) * 2;
		return (float) Math.pow(Math.pow(nx, 4) + Math.pow(ny, 4), 0.25);
	}

	// the frost in one corner, thick in the corner itself and creeping out along both edges
	public static List<Cell> frost(int seed) {
		List<Cell> cells = new ArrayList<>();
		for (int dx = 0; dx < CELLS; dx++) {
			for (int dy = 0; dy < CELLS; dy++) {
				int index = dx * CELLS + dy;
				float away = (float) Math.hypot(dx + 0.5, dy + 0.5) / CELLS;
				// 0 right on an edge, 1 on the diagonal, the frost reaches further along the edges
				float off = Math.min(dx, dy) / (float) Math.max(1, Math.max(dx, dy));
				float reach = 1 - 0.5f * off;
				float at = away / reach * (0.75f + 0.5f * JasperTimeline.noise(seed, index));
				if (at >= 1) {
					continue;
				}
				cells.add(new Cell(dx, dy, at, 1 - 0.7f * at, JasperTimeline.noise(seed, index + 3000) < 0.1f));
			}
		}
		return List.copyOf(cells);
	}

	// a pink light running out through the frost when the title lands, 0 to 1 for a cell at that point of the frost
	public static float shine(float at, long elapsed, boolean big) {
		float wave = (elapsed - JasperTimeline.hit(big)) / 500f - at;
		return clamp(1 - Math.abs(wave) * 4, 0, 1);
	}

	private static float frac(float value) {
		return value - (float) Math.floor(value);
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
