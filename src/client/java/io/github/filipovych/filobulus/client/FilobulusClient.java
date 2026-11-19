package io.github.filipovych.filobulus.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FilobulusClient implements ClientModInitializer {
	public static final String MOD_ID = "filobulus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		LOGGER.info("Filobulus loaded");
	}
}
