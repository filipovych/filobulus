package io.github.filipovych.filobulus.client.alert;

import io.github.filipovych.filobulus.alert.AlertQueue;
import io.github.filipovych.filobulus.alert.AlertQueue.Alert;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

// the one way to put a big alert on screen, AlertHud draws what is in the queue
public final class Alerts {
	public static final AlertQueue QUEUE = new AlertQueue();
	public static final long SHORT = 2500;
	public static final long LONG = 4000;

	private Alerts() {
	}

	// sound may be null, color is rgb
	public static void show(String id, String title, String subtitle, int color, int priority, long millis, SoundEvent sound) {
		QUEUE.show(new Alert(id, title, subtitle, color & 0xFFFFFF, priority, millis), System.currentTimeMillis());
		if (sound != null && AlertsConfig.sounds) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1f, 1f));
		}
	}
}
