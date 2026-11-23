package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.client.route.RouteStore;
import io.github.filipovych.filobulus.route.LibraryRoute;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// /filo routes: one row per shaft layout with its auto-load switch and the route it uses, and the way routes get in and out
public final class RoutesScreen extends FlatScreen {
	private static final int MAX_WIDTH = 440;
	private static final int MARGIN = 8;
	private static final int ROW = 20;
	private static final int LIST_TOP = 46;
	private static final int FOOTER = 38;
	private static final int AUTO_COLUMN = 140;
	private static final int ROUTE_COLUMN = 194;
	private static final int PILL_WIDTH = 30;
	private static final int GAP = 3;
	private static final int ROW_HOVER = 0x12FFFFFF;
	private static final int PILL_ON = 0xFF2F86B3;
	private static final int SCROLL_STEP = ROW * 2;

	private final Screen parent;
	private final RouteStore store = FilobulusClient.store();
	private int scroll;
	private String status = "Routes copied from Coleweight, SkyHanni or Skyblocker can be imported.";
	private int statusColor = FAINT;

	public RoutesScreen(Screen parent) {
		super(Component.literal("Filobulus routes"));
		this.parent = parent;
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int panelWidth = Math.min(width - MARGIN * 2, MAX_WIDTH);
		int left = (width - panelWidth) / 2;
		int top = MARGIN;
		int bottom = height - MARGIN;
		int listTop = top + LIST_TOP;
		int listBottom = bottom - FOOTER;
		panel(graphics, left, top, panelWidth, bottom - top);

		graphics.text(font, Component.literal("Routes").withStyle(ChatFormatting.BOLD), left + 8, top + 7, ACCENT, true);
		graphics.text(font, clip("The bundled routes are only a starting point. Import better ones.",
				panelWidth - 16), left + 8, top + 19, MUTED, true);
		graphics.text(font, "Shaft", left + 8, top + 34, FAINT, true);
		graphics.text(font, "Auto-load", left + AUTO_COLUMN, top + 34, FAINT, true);
		graphics.text(font, "Route", left + ROUTE_COLUMN, top + 34, FAINT, true);

		List<ShaftLayout> layouts = ShaftLayout.all();
		int maxScroll = Math.max(0, layouts.size() * ROW - (listBottom - listTop));
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		graphics.enableScissor(left + 1, listTop, left + panelWidth - 1, listBottom);
		boolean inList = mouseY >= listTop && mouseY < listBottom;
		for (int i = 0; i < layouts.size(); i++) {
			int y = listTop + i * ROW - scroll;
			if (y + ROW > listTop && y < listBottom) {
				drawRow(graphics, layouts.get(i), left, y, panelWidth, inList ? mouseX : -1, mouseY);
			}
		}
		graphics.disableScissor();
		if (maxScroll > 0) {
			int track = listBottom - listTop;
			int thumb = Math.max(12, track * track / (layouts.size() * ROW));
			int thumbTop = listTop + (track - thumb) * scroll / maxScroll;
			graphics.fill(left + panelWidth - 3, thumbTop, left + panelWidth - 1, thumbTop + thumb, OUTLINE);
		}
		// clicks in the header and footer must not reach a row that is scrolled under them
		clickable(left, top, panelWidth, LIST_TOP, null, null);
		clickable(left, listBottom, panelWidth, FOOTER, null, null);

		graphics.text(font, clip(status, panelWidth - 16), left + 8, listBottom + 5, statusColor, true);
		int y = bottom - BUTTON_HEIGHT - 6;
		int doneWidth = buttonWidth("Done");
		button(graphics, left + panelWidth - 8 - doneWidth, y, doneWidth, "Done", ACCENT, mouseX, mouseY, this::onClose);
	}

	private void drawRow(GuiGraphicsExtractor graphics, ShaftLayout layout, int left, int y, int panelWidth,
			int mouseX, int mouseY) {
		String key = layout.key();
		if (mouseX >= left && mouseX < left + panelWidth && mouseY >= y && mouseY < y + ROW) {
			graphics.fill(left + 1, y, left + panelWidth - 1, y + ROW, ROW_HOVER);
		}
		graphics.item(ShaftIcons.of(layout), left + 6, y + 2);
		int titleColor = layout.material().map(ShaftColors::of).orElse(ACCENT);
		graphics.text(font, layout.title(), left + 26, y + 6, titleColor, true);
		// the type code as the scoreboard shows it, left out when the name is too long to fit it
		String code = layout.isCrystal() ? "all crystal" : key;
		int codeLeft = left + 31 + font.width(layout.title());
		if (codeLeft + font.width(code) <= left + AUTO_COLUMN) {
			graphics.text(font, code, codeLeft, y + 6, FAINT, true);
		}

		boolean auto = store.choices().autoLoad(key);
		int pillLeft = left + AUTO_COLUMN + 8;
		if (auto) {
			graphics.fill(pillLeft, y + 4, pillLeft + PILL_WIDTH, y + 16, PILL_ON);
		} else {
			graphics.outline(pillLeft, y + 4, PILL_WIDTH, 12, FAINT);
		}
		graphics.centeredText(font, auto ? "ON" : "OFF", pillLeft + PILL_WIDTH / 2, y + 6, auto ? TEXT : FAINT);
		clickable(pillLeft, y + 4, PILL_WIDTH, 12, () -> {
			store.choices().setAutoLoad(key, !auto);
			store.save();
		}, null);

		List<LibraryRoute> routes = store.library().routesFor(key);
		LibraryRoute picked = store.library().routeFor(key, store.choices().preferredRouteId(key)).orElse(null);

		int x = left + panelWidth - 8 + GAP;
		int routeLeft = left + ROUTE_COLUMN;
		int routeWidth = x - GAP - routeLeft;
		if (picked == null) {
			graphics.text(font, "no route yet", routeLeft, y + 6, FAINT, true);
			return;
		}
		boolean several = routes.size() > 1;
		// a long name is cut short, the stop count always stays
		String count = " (" + picked.stops().size() + " stops)";
		String label = clip(picked.name(), routeWidth - font.width(count) - (several ? font.width("<  >") : 0)) + count;
		graphics.text(font, several ? "< " + label + " >" : label, routeLeft, y + 6, several ? TEXT : MUTED, true);
		boolean hovered = mouseX >= routeLeft && mouseX < routeLeft + routeWidth && mouseY >= y && mouseY < y + ROW;
		if (several) {
			int at = routes.indexOf(picked);
			clickable(routeLeft, y, routeWidth, ROW,
					() -> pick(routes.get((at + 1) % routes.size())),
					() -> pick(routes.get((at + routes.size() - 1) % routes.size())));
		}
		if (hovered && (several || !picked.author().isEmpty())) {
			String credit = picked.author().isEmpty() ? "" : "By " + picked.author() + ". ";
			tooltip(graphics, credit + (several ? "Click for the next route of this shaft, right click for the one before." : ""),
					mouseX, mouseY);
		}
	}

	private void pick(LibraryRoute route) {
		store.choices().pick(route.routeKey(), route.id());
		store.save();
		FilobulusClient.routeChanged(route.routeKey());
	}

	private void say(String text, int color) {
		status = text;
		statusColor = color;
	}

	private void tooltip(GuiGraphicsExtractor graphics, String text, int mouseX, int mouseY) {
		graphics.setTooltipForNextFrame(font, Component.literal(text), mouseX, mouseY);
	}

	private String clip(String text, int maxWidth) {
		return font.width(text) <= maxWidth ? text : font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("..."))) + "...";
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
