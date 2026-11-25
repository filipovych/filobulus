package io.github.filipovych.filobulus.client.alert;

import io.github.filipovych.filobulus.alert.AlertQueue;
import io.github.filipovych.filobulus.alert.AlertQueue.Alert;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.sound.SoundSlot;

// the one way to put a big alert on screen, AlertHud draws what is in the queue
public final class Alerts {
	public static final AlertQueue QUEUE = new AlertQueue();
	public static final long SHORT = 2500;
	public static final long LONG = 4000;

	private Alerts() {
	}

	// sound may be null, color is rgb, what the sound is comes from the sounds screen
	public static void show(String id, String title, String subtitle, int color, int priority, long millis, SoundSlot sound) {
		if (!AlertsConfig.alerts) {
			return;
		}
		QUEUE.show(new Alert(id, title, subtitle, color & 0xFFFFFF, priority, millis), System.currentTimeMillis());
		if (sound != null) {
			FilobulusClient.sounds().play(sound);
		}
	}
}
