package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.client.config.HudConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

// what every hud is made of: an item and a bold title in a colour of its own, then rows of a grey label and a value on the right
// only shadowed text over the game unless the dark panel is switched on
// always drawn at 0,0, the caller moves and scales it
public final class HudPanel {
	public record Row(String label, String value, int color) {
		public Row(String label, String value) {
			this(label, value, VALUE);
		}
	}

	public record Content(ItemStack icon, String title, int titleColor, List<Row> rows) implements HudDrawable {
		public Content {
			rows = List.copyOf(rows);
		}

		@Override
		public int width(Font font) {
			return HudPanel.width(font, this);
		}

		@Override
		public int height() {
			return HudPanel.height(this);
		}

		@Override
		public void draw(GuiGraphicsExtractor graphics, Font font) {
			HudPanel.draw(graphics, font, this, HudConfig.panel);
		}
	}

	private static final int PADDING = 5;
	private static final int ROW_HEIGHT = 11;
	private static final int TEXT_HEIGHT = 9;
	// items are 16 pixels in the game, drawn smaller here so one fits a line of text
	private static final int ICON_SIZE = 10;
	private static final int ICON_GAP = 3;
	// smallest space between a label and its value
	private static final int COLUMN_GAP = 12;
	// argb
	private static final int BACKGROUND = 0xB0101010;
	private static final int OUTLINE = 0x80808080;
	private static final int LABEL = 0xFFA9B6C2;
	private static final int VALUE = 0xFFFFFFFF;

	private HudPanel() {
	}

	public static int width(Font font, Content content) {
		int labels = 0;
		int values = 0;
		for (Row row : content.rows()) {
			labels = Math.max(labels, font.width(row.label()));
			values = Math.max(values, font.width(row.value()));
		}
		int rows = content.rows().isEmpty() ? 0 : labels + COLUMN_GAP + values;
		return Math.max(ICON_SIZE + ICON_GAP + font.width(title(content)), rows) + PADDING * 2;
	}

	public static int height(Content content) {
		return PADDING * 2 + ROW_HEIGHT * content.rows().size() + TEXT_HEIGHT;
	}

	public static void draw(GuiGraphicsExtractor graphics, Font font, Content content, boolean background) {
		int width = width(font, content);
		if (background) {
			graphics.fill(0, 0, width, height(content), BACKGROUND);
			graphics.outline(0, 0, width, height(content), OUTLINE);
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(PADDING, PADDING - 1);
		graphics.pose().scale(ICON_SIZE / 16f, ICON_SIZE / 16f);
		graphics.item(content.icon(), 0, 0);
		graphics.pose().popMatrix();
		graphics.text(font, title(content), PADDING + ICON_SIZE + ICON_GAP, PADDING, content.titleColor(), true);
		int y = PADDING + ROW_HEIGHT;
		for (Row row : content.rows()) {
			graphics.text(font, row.label(), PADDING, y, LABEL, true);
			graphics.text(font, row.value(), width - PADDING - font.width(row.value()), y, row.color(), true);
			y += ROW_HEIGHT;
		}
	}

	private static Component title(Content content) {
		return Component.literal(content.title()).withStyle(ChatFormatting.BOLD);
	}
}
