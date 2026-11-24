package io.github.filipovych.filobulus.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

// something a widget puts on screen, always drawn at 0,0, the caller moves and scales it
public interface HudDrawable {
	int width(Font font);

	int height();

	void draw(GuiGraphicsExtractor graphics, Font font);
}
