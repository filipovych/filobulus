package io.github.filipovych.filobulus.client.route;

import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.route.SplitBests;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

// the best run of every route, kept in config/filobulus/splits.json
public final class SplitStore {
	private final Path file;
	private final SplitBests bests = new SplitBests();

	public SplitStore(Path file) {
		this.file = file;
	}

	public SplitBests bests() {
		return bests;
	}

	public void load() {
		if (!Files.exists(file)) {
			return;
		}
		try {
			bests.readJson(JsonParser.parseString(Files.readString(file)).getAsJsonObject());
		} catch (IOException | RuntimeException e) {
			// start without bests, the next finished run writes the file again
			FilobulusClient.LOGGER.error("Could not read {}", file, e);
		}
	}

	public void save() {
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling("splits.json.tmp");
			Files.writeString(temp, bests.toJson() + "\n", StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not save {}", file, e);
		}
	}
}
