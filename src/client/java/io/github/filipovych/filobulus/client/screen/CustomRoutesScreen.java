package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.hud.RouteIcons;
import io.github.filipovych.filobulus.route.CustomRoute;
import io.github.filipovych.filobulus.route.CustomRoutes;
import io.github.filipovych.filobulus.route.RouteCodec;
import io.github.filipovych.filobulus.route.RouteFormatException;
import io.github.filipovych.filobulus.route.RouteImport;
import io.github.filipovych.filobulus.route.SharedRoute;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

// the second tab of /filo routes: the player's routes for everything that is not a shaft, each with a name and an icon
// one route per island can be in use, that one is drawn while the player is there
public final class CustomRoutesScreen extends RoutesFrame {
	private static final String MINESHAFT = "Mineshaft";

	public CustomRoutesScreen(Screen parent) {
		super(parent, "Copy a route in any waypoint mod, then press Import here.");
	}

	@Override
	protected boolean shafts() {
		return false;
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		List<CustomRoute> routes = store.custom().all();
		begin(graphics, mouseX, mouseY, "Your own routes for outside the shafts, like a Crystal Hollows loop.", routes.size());
		int editLeft = left + panelWidth - 8 - buttonWidth("Edit");
		int copyLeft = editLeft - GAP - buttonWidth("Copy");
		int pillLeft = copyLeft - 8 - PILL_WIDTH;
		int stopsRight = pillLeft - 10;
		int areaLeft = left + Math.max(120, panelWidth * 38 / 100);
		graphics.text(font, "Route", left + 8, top + 34, FAINT, true);
		graphics.text(font, "Shows in", areaLeft, top + 34, FAINT, true);
		graphics.text(font, "Stops", stopsRight - font.width("Stops"), top + 34, FAINT, true);
		graphics.text(font, "In use", pillLeft, top + 34, FAINT, true);

		if (routes.isEmpty()) {
			int middle = (listTop + listBottom) / 2;
			graphics.centeredText(font, "No routes of your own yet.", width / 2, middle - 10, MUTED);
			graphics.centeredText(font, "Coal, gemstones, powder: bring any route and give it a name and an icon.",
					width / 2, middle + 2, FAINT);
		}
		graphics.enableScissor(left + 1, listTop, left + panelWidth - 1, listBottom);
		for (int i = 0; i < routes.size(); i++) {
			int y = listTop + i * ROW - scroll;
			if (y + ROW <= listTop || y >= listBottom) {
				continue;
			}
			CustomRoute route = routes.get(i);
			rowHovered(graphics, y, mouseX, mouseY);
			graphics.item(RouteIcons.of(route.icon()), left + 6, y + 2);
			graphics.text(font, clip(route.name(), areaLeft - left - 32), left + 26, y + 6, route.on() ? TEXT : MUTED, true);
			graphics.text(font, clip(route.everywhere() ? "Everywhere" : route.area(), stopsRight - 30 - areaLeft),
					areaLeft, y + 6, route.everywhere() ? FAINT : MUTED, true);
			String stops = route.stops().size() + (route.loop() ? "" : " once");
			graphics.text(font, stops, stopsRight - font.width(stops), y + 6, MUTED, true);
			pill(graphics, pillLeft, y + 4, route.on(), () -> use(route));
			if (button(graphics, copyLeft, y + 3, buttonWidth("Copy"), "Copy", MUTED, mouseX, mouseY, () -> copy(route))) {
				tooltip(graphics, "Copy this route in the Coleweight format, which every waypoint mod reads.", mouseX, mouseY);
			}
			if (button(graphics, editLeft, y + 3, buttonWidth("Edit"), "Edit", MUTED, mouseX, mouseY, () -> edit(route))) {
				tooltip(graphics, "Change the name, the icon and where it shows, or delete it.", mouseX, mouseY);
			}
		}
		graphics.disableScissor();

		int y = end(graphics, mouseX, mouseY, routes.size());
		if (button(graphics, left + 8, y, buttonWidth("Import"), "Import", TEXT, mouseX, mouseY, this::importRoute)) {
			tooltip(graphics, "Add the route on your clipboard. It is set up for the island you are on.", mouseX, mouseY);
		}
	}

	// the island we are on, a route of our own is never for a shaft
	private static String here() {
		String area = FilobulusClient.tracker().areaName();
		return area.equals(MINESHAFT) ? "" : area;
	}

	private void use(CustomRoute route) {
		store.custom().use(route.id(), !route.on());
		store.save();
		String where = route.everywhere() ? "everywhere outside the shafts" : "in " + route.area();
		say(route.on() ? route.name() + " is off." : route.name() + " now shows " + where + ".", route.on() ? MUTED : GOOD);
	}

	private void copy(CustomRoute route) {
		minecraft.keyboardHandler.setClipboard(RouteCodec.toColeweight(route.stops()));
		say("Copied " + route.name() + " (" + route.stops().size() + " stops) to the clipboard.", GOOD);
	}

	private void edit(CustomRoute route) {
		open(new RouteNameScreen(this, "Edit route", route.stops().size() + " stops", route.name(), route.route().author(),
				new RouteNameScreen.Own(route.icon(), route.area(), route.loop(), here()), answer -> {
					CustomRoutes custom = store.custom();
					String name = answer.name().isEmpty() ? route.name() : answer.name();
					custom.replace(route.edited(name, answer.author(), answer.icon(), answer.area(), answer.loop()));
					if (route.on()) {
						// it may have moved to an island that already had a route in use
						custom.use(route.id(), true);
					}
					store.save();
					say("Saved " + name + ".", GOOD);
				}, () -> {
					store.custom().remove(route.id());
					FilobulusClient.splits().bests().forget(route.id());
					FilobulusClient.splits().save();
					store.save();
					say("Deleted " + route.name() + ".", MUTED);
				}));
	}

	private void importRoute() {
		try {
			List<SharedRoute> routes = RouteImport.read(minecraft.keyboardHandler.getClipboard()).routes();
			if (routes.stream().allMatch(SharedRoute::hasShaft)) {
				say("Those are shaft routes. Import them on the Shafts tab.", BAD);
				return;
			}
			String here = here();
			if (routes.size() > 1) {
				for (SharedRoute route : routes) {
					store.custom().add(route.name(), route.author(), RouteIcons.guess(route.name()), here, true, route.stops(), null);
				}
				store.save();
				say("Imported " + routes.size() + " routes. Switch on the one you want to use.", GOOD);
				return;
			}
			SharedRoute route = routes.get(0);
			open(new RouteNameScreen(this, "Name this route", route.stops().size() + " stops", route.name(), route.author(),
					new RouteNameScreen.Own("", here, true, here), answer -> {
						CustomRoute added = store.custom().add(answer.name(), answer.author(), answer.icon(), answer.area(),
								answer.loop(), route.stops(), null);
						store.custom().use(added.id(), true);
						store.save();
						say("Added " + added.name() + " (" + added.stops().size() + " stops) and switched it on.", GOOD);
					}, null));
		} catch (RouteFormatException e) {
			say(e.getMessage(), BAD);
		}
	}
}
