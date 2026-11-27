package io.github.filipovych.filobulus.client;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// one bad chat line or tick must not take the game down, so every watcher runs behind this
// a spot that fails says so once in the log, then keeps quiet
public final class Guard {
	private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

	private Guard() {
	}

	public static void run(String what, Runnable step) {
		try {
			step.run();
		} catch (RuntimeException e) {
			if (FAILED.add(what)) {
				FilobulusClient.LOGGER.error("{} failed, the rest of the mod goes on", what, e);
			}
		}
	}
}
