package io.github.filipovych.filobulus.client.alert;

import io.github.filipovych.filobulus.alert.AlertQueue.Alert;
import io.github.filipovych.filobulus.alert.AlertQueue.Shown;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.HudWidget;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

// where the big alerts show, a widget like the others so it can be moved in /filo hud
public final class AlertHud implements HudWidget {
	private static final float TITLE_SCALE = 2f;
	private static final int TITLE_HEIGHT = 18;
	private static final int SUBTITLE_HEIGHT = 10;
	private static final int GAP = 6;
	private static final int SUBTITLE = 0xE0E0E0;
	// text this faint is not worth drawing, and the game shows an alpha of zero as solid
	private static final int MIN_ALPHA = 10;

	// the alerts under each other, every line centred
	private record Banner(List<Shown> alerts) implements HudDrawable {
		@Override
		public int width(Font font) {
			int width = 0;
			for (Shown shown : alerts) {
				width = Math.max(width, Math.round(font.width(title(shown.alert())) * TITLE_SCALE));
				width = Math.max(width, font.width(shown.alert().subtitle()));
			}
			return width;
		}

		@Override
		public int height() {
			int height = -GAP;
			for (Shown shown : alerts) {
				height += TITLE_HEIGHT + (shown.alert().subtitle().isEmpty() ? 0 : SUBTITLE_HEIGHT) + GAP;
			}
			return Math.max(0, height);
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, Font font) {
			int middle = width(font) / 2;
			int y = 0;
			for (Shown shown : alerts) {
				Alert alert = shown.alert();
				int alpha = Math.round(shown.alpha() * 255);
				if (alpha >= MIN_ALPHA) {
					Component title = title(alert);
					graphics.pose().pushMatrix();
					graphics.pose().translate(middle - font.width(title) * TITLE_SCALE / 2, y);
					graphics.pose().scale(TITLE_SCALE, TITLE_SCALE);
					graphics.text(font, title, 0, 0, alpha << 24 | alert.color(), true);
					graphics.pose().popMatrix();
					graphics.text(font, alert.subtitle(), middle - font.width(alert.subtitle()) / 2, y + TITLE_HEIGHT,
							alpha << 24 | SUBTITLE, true);
				}
				y += TITLE_HEIGHT + (alert.subtitle().isEmpty() ? 0 : SUBTITLE_HEIGHT) + GAP;
			}
		}

		private static Component title(Alert alert) {
			return Component.literal(alert.title()).withStyle(ChatFormatting.BOLD);
		}
	}

	@Override
	public String name() {
		return "Alerts";
	}

	@Override
	public boolean enabled() {
		return AlertsConfig.alerts;
	}

	@Override
	public void setEnabled(boolean on) {
		AlertsConfig.alerts = on;
	}

	@Override
	public Optional<HudDrawable> content() {
		List<Shown> shown = Alerts.QUEUE.shown(System.currentTimeMillis());
		return shown.isEmpty() || !AlertsConfig.alerts ? Optional.empty() : Optional.of(new Banner(shown));
	}

	@Override
	public HudDrawable sample() {
		return new Banner(List.of(new Shown(new Alert("sample", "ALERT", "Alerts show here", 0x7FD4FF, 0, 1), 1f)));
	}

	@Override
	public HudPlacement placement() {
		return new HudPlacement(HudConfig.alertsAnchorX, HudConfig.alertsX,
				HudConfig.alertsAnchorY, HudConfig.alertsY, HudConfig.alertsScale);
	}

	@Override
	public void place(HudPlacement placement) {
		HudConfig.alertsAnchorX = placement.anchorX();
		HudConfig.alertsX = placement.x();
		HudConfig.alertsAnchorY = placement.anchorY();
		HudConfig.alertsY = placement.y();
		HudConfig.alertsScale = placement.scale();
	}

	@Override
	public HudPlacement defaultPlacement() {
		// centred, a bit above the crosshair
		return new HudPlacement(HudAnchor.CENTRE, 0, HudAnchor.CENTRE, -48, 1f);
	}
}
