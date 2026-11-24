package io.github.filipovych.filobulus.client.corpse;

import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.corpse.CorpseVerdict;
import io.github.filipovych.filobulus.corpse.CrackTier;
import io.github.filipovych.filobulus.corpse.CrackTimeline;
import io.github.filipovych.filobulus.corpse.CrackTimeline.Frame;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

// the corpse reveal: a block of ice cracks, shatters and shows how the loot did against the key
// what is drawn at which moment comes from CrackTimeline, here it is only put on screen with the game's own textures
public final class CrackHud {
	// fixed while it plays so the widget does not jump around, only a long caption makes it wider
	private static final int WIDTH = 160;
	private static final int HEIGHT = 92;
	private static final int CX = WIDTH / 2;
	private static final int TITLE_Y = 64;
	private static final int CAPTION_Y = 82;
	private static final float TITLE_SCALE = 2f;
	// text this faint is not worth drawing, and the game shows an alpha of zero as solid
	private static final int MIN_ALPHA = 10;
	// rgb
	private static final int CAPTION = 0xE0E0E0;
	private static final int BROWN = 0xB9855A;

	// one reveal with its words made once, at is the moment it is frozen on or -1 while it plays
	private record Scene(CorpseVerdict verdict, Component title, Component caption, int color, long at) implements HudDrawable {
		Scene(CorpseVerdict verdict, long at) {
			this(verdict, Component.literal(verdict.tier().title()).withStyle(ChatFormatting.BOLD),
					Component.literal(verdict.type().displayName() + "  " + verdict.summary()), colorOf(verdict.tier()), at);
		}

		@Override
		public int width(Font font) {
			return Math.max(WIDTH, font.width(caption));
		}

		@Override
		public int height() {
			return HEIGHT;
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, Font font) {
			// everything is laid out around the middle of WIDTH, a wider widget moves it along
			graphics.pose().pushMatrix();
			try {
				graphics.pose().translate((width(font) - WIDTH) / 2, 0);
				CrackHud.draw(graphics, font, this, at >= 0 ? at : System.currentTimeMillis() - startedAt);
			} finally {
				// the hud catches what goes wrong in here and draws on, the matrix must not stay behind
				graphics.pose().popMatrix();
			}
		}
	}

	private static long startedAt;

	private static void draw(GuiGraphicsExtractor graphics, Font font, Scene scene, long elapsed) {
		Frame frame = CrackTimeline.at(elapsed);
		int alpha = Math.round(frame.text() * 255);
		if (alpha >= MIN_ALPHA) {
			graphics.pose().pushMatrix();
			try {
				graphics.pose().translate(CX - font.width(scene.title()) * TITLE_SCALE / 2, TITLE_Y);
				graphics.pose().scale(TITLE_SCALE, TITLE_SCALE);
				graphics.text(font, scene.title(), 0, 0, alpha << 24 | scene.color(), true);
			} finally {
				graphics.pose().popMatrix();
			}
			graphics.text(font, scene.caption(), CX - font.width(scene.caption()) / 2, CAPTION_Y, alpha << 24 | CAPTION, true);
		}
	}

	private static int colorOf(CrackTier tier) {
		return 0xFFFFFF & switch (tier) {
			case DIRT -> BROWN;
			case GLACITE -> ShaftColors.GLACITE;
			case RUBY -> ShaftColors.of(ShaftMaterial.RUBY);
			case AQUAMARINE -> ShaftColors.of(ShaftMaterial.AQUAMARINE);
			case JASPER -> ShaftColors.of(ShaftMaterial.JASPER);
		};
	}
}
