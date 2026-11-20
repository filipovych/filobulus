package io.github.filipovych.filobulus.client.hud;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.hud.Hud.Box;
import io.github.filipovych.filobulus.hud.HudPlacement;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

// /filo hud: every widget is shown, with made up content if it has none, and can be dragged around
public final class HudEditScreen extends Screen {
	private static final int DIM = 0x50000000;
	private static final int FRAME = 0x80FFFFFF;
	private static final int FRAME_ACTIVE = 0xFF7FD4FF;
	private static final int GUIDE = 0x607FD4FF;
	private static final int HELP = 0xFFE0E0E0;
	private static final float SCALE_STEP = 0.1f;

	// the widget the keys act on, the last one that was clicked
	private HudWidget selected = Hud.WIDGETS.getFirst();
	private HudWidget dragging;
	private double grabX;
	private double grabY;

	public HudEditScreen() {
		super(Component.literal("Filobulus HUD"));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		// only a light dim and no blur, the point is to see the panels over the game
		graphics.fill(0, 0, width, height, DIM);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(font, "Drag to move, scroll to resize", width / 2, height / 2 - 10, HELP);
		graphics.centeredText(font, "Arrow keys nudge, Shift stops snapping, R resets", width / 2, height / 2 + 2, HELP);
		for (HudWidget widget : Hud.WIDGETS) {
			Box box = box(widget);
			if (widget == dragging) {
				drawGuides(graphics, box);
			}
			Hud.draw(graphics, font, widget, shown(widget));
			boolean active = widget == dragging || box.contains(mouseX, mouseY);
			graphics.outline(box.left() - 1, box.top() - 1, box.width() + 2, box.height() + 2,
					active ? FRAME_ACTIVE : FRAME);
		}
	}

	// a line across the screen while the panel sits exactly in the middle of an axis
	private void drawGuides(GuiGraphicsExtractor graphics, Box box) {
		if (box.left() == (width - box.width()) / 2) {
			graphics.fill(width / 2, 0, width / 2 + 1, height, GUIDE);
		}
		if (box.top() == (height - box.height()) / 2) {
			graphics.fill(0, height / 2, width, height / 2 + 1, GUIDE);
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			for (HudWidget widget : Hud.WIDGETS) {
				Box box = box(widget);
				if (box.contains(event.x(), event.y())) {
					selected = widget;
					dragging = widget;
					grabX = event.x() - box.left();
					grabY = event.y() - box.top();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (dragging == null) {
			return super.mouseDragged(event, dragX, dragY);
		}
		Box box = box(dragging);
		int left = (int) Math.round(event.x() - grabX);
		int top = (int) Math.round(event.y() - grabY);
		if (!event.hasShiftDown()) {
			left = HudPlacement.snap(left, box.width(), width);
			top = HudPlacement.snap(top, box.height(), height);
		}
		moveTo(dragging, left, top);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		for (HudWidget widget : Hud.WIDGETS) {
			if (scrollY != 0 && box(widget).contains(mouseX, mouseY)) {
				// kept to one decimal so it does not drift
				float scale = widget.placement().scale() + (scrollY > 0 ? SCALE_STEP : -SCALE_STEP);
				widget.place(widget.placement().withScale(Math.round(scale * 10) / 10f));
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		Box box = box(selected);
		if (event.isLeft()) {
			moveTo(selected, box.left() - 1, box.top());
		} else if (event.isRight()) {
			moveTo(selected, box.left() + 1, box.top());
		} else if (event.isUp()) {
			moveTo(selected, box.left(), box.top() - 1);
		} else if (event.isDown()) {
			moveTo(selected, box.left(), box.top() + 1);
		} else if (event.key() == InputConstants.KEY_R) {
			selected.place(selected.defaultPlacement());
		} else {
			return super.keyPressed(event);
		}
		return true;
	}

	@Override
	public void removed() {
		// positions live in the normal settings file
		FilobulusClient.configurator().saveConfig(FilobulusConfig.class);
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void moveTo(HudWidget widget, int left, int top) {
		Box box = box(widget);
		widget.place(HudPlacement.at(left, top, box.width(), box.height(), width, height, widget.placement().scale()));
	}

	private Box box(HudWidget widget) {
		return Hud.box(font, widget, shown(widget), width, height);
	}

	private static HudPanel.Content shown(HudWidget widget) {
		return widget.content().orElseGet(widget::sample);
	}
}
