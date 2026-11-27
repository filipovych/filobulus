package io.github.filipovych.filobulus.client.alert;

import io.github.filipovych.filobulus.alert.ShaftPortal;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.Guard;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.detect.LocationParser;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.sound.SoundSlot;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

// counts down the 30 seconds a spawned shaft portal stays and warns before it closes
// it only listens to chat, nothing is changed
public final class PortalWatch {
	private static final long LEAD = 5000;
	private static final int COLOR = 0xFF5555;

	private final ShaftPortal portal = new ShaftPortal();
	private ClientLevel level;

	public void register() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> Guard.run("portal chat", () -> {
			if (!overlay && FilobulusConfig.enabled && AlertsConfig.portal
					&& ShaftPortal.spawned(LocationParser.stripColor(message.getString()))) {
				portal.spawn(System.currentTimeMillis());
			}
		}));
	}

	public ShaftPortal portal() {
		return portal;
	}

	public void tick(Minecraft minecraft) {
		if (minecraft.level != level || FilobulusClient.tracker().location().area() == Area.MINESHAFT) {
			// we went in, or somewhere else
			level = minecraft.level;
			portal.clear();
			return;
		}
		if (portal.poll(System.currentTimeMillis(), LEAD) == ShaftPortal.Event.CLOSING && AlertsConfig.portal) {
			Alerts.show("portal", "PORTAL CLOSING", "5 seconds to get in", COLOR, 7, Alerts.LONG, SoundSlot.PORTAL);
		}
	}
}
