package io.github.filipovych.filobulus.client.alert;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.detect.GameText;
import io.github.filipovych.filobulus.detect.LocationParser;
import io.github.filipovych.filobulus.effect.EffectReader;
import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.sound.SoundSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

// knows how long the cold resistance potion still has, from the tab list widget, the sidebar or the /effects menu
// warns in a jasper shaft when there is none, and when it is about to run out
// when the game shows no effects anywhere the mod can not know, and then it stays quiet
public final class ColdWatch {
	private static final int INTERVAL = 20;
	private static final long LEAD = 30_000;
	private static final int COLOR = 0x7FD4FF;

	// wall clock time it runs out, 0 while not known
	private long until;
	// true when the game showed the effects and cold resistance was not among them
	private boolean none;
	private int ticks;
	// the shaft we already warned for, by when we got in, so each warning comes once per shaft
	private long warnedNone;
	private long warnedLow;

	// milliseconds it has left, empty while it is not known
	public OptionalLong left() {
		if (none) {
			return OptionalLong.of(0);
		}
		return until == 0 ? OptionalLong.empty() : OptionalLong.of(Math.max(0, until - System.currentTimeMillis()));
	}

	public void tick(Minecraft minecraft) {
		if (!FilobulusConfig.enabled || minecraft.player == null || ticks++ % INTERVAL != 0) {
			return;
		}
		long now = System.currentTimeMillis();
		readMenu(minecraft, now);
		EffectReader.Read read = EffectReader.read(GameText.tabLines(minecraft), GameText.sidebarLines(minecraft));
		if (read.millis().isPresent()) {
			none = false;
			// a line without a time keeps what we knew
			until = read.millis().getAsLong() > 0 ? now + read.millis().getAsLong() : Math.max(until, now + 1000);
		} else if (read.widget()) {
			none = true;
			until = 0;
		}
		Location location = FilobulusClient.tracker().location();
		boolean jasper = location.area() == Area.MINESHAFT
				&& location.shaft().filter(type -> type.material() == ShaftMaterial.JASPER).isPresent();
		long shaft = FilobulusClient.tracker().shaftEnteredAt();
		if (!AlertsConfig.coldPotion || !jasper || shaft == 0) {
			return;
		}
		OptionalLong left = left();
		if (left.isEmpty()) {
			return;
		}
		if (left.getAsLong() == 0 && warnedNone != shaft) {
			warnedNone = shaft;
			Alerts.show("cold", "NO COLD POT", "Cold Resistance is not active", COLOR, 6, Alerts.LONG, SoundSlot.COLD_POTION);
		} else if (left.getAsLong() > 0 && left.getAsLong() <= LEAD && warnedLow != shaft) {
			warnedLow = shaft;
			Alerts.show("cold", "COLD POT", "30 seconds left", COLOR, 5, Alerts.LONG, SoundSlot.COLD_POTION);
		}
	}

	// the /effects menu while the player has it open, the item of the potion has the exact time in its lore
	private void readMenu(Minecraft minecraft, long now) {
		if (!(GameCompat.screen(minecraft) instanceof AbstractContainerScreen<?> screen)
				|| !EffectReader.isEffectsMenu(LocationParser.stripColor(screen.getTitle().getString()))) {
			return;
		}
		for (Slot slot : screen.getMenu().slots) {
			ItemStack stack = slot.getItem();
			if (stack.isEmpty()) {
				continue;
			}
			List<String> lore = new ArrayList<>();
			ItemLore lines = stack.get(DataComponents.LORE);
			if (lines != null) {
				for (Component line : lines.lines()) {
					lore.add(LocationParser.stripColor(line.getString()));
				}
			}
			OptionalLong found = EffectReader.menuItem(LocationParser.stripColor(stack.getHoverName().getString()), lore);
			if (found.isPresent() && found.getAsLong() > 0) {
				none = false;
				until = now + found.getAsLong();
				return;
			}
		}
	}
}
