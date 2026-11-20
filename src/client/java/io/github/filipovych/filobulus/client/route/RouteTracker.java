package io.github.filipovych.filobulus.client.route;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.RoutesConfig;
import io.github.filipovych.filobulus.client.detect.GameText;
import io.github.filipovych.filobulus.detect.LocationParser;
import io.github.filipovych.filobulus.detect.SteadyLocation;
import io.github.filipovych.filobulus.route.LibraryRoute;
import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.route.RouteProgress;
import io.github.filipovych.filobulus.route.RouteSelector;
import io.github.filipovych.filobulus.route.Waypoint;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

// runs every client tick: works out where we are, keeps the selector up to date and walks the active route
public final class RouteTracker {
	// tab list and sidebar only change now and then, twice a second is plenty
	private static final int LOCATION_INTERVAL = 10;

	private final RouteSelector selector;
	private final SteadyLocation location = new SteadyLocation();
	// a new level means the server moved us, so the old shaft is gone for sure
	private ClientLevel level;
	// dev client only, stands in for a real shaft so routes can be tried in single player
	private Location pretend;
	private LibraryRoute tracked;
	private RouteProgress progress;
	private int ticks;
	// wall clock time we got into the shaft, 0 while not in one
	private long shaftEnteredAt;
	// read by the renderer, so it is swapped as a whole and never changed in place
	private volatile List<Waypoint> upcoming = List.of();

	public RouteTracker(RouteSelector selector) {
		this.selector = selector;
	}

	public List<Waypoint> upcoming() {
		return upcoming;
	}

	public Location location() {
		return location.current();
	}

	public Optional<RouteProgress> progress() {
		return Optional.ofNullable(progress);
	}

	public long shaftEnteredAt() {
		return shaftEnteredAt;
	}

	public void pretend(Location location) {
		pretend = location;
	}

	// /filo load of the route that is already showing starts it again from the first stop
	public void restart() {
		tracked = null;
	}

	public void tick(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.level == null || !FilobulusConfig.enabled) {
			level = null;
			location.reset();
			selector.onLocation(location.current());
			upcoming = List.of();
			shaftEnteredAt = 0;
			return;
		}
		if (minecraft.level != level) {
			level = minecraft.level;
			location.reset();
			ticks = 0;
		}
		if (ticks++ % LOCATION_INTERVAL == 0) {
			Location read = pretend != null
					? pretend
					: LocationParser.parse(GameText.tabLines(minecraft), GameText.sidebarLines(minecraft));
			selector.onLocation(location.update(read)).ifPresent(route -> announce(minecraft, route));
		}
		if (location.current().area() != Area.MINESHAFT) {
			shaftEnteredAt = 0;
		} else if (shaftEnteredAt == 0) {
			shaftEnteredAt = System.currentTimeMillis();
		}

		LibraryRoute active = selector.active().orElse(null);
		boolean changed = active != tracked;
		if (changed) {
			tracked = active;
			progress = active == null ? null : new RouteProgress(active.stops());
		}
		if (progress == null) {
			upcoming = List.of();
			return;
		}
		// measured from the feet, the same point f3 shows
		changed |= progress.update(
				minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ(), RoutesConfig.reach);
		if (changed) {
			upcoming = List.copyOf(progress.remaining());
		}
		if (progress.finished()) {
			// gemstones do not respawn in a shaft, so a finished route is cleared instead of looped
			selector.unload();
		}
	}

	private void announce(Minecraft minecraft, LibraryRoute route) {
		if (!RoutesConfig.announceLoad) {
			return;
		}
		String code = location.current().shaft().map(type -> type.code()).orElse(route.title());
		minecraft.gui.hud.getChat().addClientSystemMessage(
				FilobulusClient.chatLine("Loaded " + code + " route (" + route.stops().size() + " stops)"));
	}
}
