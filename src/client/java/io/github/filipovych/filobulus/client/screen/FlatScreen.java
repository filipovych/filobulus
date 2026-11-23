package io.github.filipovych.filobulus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

// the look of our own screens: a dark panel like the hud's and flat text buttons, no vanilla widgets
// buttons are drawn and remembered every frame, a click goes to whichever one was drawn under the mouse
public abstract class FlatScreen extends Screen {
	// argb
	protected static final int DIM = 0x90000000;
	protected static final int PANEL = 0xF0101010;
	protected static final int OUTLINE = 0x80808080;
	protected static final int ACCENT = 0xFF7FD4FF;
	protected static final int TEXT = 0xFFFFFFFF;
	protected static final int MUTED = 0xFFA9B6C2;
	protected static final int FAINT = 0xFF6B7782;
	protected static final int GOOD = 0xFF7CE38B;
	protected static final int BAD = 0xFFFF6B6B;
	protected static final int HOVER = 0x28FFFFFF;
	protected static final int BUTTON = 0x18FFFFFF;
	protected static final int BUTTON_HEIGHT = 14;

	private record Hit(int left, int top, int width, int height, Runnable onLeft, Runnable onRight) {
		boolean contains(double x, double y) {
			return x >= left && x < left + width && y >= top && y < top + height;
		}
	}

	private final List<Hit> hits = new ArrayList<>();

	protected FlatScreen(Component title) {
		super(title);
	}

	protected abstract void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY);

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, width, height, DIM);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		hits.clear();
		draw(graphics, mouseX, mouseY);
		super.extractRenderState(graphics, mouseX, mouseY, delta);
	}

	protected void panel(GuiGraphicsExtractor graphics, int left, int top, int width, int height) {
		graphics.fill(left, top, left + width, top + height, PANEL);
		graphics.outline(left, top, width, height, OUTLINE);
	}

	// makes a rectangle clickable for this frame, onRight may be null
	// with no onLeft either it only swallows the click, to shield what was drawn under it
	protected void clickable(int left, int top, int width, int height, Runnable onLeft, Runnable onRight) {
		hits.add(new Hit(left, top, width, height, onLeft, onRight));
	}

	// a text button, returns true while the mouse is over it so the caller can show a tooltip
	protected boolean button(GuiGraphicsExtractor graphics, int left, int top, int width, String label, int color,
			int mouseX, int mouseY, Runnable action) {
		boolean hovered = mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + BUTTON_HEIGHT;
		graphics.fill(left, top, left + width, top + BUTTON_HEIGHT, hovered ? HOVER : BUTTON);
		graphics.centeredText(font, label, left + width / 2, top + 3, color);
		clickable(left, top, width, BUTTON_HEIGHT, action, null);
		return hovered;
	}

	protected int buttonWidth(String label) {
		return font.width(label) + 8;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean left = event.button() == InputConstants.MOUSE_BUTTON_LEFT;
		boolean right = event.button() == InputConstants.MOUSE_BUTTON_RIGHT;
		// the last one drawn is on top
		for (int i = hits.size() - 1; i >= 0 && (left || right); i--) {
			Hit hit = hits.get(i);
			if (hit.contains(event.x(), event.y())) {
				Runnable action = right && hit.onRight() != null ? hit.onRight() : hit.onLeft();
				if (action != null) {
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
					action.run();
				}
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
