package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.route.CollectionCodec;
import io.github.filipovych.filobulus.route.LibraryRoute;
import io.github.filipovych.filobulus.route.RouteCodec;
import io.github.filipovych.filobulus.route.RouteCollection;
import io.github.filipovych.filobulus.route.RouteFormatException;
import io.github.filipovych.filobulus.route.RouteImport;
import io.github.filipovych.filobulus.route.RouteProfiles;
import io.github.filipovych.filobulus.route.SharedRoute;
import io.github.filipovych.filobulus.route.SplitText;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

// /filo routes: one row per shaft layout with its auto-load switch and the route it uses, and the way routes get in and out
// the switches and picks belong to a profile, the list at the top right swaps between them
public final class RoutesScreen extends RoutesFrame {
	private static final int AUTO_COLUMN = 138;
	private static final int ROUTE_COLUMN = 194;
	private static final int PROFILE_WIDTH = 78;
	private static final int LIST_BACK = 0xF0141618;

	// the same for forgetting the best run of a route: the first click asks, the second does it
	private String confirmReset = "";
	private boolean confirmRemove;
	private boolean profilesOpen;

	public RoutesScreen(Screen parent) {
		super(parent);
	}

	@Override
	protected boolean shafts() {
		return true;
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		List<ShaftLayout> layouts = ShaftLayout.all();
		begin(graphics, layouts.size());
		graphics.text(font, "Shaft", left + 8, top + COLUMNS_TOP, FAINT, true);
		graphics.text(font, "Auto-load", left + AUTO_COLUMN, top + COLUMNS_TOP, FAINT, true);
		graphics.text(font, "Route", left + ROUTE_COLUMN, top + COLUMNS_TOP, FAINT, true);

		graphics.enableScissor(left + 1, listTop, left + panelWidth - 1, listBottom);
		// an open profile list covers the rows, they must not light up under it
		int rowMouseY = profilesOpen ? -1 : mouseY;
		for (int i = 0; i < layouts.size(); i++) {
			int y = listTop + i * ROW - scroll;
			if (y + ROW > listTop && y < listBottom) {
				drawRow(graphics, layouts.get(i), y, mouseX, rowMouseY);
			}
		}
		graphics.disableScissor();

		int y = end(graphics, mouseX, mouseY, layouts.size());
		int x = left + 8;
		if (button(graphics, x, y, buttonWidth("Import"), "Import", TEXT, mouseX, mouseY, () -> importInto(null))) {
			tooltip(graphics, "Import what is on your clipboard: a route collection, or a route for the shaft you are in. "
					+ "Routes copied from Coleweight, SkyHanni or Skyblocker work too. The bundled routes are only a starting point.",
					mouseX, mouseY);
		}
		x += buttonWidth("Import") + GAP;
		if (button(graphics, x, y, buttonWidth("Export all"), "Export all", TEXT, mouseX, mouseY, this::exportAll)) {
			tooltip(graphics, "Copy every route this profile changed as one string that other Filobulus players can import.",
					mouseX, mouseY);
		}
		drawProfiles(graphics, mouseX, mouseY);
	}

	// the name of the profile in use, a click lists the others, plus makes a new one and minus removes this one
	private void drawProfiles(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		RouteProfiles profiles = store.profiles();
		int y = top + 4;
		int small = buttonWidth("+");
		int minusLeft = left + panelWidth - 8 - small;
		int plusLeft = minusLeft - GAP - small;
		int nameLeft = plusLeft - GAP - PROFILE_WIDTH;
		String minus = confirmRemove ? "?" : "-";
		if (button(graphics, minusLeft, y, small, minus, BAD, mouseX, mouseY, this::removeProfile)) {
			tooltip(graphics, "Remove the profile " + profiles.active() + ". Click twice.", mouseX, mouseY);
		}
		if (button(graphics, plusLeft, y, small, "+", GOOD, mouseX, mouseY, this::newProfile)) {
			tooltip(graphics, "A new profile, starting as a copy of this one.", mouseX, mouseY);
		}
		boolean hovered = button(graphics, nameLeft, y, PROFILE_WIDTH, "", TEXT, mouseX, mouseY,
				() -> profilesOpen = !profilesOpen);
		graphics.text(font, clip(profiles.active(), PROFILE_WIDTH - 16), nameLeft + 4, y + 3, TEXT, true);
		graphics.text(font, profilesOpen ? "^" : "v", nameLeft + PROFILE_WIDTH - 9, y + 3, MUTED, true);
		if (hovered && !profilesOpen) {
			tooltip(graphics, "Profiles each keep their own auto-load switches and route picks, like one for you and one to try "
					+ "a friend's routes. Imported routes are shared by all of them.", mouseX, mouseY);
		}
		if (!profilesOpen) {
			return;
		}
		// a click anywhere else closes the list
		clickable(0, 0, width, height, () -> profilesOpen = false, null);
		List<String> names = profiles.names();
		int listTop = y + BUTTON_HEIGHT + 1;
		graphics.fill(nameLeft, listTop, nameLeft + PROFILE_WIDTH, listTop + names.size() * BUTTON_HEIGHT, LIST_BACK);
		graphics.outline(nameLeft - 1, listTop - 1, PROFILE_WIDTH + 2, names.size() * BUTTON_HEIGHT + 2, OUTLINE);
		for (int i = 0; i < names.size(); i++) {
			String name = names.get(i);
			int rowTop = listTop + i * BUTTON_HEIGHT;
			boolean on = name.equals(profiles.active());
			if (mouseX >= nameLeft && mouseX < nameLeft + PROFILE_WIDTH && mouseY >= rowTop && mouseY < rowTop + BUTTON_HEIGHT) {
				graphics.fill(nameLeft, rowTop, nameLeft + PROFILE_WIDTH, rowTop + BUTTON_HEIGHT, HOVER);
			}
			graphics.text(font, clip(name, PROFILE_WIDTH - 8), nameLeft + 4, rowTop + 3, on ? ACCENT : TEXT, true);
			clickable(nameLeft, rowTop, PROFILE_WIDTH, BUTTON_HEIGHT, () -> useProfile(name), null);
		}
	}

	private void useProfile(String name) {
		profilesOpen = false;
		confirmRemove = false;
		if (store.profiles().select(name)) {
			store.save();
			FilobulusClient.profileChanged();
			say("Using the profile " + name + ".", GOOD);
		}
	}

	private void newProfile() {
		profilesOpen = false;
		confirmRemove = false;
		String from = store.profiles().active();
		open(new RouteNameScreen(this, "New profile", "Starts as a copy of " + from + ".", "", null, null, answer -> {
			Optional<String> made = store.profiles().add(answer.name());
			if (made.isEmpty()) {
				say(answer.name().isEmpty() ? "A profile needs a name." : "There is already a profile called " + answer.name() + ".", BAD);
				return;
			}
			store.save();
			say("Made " + made.get() + " from " + from + ". What you change now only changes " + made.get() + ".", GOOD);
		}, null));
	}

	private void removeProfile() {
		profilesOpen = false;
		if (store.profiles().names().size() < 2) {
			say("This is the only profile, it stays.", MUTED);
			return;
		}
		if (!confirmRemove) {
			confirmRemove = true;
			return;
		}
		confirmRemove = false;
		String gone = store.profiles().active();
		store.profiles().remove(gone);
		store.save();
		FilobulusClient.profileChanged();
		say("Removed " + gone + ", now using " + store.profiles().active() + ". Its imported routes are still here.", MUTED);
	}

	private void drawRow(GuiGraphicsExtractor graphics, ShaftLayout layout, int y, int mouseX, int mouseY) {
		String key = layout.key();
		boolean hot = rowHovered(graphics, y, mouseX, mouseY);
		graphics.item(ShaftIcons.of(layout), left + 6, y + 2);
		int titleColor = layout.material().map(ShaftColors::of).orElse(ACCENT);
		graphics.text(font, layout.title(), left + 26, y + 6, titleColor, true);
		if (hot && mouseX < left + AUTO_COLUMN) {
			// the type code as the scoreboard shows it
			tooltip(graphics, layout.isCrystal() ? "Every crystal shaft." : key, mouseX, mouseY);
		}

		boolean auto = store.choices().autoLoad(key);
		pill(graphics, left + AUTO_COLUMN + 8, y + 4, auto, () -> {
			store.choices().setAutoLoad(key, !auto);
			store.save();
		});

		List<LibraryRoute> routes = store.library().routesFor(key);
		LibraryRoute picked = store.library().routeFor(key, store.choices().preferredRouteId(key)).orElse(null);

		// every button has its own column, a row without one leaves that spot empty so the others do not move
		// they only show on the row under the mouse, the list stays quiet
		int by = y + 3;
		int editLeft = left + panelWidth - 8 - buttonWidth("Edit");
		int copyLeft = editLeft - GAP - buttonWidth("Copy");
		int importLeft = copyLeft - GAP - buttonWidth("Import");
		if (hot && picked != null && !picked.bundled()
				&& button(graphics, editLeft, by, buttonWidth("Edit"), "Edit", MUTED, mouseX, mouseY, () -> edit(picked))) {
			tooltip(graphics, "Rename this imported route, or delete it.", mouseX, mouseY);
		}
		if (hot && picked != null
				&& button(graphics, copyLeft, by, buttonWidth("Copy"), "Copy", MUTED, mouseX, mouseY, () -> copy(picked))) {
			tooltip(graphics, "Copy this route in the Coleweight format, which every waypoint mod reads.", mouseX, mouseY);
		}
		if (hot && button(graphics, importLeft, by, buttonWidth("Import"), "Import", TEXT, mouseX, mouseY, () -> importInto(layout))) {
			tooltip(graphics, "Use the route on your clipboard for " + layout.title() + " shafts.", mouseX, mouseY);
		}

		int routeLeft = left + ROUTE_COLUMN;
		int routeWidth = importLeft - GAP - routeLeft;
		if (picked == null) {
			graphics.text(font, "no route yet", routeLeft, y + 6, FAINT, true);
			return;
		}
		// the best run sits right after the route, room is kept for it
		Optional<long[]> best = FilobulusClient.splits().bests().best(picked.id(), picked.stops().size());
		boolean sureReset = confirmReset.equals(picked.id());
		String bestText = best.map(times -> sureReset ? "Reset?" : SplitText.clock(times[times.length - 1])).orElse("");
		int labelWidth = routeWidth - (bestText.isEmpty() ? 0 : font.width(bestText) + 6);
		boolean several = routes.size() > 1;
		// a long name is cut short, the stop count always stays
		String count = " (" + picked.stops().size() + " stops)";
		int nameWidth = labelWidth - (several ? font.width("<  >") : 0);
		if (font.width(picked.name() + count) > nameWidth) {
			// the word goes before the name gets cut
			count = " (" + picked.stops().size() + ")";
		}
		String label = clip(picked.name(), nameWidth - font.width(count)) + count;
		if (several) {
			label = "< " + label + " >";
		}
		graphics.text(font, label, routeLeft, y + 6, several ? TEXT : MUTED, true);
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
		if (!bestText.isEmpty()) {
			int bestLeft = routeLeft + font.width(label) + 6;
			graphics.text(font, bestText, bestLeft, y + 6, sureReset ? BAD : ACCENT, true);
			clickable(bestLeft - 2, y + 3, font.width(bestText) + 4, BUTTON_HEIGHT, () -> resetBest(picked, sureReset), null);
			if (mouseX >= bestLeft - 2 && mouseX < bestLeft + font.width(bestText) + 2 && mouseY >= y + 3 && mouseY < y + 3 + BUTTON_HEIGHT) {
				tooltip(graphics, "Your best run of this route. Click twice to forget it.", mouseX, mouseY);
			}
		}
	}

	private void resetBest(LibraryRoute route, boolean sure) {
		if (!sure) {
			confirmReset = route.id();
			return;
		}
		confirmReset = "";
		FilobulusClient.splits().bests().forget(route.id());
		FilobulusClient.splits().save();
		say("Forgot the best run of " + route.title() + ".", MUTED);
	}

	private void pick(LibraryRoute route) {
		store.choices().pick(route.routeKey(), route.id());
		store.save();
		confirmReset = "";
		FilobulusClient.routeChanged(route.routeKey());
	}

	private void copy(LibraryRoute route) {
		minecraft.keyboardHandler.setClipboard(RouteCodec.toColeweight(route.stops()));
		say("Copied " + route.title() + " (" + route.stops().size() + " stops) to the clipboard.", GOOD);
	}

	// a new name or author for an imported route, its delete button is in there too
	private void edit(LibraryRoute route) {
		String shaft = ShaftLayout.byKey(route.routeKey()).map(ShaftLayout::title).orElse(route.routeKey());
		open(new RouteNameScreen(this, "Edit route", route.stops().size() + " stops for " + shaft + " shafts",
				route.name(), route.author(), null, answer -> {
					store.library().renameUser(route.id(), answer.name(), answer.author()).ifPresent(renamed -> {
						store.save();
						FilobulusClient.routeChanged(renamed.routeKey());
						say("Now called " + renamed.title() + ".", GOOD);
					});
				}, () -> delete(route)));
	}

	private void delete(LibraryRoute route) {
		store.delete(route);
		FilobulusClient.splits().bests().forget(route.id());
		FilobulusClient.splits().save();
		store.save();
		FilobulusClient.routeChanged(route.routeKey());
		say("Deleted " + route.title() + ".", MUTED);
	}

	// target is the row that was clicked, null for the button at the bottom
	private void importInto(ShaftLayout target) {
		try {
			ShaftLayout where = target != null ? target : FilobulusClient.tracker().location().shaft()
					.map(ShaftLayout::of).orElse(null);
			List<SharedRoute> routes = RouteImport.assign(
					RouteImport.read(minecraft.keyboardHandler.getClipboard()), where);
			// a lone route without a name gets one from the player first
			if (routes.size() == 1 && routes.get(0).name().isEmpty()) {
				SharedRoute route = routes.get(0);
				String shaft = ShaftLayout.byKey(route.shaft()).map(ShaftLayout::title).orElse(route.shaft());
				open(new RouteNameScreen(this, "Name this route", route.stops().size() + " stops for " + shaft + " shafts",
						"", "", null,
						answer -> add(List.of(new SharedRoute(route.shaft(), answer.name(), answer.author(), route.stops()))),
						null));
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
		RouteCollection collection = store.picked(minecraft.getUser().getName() + "'s routes");
		if (collection.routes().isEmpty()) {
			say("Nothing to export yet, every shaft uses its bundled route.", MUTED);
			return;
		}
		minecraft.keyboardHandler.setClipboard(CollectionCodec.export(collection));
		say("Copied " + collection.routes().size() + " routes as one string. Paste it to someone, they press Import.", GOOD);
	}
}
