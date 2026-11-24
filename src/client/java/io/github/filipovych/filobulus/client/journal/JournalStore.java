package io.github.filipovych.filobulus.client.journal;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.journal.JournalCodec;
import io.github.filipovych.filobulus.journal.ShaftJournal;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;

// the journal on disk, config/filobulus/journal.jsonl with one visit per line
// lines are only ever added while playing, so a crash can not cost a shaft that was already written
public final class JournalStore {
	private final Path file;
	private final ShaftJournal journal = new ShaftJournal();
	private final long sessionStart = System.currentTimeMillis();

	public JournalStore(Path file) {
		this.file = file;
	}

	public ShaftJournal journal() {
		return journal;
	}

	// when the game was started, a session lasts until it is closed
	public long sessionStart() {
		return sessionStart;
	}

	public void load() {
		if (!Files.exists(file)) {
			return;
		}
		try {
			List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
			int bad = 0;
			for (String line : lines) {
				Optional<ShaftVisit> visit = JournalCodec.read(line);
				visit.ifPresent(journal::put);
				if (visit.isEmpty() && !line.isBlank()) {
					bad++;
				}
			}
			if (bad > 0) {
				// the lines we could not read stay in a copy, tidying would drop them
				Files.copy(file, file.resolveSibling("journal.jsonl.broken"), StandardCopyOption.REPLACE_EXISTING);
				FilobulusClient.LOGGER.warn("Skipped {} lines of {} that could not be read", bad, file);
			}
			if (lines.size() != journal.visits().size()) {
				tidy();
			}
		} catch (IOException | RuntimeException e) {
			FilobulusClient.LOGGER.error("Could not read {}", file, e);
		}
	}

	// keeps the visit and writes it at once
	public void record(ShaftVisit visit) {
		journal.put(visit);
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, JournalCodec.write(visit) + "\n", StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not write to {}", file, e);
		}
	}

	// a shaft adds a line when we enter and another when we leave, on start those are put back together
	private void tidy() throws IOException {
		StringBuilder text = new StringBuilder();
		for (ShaftVisit visit : journal.visits()) {
			text.append(JournalCodec.write(visit)).append('\n');
		}
		Path temp = file.resolveSibling("journal.jsonl.tmp");
		Files.writeString(temp, text, StandardCharsets.UTF_8);
		Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
	}
}
