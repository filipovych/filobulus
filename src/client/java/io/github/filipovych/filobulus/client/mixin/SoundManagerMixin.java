package io.github.filipovych.filobulus.client.mixin;

import io.github.filipovych.filobulus.client.sound.Mutes;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// a sound we were asked to mute is simply not played on our side, nothing is sent to the server
@Mixin(SoundManager.class)
abstract class SoundManagerMixin {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void filobulus$mute(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> result) {
		if (Mutes.muted(sound)) {
			result.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
		}
	}
}
