package io.github.filipovych.filobulus.client.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

// plays an .ogg the player put in the sounds folder, not from a resource pack
// fabric's empty sound is a streamed sound with nothing in it, the stream is swapped for the file
public final class FileSoundInstance extends AbstractSoundInstance implements FabricSoundInstance {
	private final Path file;

	public FileSoundInstance(Path file, float volume) {
		super(FabricSoundInstance.EMPTY_SOUND, SoundSource.UI, RandomSource.create());
		this.file = file;
		this.volume = volume;
		this.pitch = 1f;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, Identifier id, boolean repeatInstantly) {
		try {
			return CompletableFuture.completedFuture(new JOrbisAudioStream(new BufferedInputStream(Files.newInputStream(file))));
		} catch (IOException e) {
			return CompletableFuture.failedFuture(e);
		}
	}
}
