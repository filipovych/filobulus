package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.hud.HudPlacement;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

// every panel the mod puts on screen, drawn under the chat
public final class Hud {
	public static final List<HudWidget> WIDGETS = List.of(new RouteHud());

	// the rectangle a widget covers on screen, scale applied
	public record Box(int left, int top, int width, int height) {
		public boolean contains(double x, double y) {
			return x >= left && x < left + width && y >= top && y < top + height;
		}
	}

	private static boolean failed;

	private Hud() {
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(FilobulusClient.MOD_ID, "hud"), Hud::draw);
	}

	private static void draw(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		// /filo hud draws the widgets itself
		if (minecraft.gui.screen() instanceof HudEditScreen) {
			return;
		}
		try {
			for (HudWidget widget : WIDGETS) {
				widget.content().ifPresent(content -> draw(graphics, minecraft.font, widget, content));
			}
		} catch (RuntimeException e) {
			// same as the route drawing, say it once and keep the game running
			if (!failed) {
				failed = true;
				FilobulusClient.LOGGER.error("Could not draw the hud", e);
			}
		}
	}

	static Box box(Font font, HudWidget widget, HudPanel.Content content, int screenWidth, int screenHeight) {
		HudPlacement placement = widget.placement();
		int width = Math.round(HudPanel.width(font, content) * placement.scale());
		int height = Math.round(HudPanel.height(content) * placement.scale());
		return new Box(placement.left(width, screenWidth), placement.top(height, screenHeight), width, height);
	}

	static void draw(GuiGraphicsExtractor graphics, Font font, HudWidget widget, HudPanel.Content content) {
		Box box = box(font, widget, content, graphics.guiWidth(), graphics.guiHeight());
		float scale = widget.placement().scale();
		graphics.pose().pushMatrix();
		graphics.pose().translate(box.left(), box.top());
		graphics.pose().scale(scale, scale);
		HudPanel.draw(graphics, font, content, HudConfig.background);
		graphics.pose().popMatrix();
	}
}
