package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.client.route.RouteStore;
import io.github.filipovych.filobulus.route.CollectionCodec;
import io.github.filipovych.filobulus.route.LibraryRoute;
import io.github.filipovych.filobulus.route.RouteCodec;
import io.github.filipovych.filobulus.route.RouteCollection;
import io.github.filipovych.filobulus.route.RouteFormatException;
import io.github.filipovych.filobulus.route.RouteImport;
import io.github.filipovych.filobulus.route.SharedRoute;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import java.util.ArrayList;
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
	// id of the route whose delete button was clicked once, the second click deletes
	private String confirmDelete = "";

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
		scrollbar(graphics, left + panelWidth - 1, listTop, listBottom, layouts.size() * ROW, scroll);
		// clicks in the header and footer must not reach a row that is scrolled under them
		clickable(left, top, panelWidth, LIST_TOP, null, null);
		clickable(left, listBottom, panelWidth, FOOTER, null, null);

		graphics.text(font, clip(status, panelWidth - 16), left + 8, listBottom + 5, statusColor, true);
		int y = bottom - BUTTON_HEIGHT - 6;
		int x = left + 8;
		if (button(graphics, x, y, buttonWidth("Import"), "Import", TEXT, mouseX, mouseY, () -> importInto(null))) {
			tooltip(graphics, "Import what is on your clipboard: a route collection, or a route for the shaft you are in.",
					mouseX, mouseY);
		}
		x += buttonWidth("Import") + GAP;
		if (button(graphics, x, y, buttonWidth("Export all"), "Export all", TEXT, mouseX, mouseY, this::exportAll)) {
			tooltip(graphics, "Copy every route you changed as one string that other Filobulus players can import.",
					mouseX, mouseY);
		}
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

		// buttons from the right edge inwards
		int by = y + 3;
		int x = left + panelWidth - 8;
		if (picked != null && !picked.bundled()) {
			boolean sure = confirmDelete.equals(picked.id());
			String label = sure ? "Sure?" : "x";
			x -= buttonWidth(label);
			if (button(graphics, x, by, buttonWidth(label), label, BAD, mouseX, mouseY, () -> delete(picked, sure))) {
				tooltip(graphics, "Delete this imported route.", mouseX, mouseY);
			}
			x -= GAP;
		}
		if (picked != null) {
			x -= buttonWidth("Copy");
			if (button(graphics, x, by, buttonWidth("Copy"), "Copy", MUTED, mouseX, mouseY, () -> copy(picked))) {
				tooltip(graphics, "Copy this route in the Coleweight format, which every waypoint mod reads.", mouseX, mouseY);
			}
			x -= GAP;
		}
		x -= buttonWidth("Import");
		if (button(graphics, x, by, buttonWidth("Import"), "Import", TEXT, mouseX, mouseY, () -> importInto(layout))) {
			tooltip(graphics, "Use the route on your clipboard for " + layout.title() + " shafts.", mouseX, mouseY);
		}

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
		confirmDelete = "";
		FilobulusClient.routeChanged(route.routeKey());
	}

	private void copy(LibraryRoute route) {
		minecraft.keyboardHandler.setClipboard(RouteCodec.toColeweight(route.stops()));
		say("Copied " + route.title() + " (" + route.stops().size() + " stops) to the clipboard.", GOOD);
	}

	private void delete(LibraryRoute route, boolean sure) {
		if (!sure) {
			confirmDelete = route.id();
			return;
		}
		store.delete(route);
		FilobulusClient.splits().bests().forget(route.id());
		FilobulusClient.splits().save();
		store.save();
		confirmDelete = "";
		FilobulusClient.routeChanged(route.routeKey());
		say("Deleted " + route.title() + ".", MUTED);
	}

	// target is the row that was clicked, null for the button at the bottom
	private void importInto(ShaftLayout target) {
		confirmDelete = "";
		try {
			ShaftLayout where = target != null ? target : FilobulusClient.tracker().location().shaft()
					.map(ShaftLayout::of).orElse(null);
			List<SharedRoute> routes = RouteImport.assign(
					RouteImport.read(minecraft.keyboardHandler.getClipboard()), where);
			// a lone route without a name gets one from the player first
			if (routes.size() == 1 && routes.get(0).name().isEmpty()) {
				GameCompat.setScreen(minecraft, new RouteNameScreen(this, routes.get(0), named -> add(List.of(named))));
			} else {
				add(routes);
			}
		} catch (RouteFormatException e) {
			say(e.getMessage(), BAD);
		}
	}

	private void add(List<SharedRoute> routes) {
		List<String> titles = new ArrayList<>();
		for (SharedRoute route : routes) {
			LibraryRoute added = store.add(route);
			titles.add(added.title());
			FilobulusClient.routeChanged(added.routeKey());
		}
		store.save();
		say(titles.size() == 1
				? "Now using " + titles.get(0) + " (" + routes.get(0).stops().size() + " stops)."
				: "Imported " + titles.size() + " routes: " + String.join(", ", titles), GOOD);
	}

	private void exportAll() {
		confirmDelete = "";
		RouteCollection collection = store.picked(minecraft.getUser().getName() + "'s routes");
		if (collection.routes().isEmpty()) {
			say("Nothing to export yet, every shaft uses its bundled route.", MUTED);
			return;
		}
		minecraft.keyboardHandler.setClipboard(CollectionCodec.export(collection));
		say("Copied " + collection.routes().size() + " routes as one string. Paste it to someone, they press Import.", GOOD);
	}

	private void say(String text, int color) {
		status = text;
		statusColor = color;
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
