package io.github.filipovych.filobulus.corpse;

import java.util.ArrayList;
import java.util.List;

// the corpse reveal as a function of time: the ice pops in, cracks, shatters and shows what was inside
// the same time always gives the same frame, so the hud can ask every frame and nothing is stored
public final class CrackTimeline {
	public static final long TOTAL = 4600;

	public record Frame(float block, float shakeX, float shakeY, int cracks, float burst, float flash, float reveal, float text) {
	}

	public enum Cue {
		CRACK,
		SHATTER,
		REVEAL
	}

	public record Sound(Cue cue, int step) {
	}

	private static final long POP = 250;
	// one time per block breaking stage, the gaps get shorter
	private static final long[] CRACKS = {450, 820, 1150, 1440, 1700, 1930, 2130, 2310, 2470, 2610};
	private static final long SHATTER = 2700;
	private static final long REVEAL = 2850;
	private static final long SETTLED = 3050;
	private static final long KICK = 120;

	private static final long[] CUE_TIMES = new long[CRACKS.length + 2];
	private static final List<Sound> SOUNDS = buildSounds();

	private CrackTimeline() {
	}

	private static List<Sound> buildSounds() {
		List<Sound> sounds = new ArrayList<>();
		for (int step = 0; step < CRACKS.length; step++) {
			CUE_TIMES[step] = CRACKS[step];
			sounds.add(new Sound(Cue.CRACK, step));
		}
		CUE_TIMES[CRACKS.length] = SHATTER;
		sounds.add(new Sound(Cue.SHATTER, 0));
		CUE_TIMES[CRACKS.length + 1] = REVEAL;
		sounds.add(new Sound(Cue.REVEAL, 0));
		return List.copyOf(sounds);
	}

	public static Frame at(long elapsed) {
		long t = Math.max(0, elapsed);
		boolean whole = t < SHATTER;
		int cracks = cracks(t);
		float shakeX = 0;
		float shakeY = 0;
		if (whole && cracks >= 0) {
			float strength = 0.2f * (cracks + 1);
			long since = t - CRACKS[cracks];
			if (since < KICK) {
				strength += (1.5f + 0.15f * cracks) * (1 - since / (float) KICK);
			}
			shakeX = strength * wobble(t, 0.113, 0.047, 0);
			shakeY = strength * wobble(t, 0.097, 0.061, 1.9);
		}
		float gone = ramp(t, TOTAL - 300, TOTAL);
		return new Frame(
				whole ? back(ramp(t, 0, POP), 1.70158f) : 0,
				shakeX,
				shakeY,
				cracks,
				whole ? -1 : ramp(t, SHATTER, SHATTER + 700),
				whole ? 0 : 1 - ramp(t, SHATTER, SHATTER + 180),
				whole ? 0 : back(ramp(t, SHATTER, SETTLED), 3) * (1 - gone),
				ramp(t, 2900, 3100) * (1 - ramp(t, TOTAL - 400, TOTAL)));
	}

	// the sounds with a time in (from, to], so a caller that passes its last and its current time gets each one once
	public static List<Sound> cues(long from, long to) {
		int first = 0;
		while (first < CUE_TIMES.length && CUE_TIMES[first] <= from) {
			first++;
		}
		int end = first;
		while (end < CUE_TIMES.length && CUE_TIMES[end] <= to) {
			end++;
		}
		return first == end ? List.of() : SOUNDS.subList(first, end);
	}

	// the last stage stays after the shatter, the block is gone by then anyway
	private static int cracks(long t) {
		int stage = -1;
		while (stage + 1 < CRACKS.length && CRACKS[stage + 1] <= t) {
			stage++;
		}
		return stage;
	}

	// 0 before from, 1 after to, a straight line between
	private static float ramp(long t, long from, long to) {
		if (t <= from) {
			return 0;
		}
		return t >= to ? 1 : (t - from) / (float) (to - from);
	}

	// goes from 0 to 1 and swings past 1 on the way, more with a bigger overshoot: 1.70158 gives 10%, 3 gives 25%
	private static float back(float x, float overshoot) {
		if (x <= 0) {
			return 0;
		}
		if (x >= 1) {
			return 1;
		}
		float u = x - 1;
		return 1 + (overshoot + 1) * u * u * u + overshoot * u * u;
	}

	// two sines of different speed so it does not look like a loop, between -1 and 1
	private static float wobble(long t, double fast, double slow, double phase) {
		return (float) (0.6 * Math.sin(t * fast + phase) + 0.4 * Math.sin(t * slow + phase * 2));
	}
}
