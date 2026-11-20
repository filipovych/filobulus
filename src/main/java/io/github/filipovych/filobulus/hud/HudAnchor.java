package io.github.filipovych.filobulus.hud;

// the side of the screen an offset is measured from, the same three work for both axes
public enum HudAnchor {
	START,
	CENTRE,
	END;

	// the anchor closest to where the panel sits, so one near the right edge stays there when the window changes
	static HudAnchor nearest(int position, int size, int screen) {
		int middle = position + size / 2;
		if (middle * 3 < screen) {
			return START;
		}
		return middle * 3 > screen * 2 ? END : CENTRE;
	}

	int offset(int position, int size, int screen) {
		return switch (this) {
			case START -> position;
			case CENTRE -> position + size / 2 - screen / 2;
			case END -> position + size - screen;
		};
	}

	int position(int offset, int size, int screen) {
		int position = switch (this) {
			case START -> offset;
			case CENTRE -> screen / 2 + offset - size / 2;
			case END -> screen + offset - size;
		};
		// never off screen, even after the window got smaller
		return Math.max(0, Math.min(position, screen - size));
	}
}
