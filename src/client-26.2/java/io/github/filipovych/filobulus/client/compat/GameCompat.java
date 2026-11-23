package io.github.filipovych.filobulus.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// the game calls that were renamed in 26.2, src/client-26.1 has this same class for the older game
public final class GameCompat {
	private GameCompat() {
	}

	public static Screen screen(Minecraft minecraft) {
		return minecraft.gui.screen();
	}

	public static void setScreen(Minecraft minecraft, Screen screen) {
		minecraft.gui.setScreen(screen);
	}

	public static void say(Minecraft minecraft, Component message) {
		minecraft.gui.hud.getChat().addClientSystemMessage(message);
	}
}
