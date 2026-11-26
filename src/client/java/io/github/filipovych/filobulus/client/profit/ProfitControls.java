package io.github.filipovych.filobulus.client.profit;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.TrackerConfig;
import io.github.filipovych.filobulus.client.hud.Hud;
import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.HudWidget;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

// while an inventory is open, a few small buttons under the profit panel: the period, the price and a reset
// the way skysoft does it, only our own panel is touched and the clicks never reach the inventory
public final class ProfitControls {
	private static final int HEIGHT = 12;
	private static final int GAP = 2;
	// argb
	private static final int BACK = 0xC0101010;
	private static final int HOVER = 0xE0303840;
	private static final int TEXT = 0xFFA9B6C2;
	private static final int RESET = 0xFFFF6B6B;
	private static final long CONFIRM_MILLIS = 3000;

	private record Button(int left, int top, int width, String label, int color, Runnable action) {
		boolean contains(double x, double y) {
			return x >= left && x < left + width && y >= top && y < top + HEIGHT;
		}
	}

	private static long resetAskedAt;

	private ProfitControls() {
	}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (!(screen instanceof AbstractContainerScreen<?>)) {
				return;
			}
			ScreenEvents.afterExtract(screen).register((shown, graphics, mouseX, mouseY, delta) -> draw(graphics, mouseX, mouseY));
			ScreenMouseEvents.allowMouseClick(screen).register((shown, event) -> {
				if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
					return true;
				}
				for (Button button : buttons(Minecraft.getInstance().font)) {
					if (button.contains(event.x(), event.y())) {
						Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
						button.action().run();
						FilobulusClient.configurator().saveConfig(FilobulusConfig.class);
						return false;
					}
				}
				return true;
			});
		});
	}

	private static void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Font font = Minecraft.getInstance().font;
		for (Button button : buttons(font)) {
			boolean hovered = button.contains(mouseX, mouseY);
			graphics.fill(button.left(), button.top(), button.left() + button.width(), button.top() + HEIGHT, hovered ? HOVER : BACK);
			graphics.centeredText(font, button.label(), button.left() + button.width() / 2, button.top() + 2, button.color());
		}
	}

	// right under the panel, empty while the panel is not on screen
	private static List<Button> buttons(Font font) {
		List<Button> buttons = new ArrayList<>();
		Optional<HudWidget> widget = Hud.WIDGETS.stream().filter(each -> each instanceof ProfitHud).findFirst();
		Optional<HudDrawable> content = widget.flatMap(HudWidget::content);
		if (widget.isEmpty() || content.isEmpty()) {
			return buttons;
		}
		Minecraft minecraft = Minecraft.getInstance();
		Hud.Box box = Hud.box(font, widget.get(), content.get(), minecraft.getWindow().getGuiScaledWidth(),
				minecraft.getWindow().getGuiScaledHeight());
		int screenWidth = minecraft.getWindow().getGuiScaledWidth();
		int y = box.top() + box.height() + GAP;
		boolean sure = System.currentTimeMillis() - resetAskedAt < CONFIRM_MILLIS;
		List<String> labels = List.of(TrackerConfig.period.toString(), TrackerConfig.price.toString(), sure ? "Sure?" : "Reset");
		List<Runnable> actions = List.of(ProfitControls::nextPeriod, ProfitControls::nextPrice, () -> reset(sure));
		int total = -GAP;
		for (String label : labels) {
			total += font.width(label) + 8 + GAP;
		}
		// a panel on the right half lines its buttons up with its right edge, so they stay on screen
		int x = box.left() + box.width() / 2 > screenWidth / 2 ? box.left() + box.width() - total : box.left();
		x = Math.max(0, Math.min(x, screenWidth - total));
		for (int i = 0; i < labels.size(); i++) {
			int width = font.width(labels.get(i)) + 8;
			buttons.add(new Button(x, y, width, labels.get(i), i == 2 ? RESET : TEXT, actions.get(i)));
			x += width + GAP;
		}
		return buttons;
	}

	private static void nextPeriod() {
		TrackerConfig.Period[] all = TrackerConfig.Period.values();
		TrackerConfig.period = all[(TrackerConfig.period.ordinal() + 1) % all.length];
	}

	private static void nextPrice() {
		TrackerConfig.Price[] all = TrackerConfig.Price.values();
		TrackerConfig.price = all[(TrackerConfig.price.ordinal() + 1) % all.length];
	}

	// the first click asks, a second one within a few seconds clears the period on show
	private static void reset(boolean sure) {
		if (!sure) {
			resetAskedAt = System.currentTimeMillis();
			return;
		}
		resetAskedAt = 0;
		FilobulusClient.profit().reset();
	}
}
