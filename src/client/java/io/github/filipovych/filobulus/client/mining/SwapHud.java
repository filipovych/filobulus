package io.github.filipovych.filobulus.client.mining;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.MiningConfig;
import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.HudWidget;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

// ping gliding: a small mark under the crosshair once the server should have the block broken
public final class SwapHud implements HudWidget {
	private static final String TEXT = "> next";
	// argb
	private static final int COLOR = 0xFF7CE38B;

	private static final HudDrawable CUE = new HudDrawable() {
		@Override
		public int width(Font font) {
			return font.width(TEXT);
		}

		@Override
		public int height() {
			return 9;
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, Font font) {
			graphics.text(font, TEXT, 0, 0, COLOR, true);
		}
	};

	@Override
	public String name() {
		return "Swap cue";
	}

	@Override
	public boolean enabled() {
		return MiningConfig.swapCue;
	}

	@Override
	public void setEnabled(boolean on) {
		MiningConfig.swapCue = on;
	}

	@Override
	public Optional<HudDrawable> content() {
		boolean on = MiningConfig.swapCue && FilobulusClient.mining().swapNow(Minecraft.getInstance());
		return on ? Optional.of(CUE) : Optional.empty();
	}

	@Override
	public HudDrawable sample() {
		return CUE;
	}

	@Override
	public HudPlacement placement() {
		return new HudPlacement(MiningConfig.swapHudAnchorX, MiningConfig.swapHudX,
				MiningConfig.swapHudAnchorY, MiningConfig.swapHudY, MiningConfig.swapHudScale);
	}

	@Override
	public void place(HudPlacement placement) {
		MiningConfig.swapHudAnchorX = placement.anchorX();
		MiningConfig.swapHudX = placement.x();
		MiningConfig.swapHudAnchorY = placement.anchorY();
		MiningConfig.swapHudY = placement.y();
		MiningConfig.swapHudScale = placement.scale();
	}

	@Override
	public HudPlacement defaultPlacement() {
		return new HudPlacement(HudAnchor.CENTRE, 0, HudAnchor.CENTRE, 12, 1f);
	}
}
