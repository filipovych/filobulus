package io.github.filipovych.filobulus.alert;

import java.util.ArrayList;
import java.util.List;

// the jasper moment as a function of time: frost grows in from the edges, a crack runs across, the title shatters in
// the big one is for a jasper that ends a long dry streak, it lasts longer and throws more shards
// the same time always gives the same frame, so the hud can ask every frame and nothing is stored
public final class JasperTimeline {
	// a dry streak this long gets the big one
	public static final int BIG_DRY = 30;

	public record Frame(float frost, float crack, float crackAlpha, float titleScale, float title, float burst, float flash,
			float subtitle, float dry) {
	}

	// one piece of the title flying out, dx and dy is how far it gets, size in pixels
	public record Shard(float dx, float dy, float spin, int size, boolean white) {
	}

	// a piece of the crack from x1,y1 to x2,y2, everything 0 to 1 of the screen
	// from and to is the part of the crack run where it grows, so it is drawn bit by bit
	public record Segment(float x1, float y1, float x2, float y2, float from, float to, boolean branch) {
	}

	private record Times(long total, long frostIn, long hit, long burst, float startScale) {
	}

	private static final Times NORMAL = new Times(3200, 900, 550, 800, 3.5f);
	private static final Times BIG = new Times(4600, 1300, 750, 1150, 5f);
	private static final long CRACK_START = 150;
	private static final long FADE = 500;
	private static final List<Shard> SHARDS = shards(12, 11);
	private static final List<Shard> BIG_SHARDS = shards(28, 29);
	private static final int CRACK_STEPS = 16;

	private JasperTimeline() {
	}

	public static boolean big(int dry) {
		return dry >= BIG_DRY;
	}

	public static long total(boolean big) {
		return times(big).total();
	}

	// when the title lands, there is a sound there
	public static long hit(boolean big) {
		return times(big).hit();
	}

	public static Frame at(long elapsed, boolean big) {
		Times times = times(big);
		long t = Math.max(0, elapsed);
		long hit = times.hit();
		float fade = 1 - ramp(t, times.total() - FADE, times.total());
		boolean landed = t >= hit;
		float drop = out(ramp(t, hit, hit + 300));
		return new Frame(
				out(ramp(t, 0, times.frostIn())) * fade,
				ramp(t, CRACK_START, hit),
				t > CRACK_START ? fade : 0,
				landed ? 1 + (times.startScale() - 1) * (1 - drop) : 0,
				landed ? ramp(t, hit, hit + 120) * fade : 0,
				landed ? ramp(t, hit, hit + times.burst()) : -1,
				landed ? 1 - ramp(t, hit, hit + 200) : 0,
				ramp(t, hit + 250, hit + 450) * fade,
				big ? ramp(t, hit + 600, hit + 900) * fade : 0);
	}

	// true when the title landed in (from, to], a caller passing its last and current time hears it once
	public static boolean lands(long from, long to, boolean big) {
		long hit = hit(big);
		return from < hit && hit <= to;
	}

	public static List<Shard> shards(boolean big) {
		return big ? BIG_SHARDS : SHARDS;
	}

	// a jagged line from the left edge to the right one with a couple of branches, seed picks which one
	public static List<Segment> crack(int seed) {
		List<Segment> segments = new ArrayList<>();
		float x = 0;
		float y = 0.38f + 0.24f * noise(seed, 0);
		for (int step = 0; step < CRACK_STEPS; step++) {
			float nx = (step + 1) / (float) CRACK_STEPS;
			// small zig zags, pulled back towards the middle so it does not wander off
			float ny = clamp(y + (noise(seed, step + 1) - 0.5f) * 0.12f + (0.5f - y) * 0.15f, 0.2f, 0.8f);
			float from = step / (float) CRACK_STEPS;
			float to = (step + 1) / (float) CRACK_STEPS;
			segments.add(new Segment(x, y, nx, ny, from, to, false));
			// every few steps a short branch splits off up or down
			if (step % 5 == 2) {
				float up = noise(seed, step + 40) < 0.5f ? -1 : 1;
				float bx = nx + 0.03f + 0.03f * noise(seed, step + 60);
				float by = ny + up * (0.06f + 0.05f * noise(seed, step + 80));
				segments.add(new Segment(nx, ny, bx, by, to, Math.min(1, to + 0.12f), true));
				segments.add(new Segment(bx, by, bx + 0.025f, by + up * 0.04f, Math.min(1, to + 0.12f),
						Math.min(1, to + 0.2f), true));
			}
			x = nx;
			y = ny;
		}
		return List.copyOf(segments);
	}

	// a number between 0 and 1 that is always the same for the same seed and index
	public static float noise(int seed, int index) {
		int h = seed * 0x9E3779B1 + index * 0x85EBCA77;
		h ^= h >>> 15;
		h *= 0x2C1B3C6D;
		h ^= h >>> 12;
		h *= 0x297A2D39;
		h ^= h >>> 15;
		return (h >>> 8) / (float) (1 << 24);
	}

	private static List<Shard> shards(int count, int seed) {
		List<Shard> shards = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			// spread round the circle with a bit of wobble, flatter than high so they fly past the sides of the word
			double angle = (i + 0.6 * noise(seed, i)) * (Math.PI * 2 / count);
			float reach = 40 + 50 * noise(seed, i + 100);
			shards.add(new Shard((float) Math.cos(angle) * reach * 1.6f, (float) Math.sin(angle) * reach * 0.8f,
					(noise(seed, i + 200) - 0.5f) * 6, 2 + Math.round(2 * noise(seed, i + 300)), i % 3 == 0));
		}
		return List.copyOf(shards);
	}

	private static Times times(boolean big) {
		return big ? BIG : NORMAL;
	}

	// 0 before from, 1 after to, a straight line between
	private static float ramp(long t, long from, long to) {
		if (t <= from) {
			return 0;
		}
		return t >= to ? 1 : (t - from) / (float) (to - from);
	}

	// fast at first and slow at the end
	private static float out(float x) {
		float u = 1 - x;
		return 1 - u * u * u;
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
