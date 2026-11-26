package io.github.filipovych.filobulus.client.corpse;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.CorpsesConfig;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.hud.Frost;
import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.HudWidget;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.corpse.CorpseType;
import io.github.filipovych.filobulus.corpse.CorpseVerdict;
import io.github.filipovych.filobulus.corpse.CrackTier;
import io.github.filipovych.filobulus.corpse.CrackTimeline;
import io.github.filipovych.filobulus.corpse.CrackTimeline.Frame;
import io.github.filipovych.filobulus.corpse.CrackTimeline.Sound;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.sound.SoundSlot;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
	private static final int PIECE = BLOCK / 4;
	private static final int FLASH = 28;
	private static final int TITLE_Y = 64;
	private static final int CAPTION_Y = 82;
	private static final float TITLE_SCALE = 2f;
	private static final float ITEM_SCALE = 3f;
	private static final int ORBIT = 28;
	private static final int SPARKS = 6;
	// text this faint is not worth drawing, and the game shows an alpha of zero as solid
	private static final int MIN_ALPHA = 10;
	// rgb
	private static final int CAPTION = 0xE0E0E0;
	private static final int BROWN = 0xB9855A;
	private static final int WHITE = 0xFFFFFF;
	// a dark edge, pale ice on a pale wall is hard to see without one
	private static final int EDGE = 0xFF1E2C52;
	// the crack textures are grey, this makes them a dark blue
	private static final int CRACK_TINT = 0xFF5078D8;
	private static final Identifier ICE = Identifier.withDefaultNamespace("textures/block/packed_ice.png");
	private static final Identifier[] CRACKS = new Identifier[10];
	// the block breaks into 4 by 4 pieces, this is how far each one flies, x then y, gravity comes on top
	private static final float[] SHARDS = {
			-46, -40, -18, -52, 14, -56, 50, -36,
			-58, -18, -22, -30, 26, -34, 54, -12,
			-52, -4, -12, -20, 16, -14, 58, -8,
			-40, 6, -20, -6, 10, 2, 44, 10};
	private static final float GRAVITY = 60;
	private static final float SPIN = 0.04f;
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

	private static Scene scene;
	private static Optional<HudDrawable> shown = Optional.empty();
	private static long startedAt;
	// how far into the reveal the sounds have been played
	private static long heard;
	private static ItemStack dirt;

	public static void play(CorpseVerdict verdict) {
		show(new Scene(verdict, -1));
		startedAt = System.currentTimeMillis();
		heard = -1;
		if (verdict.tier() == CrackTier.JASPER) {
			Frost.glint(Frost.ALL, colorOf(CrackTier.JASPER));
		}
	}

	// stays on that one moment until the next play, to look at a frame for as long as it takes
	public static void hold(CorpseVerdict verdict, long elapsed) {
		show(new Scene(verdict, elapsed));
	}

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

	// once a client tick: the sounds that came due since the last one, and the end of the reveal
	public static void tick() {
		Scene playing = scene;
		if (playing == null || playing.at() >= 0) {
			return;
		}
		long elapsed = System.currentTimeMillis() - startedAt;
		if (CorpsesConfig.revealSounds) {
			for (Sound sound : CrackTimeline.cues(heard, elapsed)) {
				sound(sound, playing.verdict().tier());
			}
		}
		heard = elapsed;
		if (elapsed >= CrackTimeline.TOTAL) {
			show(null);
		}
	}

	private static void show(Scene next) {
		scene = next;
		shown = Optional.ofNullable(next);
	}

	private static void sound(Sound sound, CrackTier tier) {
		if (sound.cue() == CrackTimeline.Cue.REVEAL) {
			// the tier's sound is the player's pick on the sounds screen
			FilobulusClient.sounds().play(switch (tier) {
				case DIRT -> SoundSlot.CORPSE_DIRT;
				case GLACITE -> SoundSlot.CORPSE_GLACITE;
				case RUBY -> SoundSlot.CORPSE_RUBY;
				case AQUAMARINE -> SoundSlot.CORPSE_AQUAMARINE;
				case JASPER -> SoundSlot.CORPSE_JASPER;
			});
			return;
		}
		Minecraft.getInstance().getSoundManager().play(switch (sound.cue()) {
			case CRACK -> SimpleSoundInstance.forUI(SoundEvents.GLASS_HIT, 0.8f + 0.1f * sound.step(), 0.7f);
			case SHATTER -> SimpleSoundInstance.forUI(SoundEvents.GLASS_BREAK, 1f, 1f);
			case REVEAL -> null;
		});
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
		if (frame.reveal() > 0) {
			drawItem(graphics, scene, frame.reveal(), elapsed);
		}
		if (frame.burst() >= 0 && frame.burst() < 1) {
			drawShards(graphics, frame.burst());
		}
		int flash = Math.round(frame.flash() * 255);
		if (flash >= MIN_ALPHA) {
			graphics.fill(CX - FLASH, CY - FLASH, CX + FLASH, CY + FLASH, flash << 24 | WHITE);
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

	// an item can not be faded, so it grows in and shrinks away
	private static void drawItem(GuiGraphicsExtractor graphics, Scene scene, float reveal, long elapsed) {
		CrackTier tier = scene.verdict().tier();
		float size = reveal * ITEM_SCALE;
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(CX, CY + (float) Math.sin(elapsed / 320.0) * 1.5f);
			graphics.pose().scale(size, size);
			graphics.item(icon(tier), -8, -8);
		} finally {
			graphics.pose().popMatrix();
		}
		if (tier != CrackTier.JASPER) {
			return;
		}
		// jasper gets a ring of sparks going round the item
		for (int spark = 0; spark < SPARKS; spark++) {
			double angle = elapsed / 380.0 + spark * (Math.PI * 2 / SPARKS);
			int x = CX + (int) Math.round(Math.cos(angle) * ORBIT * reveal);
			int y = CY + (int) Math.round(Math.sin(angle) * ORBIT * reveal);
			int color = 0xFF000000 | (spark % 2 == 0 ? scene.color() : WHITE);
			// two bars make a small cross, a square dot looks like a stray pixel
			graphics.fill(x - 2, y, x + 3, y + 1, color);
			graphics.fill(x, y - 2, x + 1, y + 3, color);
		}
	}

	// every piece keeps its own bit of the ice texture, flies off, turns and shrinks
	private static void drawShards(GuiGraphicsExtractor graphics, float burst) {
		// fast at first, slower at the end
		float out = 1 - (1 - burst) * (1 - burst);
		float size = 1 - burst * burst;
		for (int shard = 0; shard < 16; shard++) {
			int column = shard % 4;
			int row = shard / 4;
			float flyX = SHARDS[shard * 2];
			float flyY = SHARDS[shard * 2 + 1];
			graphics.pose().pushMatrix();
			try {
				graphics.pose().translate(CX + (column - 1.5f) * PIECE + flyX * out,
						CY + (row - 1.5f) * PIECE + flyY * out + GRAVITY * burst * burst);
				graphics.pose().rotate(flyX * SPIN * burst);
				graphics.pose().scale(size, size);
				graphics.fill(-PIECE / 2 - 1, -PIECE / 2 - 1, PIECE / 2 + 1, PIECE / 2 + 1, EDGE);
				graphics.blit(RenderPipelines.GUI_TEXTURED, ICE, -PIECE / 2, -PIECE / 2, column * 4f, row * 4f,
						PIECE, PIECE, 4, 4, 16, 16, -1);
			} finally {
				graphics.pose().popMatrix();
			}
		}
	}

	private static ItemStack icon(CrackTier tier) {
		return switch (tier) {
			case DIRT -> {
				// made late, item stacks can not be built while the game is still starting
				if (dirt == null) {
					dirt = new ItemStack(Items.DIRT);
				}
				yield dirt;
			}
			case GLACITE -> ShaftIcons.of(ShaftMaterial.LITTLEFOOT);
			case RUBY -> ShaftIcons.of(ShaftMaterial.RUBY);
			case AQUAMARINE -> ShaftIcons.of(ShaftMaterial.AQUAMARINE);
			case JASPER -> ShaftIcons.of(ShaftMaterial.JASPER);
		};
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
	public String name() {
		return "Corpse reveal";
	}

	@Override
	public boolean enabled() {
		return CorpsesConfig.reveal;
	}

	@Override
	public void setEnabled(boolean on) {
		CorpsesConfig.reveal = on;
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
