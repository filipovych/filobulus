package io.github.filipovych.filobulus.client.corpse;

import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.HudWidget;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.corpse.CorpseType;
import io.github.filipovych.filobulus.corpse.CorpseVerdict;
import io.github.filipovych.filobulus.corpse.CrackTier;
import io.github.filipovych.filobulus.corpse.CrackTimeline;
import io.github.filipovych.filobulus.corpse.CrackTimeline.Frame;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

// the corpse reveal: a block of ice cracks, shatters and shows how the loot did against the key
// what is drawn at which moment comes from CrackTimeline, here it is only put on screen with the game's own textures
public final class CrackHud implements HudWidget {
	// fixed while it plays so the widget does not jump around, only a long caption makes it wider
	private static final int WIDTH = 160;
	private static final int HEIGHT = 92;
	// the middle of the block, the revealed item sits there too
	private static final int CX = WIDTH / 2;
	private static final int CY = 32;
	private static final int BLOCK = 48;
	private static final int TITLE_Y = 64;
	private static final int CAPTION_Y = 82;
	private static final float TITLE_SCALE = 2f;
	// text this faint is not worth drawing, and the game shows an alpha of zero as solid
	private static final int MIN_ALPHA = 10;
	// rgb
	private static final int CAPTION = 0xE0E0E0;
	private static final int BROWN = 0xB9855A;
	// a dark edge, pale ice on a pale wall is hard to see without one
	private static final int EDGE = 0xFF1E2C52;
	// the crack textures are grey, this makes them a dark blue
	private static final int CRACK_TINT = 0xFF5078D8;
	private static final Identifier ICE = Identifier.withDefaultNamespace("textures/block/packed_ice.png");
	private static final Identifier[] CRACKS = new Identifier[10];
	private static final double KEY_COST = 4_000_000;
	// the editor shows the moment after the reveal has settled
	private static final Scene SAMPLE = new Scene(example(CrackTier.JASPER), 3600);

	static {
		for (int stage = 0; stage < CRACKS.length; stage++) {
			CRACKS[stage] = Identifier.withDefaultNamespace("textures/block/destroy_stage_" + stage + ".png");
		}
	}

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

	private static Optional<HudDrawable> shown = Optional.empty();
	private static long startedAt;

	// made up numbers that land in the tier, for /filo crack and the hud editor
	public static CorpseVerdict example(CrackTier tier) {
		double roi = switch (tier) {
			case DIRT -> 0.2;
			case GLACITE -> 0.5;
			case RUBY -> 0.85;
			case AQUAMARINE -> 1.6;
			case JASPER -> 3.2;
		};
		return new CorpseVerdict(CorpseType.UMBER, tier, roi * KEY_COST, KEY_COST, false, "", 0);
	}

	private static void draw(GuiGraphicsExtractor graphics, Font font, Scene scene, long elapsed) {
		Frame frame = CrackTimeline.at(elapsed);
		if (frame.block() > 0) {
			graphics.pose().pushMatrix();
			try {
				graphics.pose().translate(CX + frame.shakeX(), CY + frame.shakeY());
				graphics.pose().scale(frame.block(), frame.block());
				graphics.fill(-BLOCK / 2 - 1, -BLOCK / 2 - 1, BLOCK / 2 + 1, BLOCK / 2 + 1, EDGE);
				// a block texture is 16 pixels, stretched flat over the square
				graphics.blit(RenderPipelines.GUI_TEXTURED, ICE, -BLOCK / 2, -BLOCK / 2, 0f, 0f, BLOCK, BLOCK, 16, 16, 16, 16, -1);
				if (frame.cracks() >= 0) {
					graphics.blit(RenderPipelines.GUI_TEXTURED, CRACKS[frame.cracks()], -BLOCK / 2, -BLOCK / 2, 0f, 0f,
							BLOCK, BLOCK, 16, 16, 16, 16, CRACK_TINT);
				}
			} finally {
				graphics.pose().popMatrix();
			}
		}
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

	@Override
	public Optional<HudDrawable> content() {
		return shown;
	}

	@Override
	public HudDrawable sample() {
		return SAMPLE;
	}

	@Override
	public HudPlacement placement() {
		return new HudPlacement(HudConfig.crackAnchorX, HudConfig.crackX,
				HudConfig.crackAnchorY, HudConfig.crackY, HudConfig.crackScale);
	}

	@Override
	public void place(HudPlacement placement) {
		HudConfig.crackAnchorX = placement.anchorX();
		HudConfig.crackX = placement.x();
		HudConfig.crackAnchorY = placement.anchorY();
		HudConfig.crackY = placement.y();
		HudConfig.crackScale = placement.scale();
	}

	@Override
	public HudPlacement defaultPlacement() {
		// top middle, clear of the crosshair and of the alerts under it
		return new HudPlacement(HudAnchor.CENTRE, 0, HudAnchor.START, 4, 1f);
	}
}
