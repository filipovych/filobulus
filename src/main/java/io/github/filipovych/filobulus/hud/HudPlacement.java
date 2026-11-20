package io.github.filipovych.filobulus.hud;

// where a hud panel sits: an offset in pixels from an anchor on each axis, plus its scale
// sizes passed in are the size on screen, so with the scale already applied
public record HudPlacement(HudAnchor anchorX, int x, HudAnchor anchorY, int y, float scale) {
	public static final float MIN_SCALE = 0.5f;
	public static final float MAX_SCALE = 3f;
	private static final int SNAP_RANGE = 6;

	public HudPlacement {
		scale = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
	}

	public static HudPlacement at(
			int left, int top, int width, int height, int screenWidth, int screenHeight, float scale) {
		HudAnchor anchorX = HudAnchor.nearest(left, width, screenWidth);
		HudAnchor anchorY = HudAnchor.nearest(top, height, screenHeight);
		return new HudPlacement(
				anchorX, anchorX.offset(left, width, screenWidth),
				anchorY, anchorY.offset(top, height, screenHeight), scale);
	}

	public int left(int width, int screenWidth) {
		return anchorX.position(x, width, screenWidth);
	}

	public int top(int height, int screenHeight) {
		return anchorY.position(y, height, screenHeight);
	}

	public HudPlacement withScale(float scale) {
		return new HudPlacement(anchorX, x, anchorY, y, scale);
	}

	// pulls a panel to the screen edge or the screen middle when it is dragged close to one, works per axis
	public static int snap(int position, int size, int screen) {
		int centred = (screen - size) / 2;
		if (Math.abs(position) <= SNAP_RANGE) {
			return 0;
		}
		if (Math.abs(position - (screen - size)) <= SNAP_RANGE) {
			return screen - size;
		}
		return Math.abs(position - centred) <= SNAP_RANGE ? centred : position;
	}
}
