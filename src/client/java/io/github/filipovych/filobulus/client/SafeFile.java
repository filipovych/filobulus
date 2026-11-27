package io.github.filipovych.filobulus.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// the files in config/filobulus are written whole into a temp file and moved over the old one, so a crash
// half way never leaves a broken file, and a file we could not read is kept next to the original with the
// time in its name so no earlier copy is lost
public final class SafeFile {
	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private SafeFile() {
	}

	public static void write(Path file, String text) throws IOException {
		Files.createDirectories(file.getParent());
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(temp, text, StandardCharsets.UTF_8);
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
	}

	// moves it away so the next save can not write over it, true when that worked
	public static boolean moveBroken(Path file) {
		try {
			Files.move(file, broken(file));
			return true;
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not keep a copy of {}", file, e);
			return false;
		}
	}

	public static void copyBroken(Path file) {
		try {
			Files.copy(file, broken(file));
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not keep a copy of {}", file, e);
		}
	}

	private static Path broken(Path file) {
		return file.resolveSibling(file.getFileName() + ".broken-" + STAMP.format(LocalDateTime.now()));
	}
}
