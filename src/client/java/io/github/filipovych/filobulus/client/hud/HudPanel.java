package io.github.filipovych.filobulus.client.hud;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

// the flat dark panel every hud uses: a bold title in a colour of its own, then rows of a grey label and a value on the right
// always drawn at 0,0, the caller moves and scales it
public final class HudPanel {
	public record Row(String label, String value) {
	}

	public record Content(String title, int titleColor, List<Row> rows) {
		public Content {
			rows = List.copyOf(rows);
		}
	}

	private static final int PADDING = 5;
	private static final int ROW_HEIGHT = 11;
	private static final int TEXT_HEIGHT = 9;
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
		return Math.max(font.width(title(content)), rows) + PADDING * 2;
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
		graphics.text(font, title(content), PADDING, PADDING, content.titleColor(), true);
		int y = PADDING + ROW_HEIGHT;
		for (Row row : content.rows()) {
			graphics.text(font, row.label(), PADDING, y, LABEL, true);
			graphics.text(font, row.value(), width - PADDING - font.width(row.value()), y, VALUE, true);
			y += ROW_HEIGHT;
		}
	}

	private static Component title(Content content) {
		return Component.literal(content.title()).withStyle(ChatFormatting.BOLD);
	}
}
