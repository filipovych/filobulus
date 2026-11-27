package io.github.filipovych.filobulus.client.alert;

import io.github.filipovych.filobulus.alert.JasperStorm;
import io.github.filipovych.filobulus.alert.JasperStorm.Cell;
import io.github.filipovych.filobulus.alert.JasperStorm.Flake;
import io.github.filipovych.filobulus.alert.JasperStorm.Spot;
import io.github.filipovych.filobulus.alert.JasperTimeline;
import io.github.filipovych.filobulus.alert.JasperTimeline.Frame;
import io.github.filipovych.filobulus.alert.JasperTimeline.Shard;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.config.SoundsConfig;
import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.HudWidget;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.sound.SoundSlot;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

// the jasper moment: a gust of snow blows round the edges of the screen, frost creeps into the corners
// and JASPER shatters in, the middle of the screen stays clear the whole time
// the storm goes by the whole screen, the words sit where the player put this widget
// what is drawn when comes from JasperTimeline and JasperStorm, here it is only put on screen with plain fills
public final class JasperHud implements HudWidget {
	private static final int WIDTH = 200;
	private static final int HEIGHT = 54;
	private static final int CX = WIDTH / 2;
	// the middle of the title, it grows and shrinks round that point
	private static final int TITLE_MID = 14;
	private static final int SUBTITLE_Y = 31;
	private static final int DRY_Y = 43;
	private static final float TITLE_SCALE = 2.5f;
	// the corner frost reaches this far into the screen, of its shorter side
	private static final float FROST_REACH = 0.28f;
	private static final float BOTTOM_FROST = 0.6f;
	private static final float GRAVITY = 30;
	// the shards are blown along with the snow a bit
	private static final float WIND = 26;
	// text this faint is not worth drawing, and the game shows an alpha of zero as solid
	private static final int MIN_ALPHA = 10;
	// rgb
	private static final int PINK = ShaftColors.of(ShaftMaterial.JASPER) & 0xFFFFFF;
	private static final int PALE_PINK = 0xFFB0F4;
	private static final int FROST = 0xA8DDFF;
	private static final int ICE = 0xE6F7FF;
	private static final int SHADOW = 0x2A4870;
	private static final int SUBTITLE = 0xE0E0E0;
	private static final int WHITE = 0xFFFFFF;
	private static final Component TITLE = Component.literal("JASPER").withStyle(ChatFormatting.BOLD);
	// the editor shows the words once they settled, without the frost over the whole screen
	private static final Scene SAMPLE = new Scene("JASP_1", 47, 2400, false);

	// one moment with its words, flakes and frost made once, at is the moment it is frozen on or -1 while it plays
	private record Scene(Component subtitle, Component dry, boolean big, int seed, List<Flake> flakes,
			List<List<Cell>> corners, long at, boolean screen) implements HudDrawable {
		Scene(String code, int dry, long at, boolean screen) {
			this(code, dry, JasperTimeline.big(dry), code.hashCode() + dry, at, screen);
		}

		private Scene(String code, int dry, boolean big, int seed, long at, boolean screen) {
			this(Component.literal(code), Component.literal("DRY " + dry).withStyle(ChatFormatting.BOLD), big, seed,
					JasperStorm.flakes(seed, big), List.of(JasperStorm.frost(seed), JasperStorm.frost(seed + 1),
							JasperStorm.frost(seed + 2), JasperStorm.frost(seed + 3)), at, screen);
		}

		@Override
		public int width(Font font) {
			return WIDTH;
		}

		@Override
		public int height() {
			return HEIGHT;
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, Font font) {
			JasperHud.draw(graphics, font, this, at >= 0 ? at : System.currentTimeMillis() - startedAt);
		}
	}

	private static Scene scene;
	private static Optional<HudDrawable> shown = Optional.empty();
	private static long startedAt;
	private static long heard;

	// code is the shaft code like JASP_1, dry the shafts without jasper right before this one
	public static void play(String code, int dry) {
		show(new Scene(code, dry, -1, true));
		startedAt = System.currentTimeMillis();
		heard = 0;
		FilobulusClient.sounds().play(SoundSlot.JASPER_SHAFT);
	}

	// stays on that one moment until the next play, to look at a frame for as long as it takes
	public static void hold(String code, int dry, long elapsed) {
		show(new Scene(code, dry, elapsed, true));
	}

	// takes away a hold or a moment that is still going
	public static void stop() {
		show(null);
	}

	// once a client tick: the glass sound when the title lands, and the end
	public static void tick() {
		Scene playing = scene;
		if (playing == null || playing.at() >= 0) {
			return;
		}
		long elapsed = System.currentTimeMillis() - startedAt;
		if (SoundsConfig.sounds && JasperTimeline.lands(heard, elapsed, playing.big())) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.GLASS_BREAK, 1.2f, 0.8f));
		}
		heard = elapsed;
		if (elapsed >= JasperTimeline.total(playing.big())) {
			show(null);
		}
	}

	private static void show(Scene next) {
		scene = next;
		shown = Optional.ofNullable(next);
	}

	private static void draw(GuiGraphicsExtractor graphics, Font font, Scene scene, long elapsed) {
		Frame frame = JasperTimeline.at(elapsed, scene.big());
		if (scene.screen()) {
			graphics.pose().pushMatrix();
			try {
				// the storm goes by the whole screen and not by where the widget sits
				graphics.pose().identity();
				drawFrost(graphics, scene, frame.frost(), elapsed, graphics.guiWidth(), graphics.guiHeight());
				drawSnow(graphics, JasperStorm.spots(scene.flakes(), scene.seed(), frame.gust(), frame.drift()),
						graphics.guiWidth(), graphics.guiHeight());
			} finally {
				graphics.pose().popMatrix();
			}
		}
		int flash = Math.round(frame.flash() * 140);
		if (flash >= MIN_ALPHA) {
			int half = Math.round(font.width(TITLE) * TITLE_SCALE / 2) + 6;
			graphics.fill(CX - half, TITLE_MID - 16, CX + half, TITLE_MID + 16, flash << 24 | WHITE);
		}
		if (frame.burst() >= 0 && frame.burst() < 1) {
			drawShards(graphics, JasperTimeline.shards(scene.big()), frame.burst());
		}
		int alpha = Math.round(frame.title() * 255);
		if (alpha >= MIN_ALPHA) {
			float scale = TITLE_SCALE * frame.titleScale();
			graphics.pose().pushMatrix();
			try {
				// scaled round the middle of the word, so it shrinks onto its place
				graphics.pose().translate(CX, TITLE_MID);
				graphics.pose().scale(scale, scale);
				graphics.text(font, TITLE, -font.width(TITLE) / 2, -4, alpha << 24 | PINK, true);
			} finally {
				graphics.pose().popMatrix();
			}
		}
		alpha = Math.round(frame.subtitle() * 255);
		if (alpha >= MIN_ALPHA) {
			graphics.text(font, scene.subtitle(), CX - font.width(scene.subtitle()) / 2, SUBTITLE_Y, alpha << 24 | SUBTITLE, true);
		}
		alpha = Math.round(frame.dry() * 255);
		if (alpha >= MIN_ALPHA) {
			graphics.text(font, scene.dry(), CX - font.width(scene.dry()) / 2, DRY_Y, alpha << 24 | PALE_PINK, true);
		}
	}

	// pixel frost in the four corners like the powder snow overlay but small, a pink light runs through it on the hit
	private static void drawFrost(GuiGraphicsExtractor graphics, Scene scene, float frost, long elapsed, int width,
			int height) {
		if (frost <= 0) {
			return;
		}
		int cell = Math.max(2, Math.round(Math.min(width, height) * FROST_REACH / JasperStorm.CELLS));
		for (int corner = 0; corner < 4; corner++) {
			boolean right = corner % 2 == 1;
			boolean bottom = corner >= 2;
			for (Cell bit : scene.corners().get(corner)) {
				// less of it at the bottom, the chat and the hotbar are there
				if (bottom && bit.at() > BOTTOM_FROST) {
					continue;
				}
				// it grows in from the corner and melts back into it
				float there = Math.min(1, (frost - bit.at()) * 5);
				if (there <= 0) {
					continue;
				}
				float shine = JasperStorm.shine(bit.at(), elapsed, scene.big()) * 0.8f;
				if (bit.glint()) {
					// a few bits twinkle pink the whole time
					shine = Math.max(shine, 0.5f + 0.5f * (float) Math.sin(elapsed / 180.0 + bit.at() * 20));
				}
				int alpha = Math.round(210 * bit.shade() * there * Math.max(0.6f, shine));
				if (alpha < MIN_ALPHA) {
					continue;
				}
				int rgb = shine > 0.35f ? PALE_PINK : bit.shade() > 0.7f ? ICE : FROST;
				int x = right ? width - (bit.dx() + 1) * cell : bit.dx() * cell;
				int y = bottom ? height - (bit.dy() + 1) * cell : bit.dy() * cell;
				graphics.fill(x, y, x + cell, y + cell, alpha << 24 | rgb);
			}
		}
	}

	// the flakes as little pixel shapes like the game's snow, each with a faint streak behind it in the wind
	private static void drawSnow(GuiGraphicsExtractor graphics, List<Spot> spots, int width, int height) {
		for (Spot spot : spots) {
			int alpha = Math.round(spot.alpha() * 255);
			if (alpha < MIN_ALPHA) {
				continue;
			}
			int rgb = switch (spot.tint()) {
				case WHITE -> WHITE;
				case ICE -> ICE;
				case PINK -> PINK;
			};
			int x = Math.round(spot.x() * width);
			int y = Math.round(spot.y() * height);
			int tail = Math.round(spot.tail() * width);
			int faint = Math.round(alpha * (spot.size() == 1 && tail > 8 ? 0.45f : 0.3f));
			if (tail > 1 && faint >= MIN_ALPHA) {
				graphics.fill(x - tail, y, x, y + 1, faint << 24 | rgb);
			}
			if (spot.size() > 1) {
				// a dark copy one pixel down and right, white on a bright sky is hard to see without it
				flake(graphics, x + 1, y + 1, spot.size(), alpha / 2 << 24 | SHADOW);
			}
			flake(graphics, x, y, spot.size(), alpha << 24 | rgb);
		}
	}

	private static void flake(GuiGraphicsExtractor graphics, int x, int y, int size, int color) {
		switch (size) {
			case 1 -> graphics.fill(x, y, x + 1, y + 1, color);
			case 2 -> graphics.fill(x, y, x + 2, y + 2, color);
			default -> {
				// a little plus, the biggest flakes
				graphics.fill(x - 1, y, x + 2, y + 1, color);
				graphics.fill(x, y - 1, x + 1, y + 2, color);
			}
		}
	}

	// small pink and white chips flying out of the word, they slow down, drop a bit and shrink
	private static void drawShards(GuiGraphicsExtractor graphics, List<Shard> shards, float burst) {
		float out = 1 - (1 - burst) * (1 - burst);
		int alpha = Math.round((1 - burst) * 255);
		if (alpha < MIN_ALPHA) {
			return;
		}
		for (Shard shard : shards) {
			float size = shard.size() * (1 - burst * 0.6f);
			graphics.pose().pushMatrix();
			try {
				graphics.pose().translate(CX + shard.dx() * out + WIND * burst,
						TITLE_MID + shard.dy() * out + GRAVITY * burst * burst);
				graphics.pose().rotate(shard.spin() * burst);
				graphics.pose().scale(size, size);
				graphics.fill(-1, 0, 1, 1, alpha << 24 | (shard.white() ? WHITE : PINK));
			} finally {
				graphics.pose().popMatrix();
			}
		}
	}

	@Override
	public String name() {
		return "Jasper moment";
	}

	@Override
	public boolean enabled() {
		return AlertsConfig.jasperMoment;
	}

	@Override
	public void setEnabled(boolean on) {
		AlertsConfig.jasperMoment = on;
	}

	@Override
	public Optional<HudDrawable> content() {
		return AlertsConfig.jasperMoment ? shown : Optional.empty();
	}

	@Override
	public HudDrawable sample() {
		return SAMPLE;
	}

	@Override
	public HudPlacement placement() {
		return new HudPlacement(HudConfig.jasperAnchorX, HudConfig.jasperX,
				HudConfig.jasperAnchorY, HudConfig.jasperY, HudConfig.jasperScale);
	}

	@Override
	public void place(HudPlacement placement) {
		HudConfig.jasperAnchorX = placement.anchorX();
		HudConfig.jasperX = placement.x();
		HudConfig.jasperAnchorY = placement.anchorY();
		HudConfig.jasperY = placement.y();
		HudConfig.jasperScale = placement.scale();
	}

	@Override
	public HudPlacement defaultPlacement() {
		// where the shaft alert sits, it takes its place for a jasper shaft
		return new HudPlacement(HudAnchor.CENTRE, 0, HudAnchor.CENTRE, -56, 1f);
	}
}
