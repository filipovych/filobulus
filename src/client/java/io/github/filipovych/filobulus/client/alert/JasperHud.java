package io.github.filipovych.filobulus.client.alert;

import io.github.filipovych.filobulus.alert.JasperTimeline;
import io.github.filipovych.filobulus.alert.JasperTimeline.Frame;
import io.github.filipovych.filobulus.alert.JasperTimeline.Segment;
import io.github.filipovych.filobulus.alert.JasperTimeline.Shard;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.config.HudConfig;
import io.github.filipovych.filobulus.client.config.SoundsConfig;
import io.github.filipovych.filobulus.client.hud.Frost;
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

// the jasper moment: frost creeps in from the screen edges, a pink crack runs across and JASPER shatters in
// the frost and the crack cover the whole screen, the words sit where the player put this widget
// what is drawn when comes from JasperTimeline, here it is only put on screen with plain fills
public final class JasperHud implements HudWidget {
	private static final int WIDTH = 200;
	private static final int HEIGHT = 54;
	private static final int CX = WIDTH / 2;
	// the middle of the title, it grows and shrinks round that point
	private static final int TITLE_MID = 14;
	private static final int SUBTITLE_Y = 31;
	private static final int DRY_Y = 43;
	private static final float TITLE_SCALE = 2.5f;
	// the frost reaches this far into the screen at most, of its shorter side
	private static final float FROST_REACH = 0.2f;
	private static final int BANDS = 10;
	private static final int ICICLE_GAP = 3;
	private static final float GRAVITY = 30;
	// text this faint is not worth drawing, and the game shows an alpha of zero as solid
	private static final int MIN_ALPHA = 10;
	// rgb
	private static final int PINK = ShaftColors.of(ShaftMaterial.JASPER) & 0xFFFFFF;
	private static final int CRACK_DARK = 0x6A1060;
	private static final int CRACK_LIGHT = 0xFFB0F4;
	private static final int FROST = 0xA8DDFF;
	private static final int ICICLE = 0xE6F7FF;
	private static final int SUBTITLE = 0xE0E0E0;
	private static final int WHITE = 0xFFFFFF;
	private static final Component TITLE = Component.literal("JASPER").withStyle(ChatFormatting.BOLD);
	// the editor shows the words once they settled, without the frost over the whole screen
	private static final Scene SAMPLE = new Scene("JASP_1", 47, 2400, false);

	// one moment with its words and its crack made once, at is the moment it is frozen on or -1 while it plays
	private record Scene(Component subtitle, Component dry, boolean big, int seed, List<Segment> crack, long at,
			boolean screen) implements HudDrawable {
		Scene(String code, int dry, long at, boolean screen) {
			this(Component.literal(code), Component.literal("DRY " + dry).withStyle(ChatFormatting.BOLD),
					JasperTimeline.big(dry), code.hashCode() + dry, JasperTimeline.crack(code.hashCode() + dry), at, screen);
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
		Frost.glint(Frost.ALL, PINK);
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
				// the frost and the crack go by the whole screen and not by where the widget sits
				graphics.pose().identity();
				drawFrost(graphics, scene.seed(), frame.frost(), graphics.guiWidth(), graphics.guiHeight());
				drawCrack(graphics, scene.crack(), frame, graphics.guiWidth(), graphics.guiHeight());
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
			graphics.text(font, scene.dry(), CX - font.width(scene.dry()) / 2, DRY_Y, alpha << 24 | CRACK_LIGHT, true);
		}
	}

	// a soft icy border made of bands that get fainter inwards, then short icicles growing in from every edge
	private static void drawFrost(GuiGraphicsExtractor graphics, int seed, float frost, int width, int height) {
		float reach = Math.min(width, height) * FROST_REACH * frost;
		if (reach < 1) {
			return;
		}
		for (int band = 0; band < BANDS; band++) {
			int inner = Math.round(reach * band / BANDS);
			int outer = Math.round(reach * (band + 1) / BANDS);
			float fade = 1 - band / (float) BANDS;
			int alpha = Math.round(90 * fade * fade * frost);
			if (alpha < MIN_ALPHA || outer <= inner) {
				continue;
			}
			int color = alpha << 24 | FROST;
			graphics.fill(0, inner, width, outer, color);
			graphics.fill(0, height - outer, width, height - inner, color);
			graphics.fill(inner, outer, outer, height - outer, color);
			graphics.fill(width - outer, outer, width - inner, height - outer, color);
		}
		int alpha = Math.round(90 * frost);
		if (alpha < MIN_ALPHA) {
			return;
		}
		int color = alpha << 24 | ICICLE;
		int index = 0;
		// uneven gaps and mostly short ones with now and then a long one, so it does not look like a comb
		for (int x = 0; x < width; x += ICICLE_GAP + Math.round(4 * JasperTimeline.noise(seed, index++))) {
			icicle(graphics, x, 0, reach, JasperTimeline.noise(seed, index++), true, 1, color);
			icicle(graphics, x, height, reach * 0.7f, JasperTimeline.noise(seed, index++), true, -1, color);
		}
		for (int y = 0; y < height; y += ICICLE_GAP + Math.round(4 * JasperTimeline.noise(seed, index++))) {
			icicle(graphics, 0, y, reach * 0.8f, JasperTimeline.noise(seed, index++), false, 1, color);
			icicle(graphics, width, y, reach * 0.8f, JasperTimeline.noise(seed, index++), false, -1, color);
		}
	}

	// grows from the edge at x,y inwards, two pixels thick near the edge and one further in
	private static void icicle(GuiGraphicsExtractor graphics, int x, int y, float reach, float roll, boolean down, int way,
			int color) {
		int length = Math.round(reach * (0.1f + 0.9f * roll * roll * roll));
		if (length < 1) {
			return;
		}
		int root = length / 3;
		if (down) {
			int from = way > 0 ? y : y - length;
			graphics.fill(x, from, x + 1, from + length, color);
			graphics.fill(x + 1, way > 0 ? y : y - root, x + 2, way > 0 ? y + root : y, color);
		} else {
			int from = way > 0 ? x : x - length;
			graphics.fill(from, y, from + length, y + 1, color);
			graphics.fill(way > 0 ? x : x - root, y + 1, way > 0 ? x + root : x, y + 2, color);
		}
	}

	// every segment grows while the crack runs over its part, drawn as small squares with a dark rim
	private static void drawCrack(GuiGraphicsExtractor graphics, List<Segment> crack, Frame frame, int width, int height) {
		int alpha = Math.round(frame.crackAlpha() * 255);
		if (alpha < MIN_ALPHA || frame.crack() <= 0) {
			return;
		}
		int dark = Math.round(alpha * 0.6f) << 24 | CRACK_DARK;
		int light = alpha << 24 | CRACK_LIGHT;
		int pink = alpha << 24 | PINK;
		for (Segment segment : crack) {
			float grown = Math.min(1, (frame.crack() - segment.from()) / (segment.to() - segment.from()));
			if (grown <= 0) {
				continue;
			}
			float x1 = segment.x1() * width;
			float y1 = segment.y1() * height;
			float x2 = x1 + (segment.x2() * width - x1) * grown;
			float y2 = y1 + (segment.y2() * height - y1) * grown;
			if (segment.branch()) {
				line(graphics, x1, y1, x2, y2, 0, pink);
			} else {
				line(graphics, x1, y1, x2, y2, 1, dark);
				line(graphics, x1, y1, x2, y2, 0, light);
			}
		}
	}

	// a line as a few runs of pixels, one fill per run and not one per pixel, rim widens it on every side
	private static void line(GuiGraphicsExtractor graphics, float x1, float y1, float x2, float y2, int rim, int color) {
		boolean flat = Math.abs(x2 - x1) >= Math.abs(y2 - y1);
		float from = flat ? x1 : y1;
		float to = flat ? x2 : y2;
		int start = Math.round(Math.min(from, to));
		int end = Math.round(Math.max(from, to));
		int runStart = start;
		int runAt = across(x1, y1, x2, y2, flat, start);
		for (int at = start + 1; at <= end + 1; at++) {
			int here = at > end ? Integer.MIN_VALUE : across(x1, y1, x2, y2, flat, at);
			if (here == runAt) {
				continue;
			}
			if (flat) {
				graphics.fill(runStart - rim, runAt - rim, at + rim, runAt + 1 + rim, color);
			} else {
				graphics.fill(runAt - rim, runStart - rim, runAt + 1 + rim, at + rim, color);
			}
			runStart = at;
			runAt = here;
		}
	}

	// where the line is across its main direction at a point along it
	private static int across(float x1, float y1, float x2, float y2, boolean flat, int at) {
		float from = flat ? x1 : y1;
		float to = flat ? x2 : y2;
		float t = to == from ? 0 : (at - from) / (to - from);
		return Math.round(flat ? y1 + (y2 - y1) * t : x1 + (x2 - x1) * t);
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
				graphics.pose().translate(CX + shard.dx() * out, TITLE_MID + shard.dy() * out + GRAVITY * burst * burst);
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
