package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.Guard;
import io.github.filipovych.filobulus.client.alert.AlertHud;
import io.github.filipovych.filobulus.client.alert.JasperHud;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.corpse.CrackHud;
import io.github.filipovych.filobulus.client.journal.RecapHud;
import io.github.filipovych.filobulus.client.mining.BreakHud;
import io.github.filipovych.filobulus.client.mining.ComboHud;
import io.github.filipovych.filobulus.client.mining.SwapHud;
import io.github.filipovych.filobulus.client.profit.ProfitHud;
import io.github.filipovych.filobulus.hud.HudPlacement;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

// every panel the mod puts on screen, drawn under the chat
public final class Hud {
	public static final List<HudWidget> WIDGETS = List.of(new RouteHud(), new ProfitHud(), new BreakHud(),
			new RecapHud(), new AlertHud(), new CrackHud(), new SwapHud(), new ComboHud(), new JasperHud());

	// the rectangle a widget covers on screen, scale applied
	public record Box(int left, int top, int width, int height) {
		public boolean contains(double x, double y) {
			return x >= left && x < left + width && y >= top && y < top + height;
		}
	}

	// client ticks since the game started, the panels work themselves out once per tick
	private static long ticks;
	// a panel keeps its widest size for a few seconds, so it does not jitter while numbers change
	private static final long STEADY_MILLIS = 3000;
	private static final Map<HudWidget, long[]> WIDEST = new IdentityHashMap<>();
	// the content is the same object for a whole tick, so it is measured once and not every frame
	private record Laid(HudDrawable source, HudDrawable shown, int width, int height) {
	}

	private static final Map<HudWidget, Laid> LAID = new IdentityHashMap<>();

	private Hud() {
	}

	// called at the end of every client tick
	public static void tick() {
		ticks++;
	}

	public static long ticks() {
		return ticks;
	}

	public static void register() {
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(FilobulusClient.MOD_ID, "hud"), Hud::draw);
	}

	private static void draw(GuiGraphicsExtractor graphics, DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		// /filo hud draws the widgets itself
		if (!FilobulusConfig.enabled || GameCompat.screen(minecraft) instanceof HudEditScreen) {
			return;
		}
		for (HudWidget widget : WIDGETS) {
			Guard.run("hud " + widget.getClass().getSimpleName(), () -> widget.content()
					.ifPresent(content -> draw(graphics, minecraft.font, widget, laid(minecraft.font, widget, content))));
		}
	}

	private static Laid laid(Font font, HudWidget widget, HudDrawable content) {
		Laid laid = LAID.get(widget);
		long now = System.currentTimeMillis();
		long[] widest = WIDEST.computeIfAbsent(widget, key -> new long[2]);
		// a panel held at its widest is measured again once that is over
		boolean held = laid != null && laid.shown() != laid.source() && now - widest[1] > STEADY_MILLIS;
		if (laid == null || laid.source() != content || held) {
			HudDrawable shown = steady(font, content, now, widest);
			laid = new Laid(content, shown, shown.width(font), shown.height());
			LAID.put(widget, laid);
		}
		return laid;
	}

	private static HudDrawable steady(Font font, HudDrawable content, long now, long[] widest) {
		if (!(content instanceof HudPanel.Content panel)) {
			return content;
		}
		int width = panel.width(font);
		if (width >= widest[0] || now - widest[1] > STEADY_MILLIS) {
			widest[0] = width;
			widest[1] = now;
			return content;
		}
		return panel.atLeast((int) widest[0]);
	}

	public static Box box(Font font, HudWidget widget, HudDrawable content, int screenWidth, int screenHeight) {
		HudPlacement placement = widget.placement();
		int width = Math.round(content.width(font) * placement.scale());
		int height = Math.round(content.height() * placement.scale());
		return new Box(placement.left(width, screenWidth), placement.top(height, screenHeight), width, height);
	}

	// the position editor draws every widget whether it is on or not, measured each time
	static void draw(GuiGraphicsExtractor graphics, Font font, HudWidget widget, HudDrawable content) {
		draw(graphics, font, widget, new Laid(content, content, content.width(font), content.height()));
	}

	private static void draw(GuiGraphicsExtractor graphics, Font font, HudWidget widget, Laid laid) {
		HudPlacement placement = widget.placement();
		float scale = placement.scale();
		int width = Math.round(laid.width() * scale);
		int height = Math.round(laid.height() * scale);
		Box box = new Box(placement.left(width, graphics.guiWidth()), placement.top(height, graphics.guiHeight()), width, height);
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(box.left(), box.top());
			graphics.pose().scale(scale, scale);
			if (laid.shown() instanceof HudPanel.Content panel) {
				HudPanel.draw(graphics, font, panel, HudConfig.panel, laid.width());
			} else {
				laid.shown().draw(graphics, font);
			}
		} finally {
			// a widget that throws must not leave its matrix on what is drawn after it
			graphics.pose().popMatrix();
		}
	}
}
