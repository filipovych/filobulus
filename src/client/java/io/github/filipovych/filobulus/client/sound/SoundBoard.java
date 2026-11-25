package io.github.filipovych.filobulus.client.sound;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.SoundsConfig;
import io.github.filipovych.filobulus.sound.SoundChoices;
import io.github.filipovych.filobulus.sound.SoundChoices.Choice;
import io.github.filipovych.filobulus.sound.SoundSlot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

// the one place sounds are played from: what each slot plays, the player's own .ogg files, sounds.json
public final class SoundBoard {
	private final Path file;
	private final Path folder;
	private SoundChoices choices = new SoundChoices();
	private List<String> files = List.of();

	public SoundBoard(Path file, Path folder) {
		this.file = file;
		this.folder = folder;
	}

	public void load() {
		try {
			Files.createDirectories(folder);
			if (Files.exists(file)) {
				choices = SoundChoices.fromJson(Files.readString(file));
			}
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not read {}", file, e);
		}
		rescan();
	}

	public void save() {
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling("sounds.json.tmp");
			Files.writeString(temp, choices.toJson() + "\n", StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not save {}", file, e);
		}
	}

	// looks in the folder again, for files added while the game runs
	public void rescan() {
		List<String> found = new ArrayList<>();
		try (Stream<Path> each = Files.list(folder)) {
			each.map(path -> path.getFileName().toString())
					.filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".ogg"))
					.sorted(String.CASE_INSENSITIVE_ORDER)
					.forEach(found::add);
		} catch (IOException e) {
			// no folder, no files
		}
		files = List.copyOf(found);
	}

	public Path folder() {
		return folder;
	}

	public List<String> files() {
		return files;
	}

	public Choice choice(SoundSlot slot) {
		return choices.get(slot);
	}

	public void choose(SoundSlot slot, Choice choice) {
		choices.set(slot, choice);
		save();
	}

	// every sound a slot can pick, in the order a click goes through them: the game's own, the player's files, none
	public List<String> options() {
		List<String> options = new ArrayList<>(BuiltInSounds.keys());
		files.forEach(name -> options.add(SoundChoices.FILE + name));
		options.add(SoundChoices.NONE);
		return options;
	}

	public String title(String key) {
		if (key.equals(SoundChoices.NONE)) {
			return "None";
		}
		if (key.startsWith(SoundChoices.FILE)) {
			String name = key.substring(SoundChoices.FILE.length());
			return name.substring(0, name.length() - ".ogg".length());
		}
		BuiltInSounds.BuiltIn builtIn = BuiltInSounds.get(key);
		return builtIn == null ? "?" : builtIn.title();
	}

	// plays what the slot is set to, unless sounds are off
	public void play(SoundSlot slot) {
		if (SoundsConfig.sounds) {
			preview(slot);
		}
	}

	// plays it whatever the switch says, for the play button
	public void preview(SoundSlot slot) {
		Choice choice = choices.get(slot);
		float volume = choice.volume() / (float) SoundChoices.FULL;
		if (choice.key().equals(SoundChoices.NONE) || volume <= 0) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (choice.isFile()) {
			Path sound = folder.resolve(choice.fileName());
			if (Files.isRegularFile(sound)) {
				minecraft.getSoundManager().play(new FileSoundInstance(sound, volume));
				return;
			}
		}
		// a file that is gone plays the slot's own sound instead of nothing
		BuiltInSounds.BuiltIn builtIn = BuiltInSounds.get(choice.isFile() ? slot.fallback() : choice.key());
		if (builtIn != null) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(builtIn.event().get(), builtIn.pitch(), volume));
		}
	}

	// the file manager of the system on the sounds folder, the game has no call for that on every version
	public boolean openFolder() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String path = folder.toAbsolutePath().toString();
		List<String> command = os.contains("win") ? List.of("explorer.exe", path)
				: os.contains("mac") ? List.of("open", path) : List.of("xdg-open", path);
		try {
			Files.createDirectories(folder);
			new ProcessBuilder(command).start();
			return true;
		} catch (IOException e) {
			FilobulusClient.LOGGER.warn("Could not open {}: {}", path, e.toString());
			return false;
		}
	}
}
