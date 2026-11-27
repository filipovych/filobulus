package io.github.filipovych.filobulus.client.sound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
		this(file, volume, 1f);
	}

	public FileSoundInstance(Path file, float volume, float pitch) {
		super(FabricSoundInstance.EMPTY_SOUND, SoundSource.UI, RandomSource.create());
		this.file = file;
		this.volume = volume;
		this.pitch = pitch;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, Identifier id, boolean repeatInstantly) {
		InputStream in;
		try {
			in = new BufferedInputStream(Files.newInputStream(file));
		} catch (IOException e) {
			return CompletableFuture.failedFuture(e);
		}
		try {
			return CompletableFuture.completedFuture(new JOrbisAudioStream(in));
		} catch (IOException | RuntimeException e) {
			// not an ogg after all, the file must not stay open or windows keeps it locked
			try {
				in.close();
			} catch (IOException ignored) {
				// the first error is the one worth telling
			}
			return CompletableFuture.failedFuture(e);
		}
	}
}
