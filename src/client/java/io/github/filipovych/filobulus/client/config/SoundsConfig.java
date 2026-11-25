package io.github.filipovych.filobulus.client.config;

import com.teamresourceful.resourcefulconfig.api.annotations.Category;
import com.teamresourceful.resourcefulconfig.api.annotations.Comment;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigButton;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigInfo;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.screen.SoundsScreen;
import net.minecraft.client.Minecraft;

@Category("sounds")
@ConfigInfo(title = "Sounds", description = "What every alert and corpse tier plays, your own .ogg files too.", icon = "volume-2")
public final class SoundsConfig {
	@ConfigEntry(id = "sounds", translation = "filobulus.config.sounds.sounds")
	@Comment("Play the sounds of alerts, achievements and the corpse reveal. Off keeps everything silent.")
	public static boolean sounds = true;

	@ConfigButton(title = "Pick sounds", text = "Open")
	@Comment("Pick the sound and the volume of every alert and every corpse tier. Also at /filo sounds.")
	public static final Runnable openSounds = () -> {
		Minecraft minecraft = Minecraft.getInstance();
		GameCompat.setScreen(minecraft, new SoundsScreen(GameCompat.screen(minecraft)));
	};

	@ConfigButton(title = "Your own sounds", text = "Open folder")
	@Comment("Opens config/filobulus/sounds. Every .ogg file in it can be picked for any alert.")
	public static final Runnable openFolder = () -> FilobulusClient.sounds().openFolder();

	private SoundsConfig() {
	}
}
