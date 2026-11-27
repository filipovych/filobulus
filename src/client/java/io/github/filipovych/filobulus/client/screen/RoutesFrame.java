package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.route.RouteStore;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// what the two routes screens share: the panel, the title with its tabs, the list area, the status line and the done button
abstract class RoutesFrame extends FlatScreen {
	protected static final int MAX_WIDTH = 470;
	protected static final int MARGIN = 8;
	protected static final int ROW = 20;
	protected static final int LIST_TOP = 34;
	// where the column names of a tab go
	protected static final int COLUMNS_TOP = 22;
	protected static final int FOOTER = 38;
	protected static final int GAP = 3;
	protected static final int ROW_HOVER = 0x12FFFFFF;
	private static final int SCROLL_STEP = ROW * 2;

	protected final Screen parent;
	protected final RouteStore store = FilobulusClient.store();
	// where things are this frame, set by begin
	protected int left;
	protected int top;
	protected int panelWidth;
	protected int listTop;
	protected int listBottom;
	protected int scroll;
	// empty until something happens
	private String status = "";
	private int statusColor = FAINT;

	protected RoutesFrame(Screen parent) {
		super(Component.literal("Filobulus routes"));
		this.parent = parent;
		if (store.readOnly()) {
			say("Changes are not saved, routes.json could not be read.", BAD);
		}
	}

	protected abstract boolean shafts();

	// the panel and where the list goes, rows is how many the list has
	// the title and the tabs are drawn by end, after the rows, so a row scrolled under them can not take their click
	protected void begin(GuiGraphicsExtractor graphics, int rows) {
		panelWidth = Math.min(width - MARGIN * 2, MAX_WIDTH);
		left = (width - panelWidth) / 2;
		top = MARGIN;
		listTop = top + LIST_TOP;
		listBottom = height - MARGIN - FOOTER;
		panel(graphics, left, top, panelWidth, height - MARGIN * 2);
		scroll = Math.max(0, Math.min(scroll, rows * ROW - (listBottom - listTop)));
	}

	// call after the rows: the scrollbar, the status line and the done button, gives back where the footer buttons go
	protected int end(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int rows) {
		scrollbar(graphics, left + panelWidth - 1, listTop, listBottom, rows * ROW, scroll);
		// clicks in the header and footer must not reach a row that is scrolled under them
		clickable(left, top, panelWidth, LIST_TOP, null, null);
		clickable(left, listBottom, panelWidth, FOOTER, null, null);
		Component title = Component.literal("Routes").withStyle(ChatFormatting.BOLD);
		graphics.text(font, title, left + 8, top + 7, ACCENT, true);
		int x = left + 16 + font.width(title);
		x += tab(graphics, x, top + 4, "Shafts", shafts(), mouseX, mouseY, () -> open(new RoutesScreen(parent)));
		tab(graphics, x, top + 4, "Islands", !shafts(), mouseX, mouseY, () -> open(new CustomRoutesScreen(parent)));
		graphics.text(font, clip(status, panelWidth - 16), left + 8, listBottom + 5, statusColor, true);
		int y = height - MARGIN - BUTTON_HEIGHT - 6;
		int doneWidth = buttonWidth("Done");
		button(graphics, left + panelWidth - 8 - doneWidth, y, doneWidth, "Done", ACCENT, mouseX, mouseY, this::onClose);
		return y;
	}

	protected boolean rowHovered(GuiGraphicsExtractor graphics, int y, int mouseX, int mouseY) {
		boolean inList = mouseY >= listTop && mouseY < listBottom;
		boolean hovered = inList && mouseX >= left && mouseX < left + panelWidth && mouseY >= y && mouseY < y + ROW;
		if (hovered) {
			graphics.fill(left + 1, y, left + panelWidth - 1, y + ROW, ROW_HOVER);
		}
		return hovered;
	}

	protected void say(String text, int color) {
		status = text;
		statusColor = color;
	}

	protected void open(Screen screen) {
		GameCompat.setScreen(minecraft, screen);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= (int) Math.signum(scrollY) * SCROLL_STEP;
		return true;
	}

	@Override
	public void onClose() {
		GameCompat.setScreen(minecraft, parent);
	}
}
