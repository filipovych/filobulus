package io.github.filipovych.filobulus.client.alert;

import io.github.filipovych.filobulus.alert.WispTimer;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.sound.SoundSlot;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

// starts the wisp timer when we use a will-o'-wisp and warns when its five minutes are nearly over
// it only watches our own right click, the click itself is left alone
public final class WispWatch {
	private static final String WISP_ID = "WILL_O_WISP";
	private static final long LEAD = 10_000;
	private static final int COLOR = 0xFF6B6B;

	private final WispTimer timer = new WispTimer();
	private ClientLevel level;

	public void register() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			used(player, hand);
			return InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			used(player, hand);
			return InteractionResult.PASS;
		});
	}

	public WispTimer timer() {
		return timer;
	}

	// /filo wisp, for a wisp somebody else placed or one we did not notice
	public void placed() {
		timer.placed(System.currentTimeMillis());
	}

	public void tick(Minecraft minecraft) {
		if (minecraft.level != level) {
			// a wisp stays behind when we change lobby or go into a shaft
			level = minecraft.level;
			timer.clear();
		}
		if (!AlertsConfig.wisp || FilobulusClient.tracker().location().area() == Area.ELSEWHERE) {
			return;
		}
		switch (timer.poll(System.currentTimeMillis(), LEAD)) {
			case SOON -> Alerts.show("wisp", "WISP", "10 seconds left", COLOR, 4, Alerts.SHORT, null);
			case GONE -> Alerts.show("wisp", "WISP GONE", "Place a new one", COLOR, 5, Alerts.LONG, SoundSlot.WISP_GONE);
			case NONE -> {
			}
		}
	}

	private void used(Player player, InteractionHand hand) {
		if (player != Minecraft.getInstance().player) {
			return;
		}
		ItemStack stack = player.getItemInHand(hand);
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		// skyblock items carry their id in the custom data
		if (data != null && WISP_ID.equals(data.copyTag().getStringOr("id", ""))) {
			placed();
		}
	}
}
