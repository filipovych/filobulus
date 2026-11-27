package io.github.filipovych.filobulus.client.journal;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.JournalConfig;
import io.github.filipovych.filobulus.journal.MonthlyWrapped;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.ZoneId;
import net.minecraft.client.Minecraft;

// the first time the player is in game in a new month, says in chat that last month's wrapped is ready
// also when the month turns while the game is open, it keeps looking once a minute
// config/filobulus/wrapped.txt keeps the month it last said it for, like 2026-11, so it only says it once
public final class WrappedNotice {
	// the server fills the chat on join, so wait a bit after the player is in
	private static final int WAIT_TICKS = 200;
	private static final int CHECK_TICKS = 1200;

	private final Path file;
	private int ticksIn;
	private String seen;

	public WrappedNotice(Path file) {
		this.file = file;
	}

	public void tick(Minecraft minecraft) {
		if (!FilobulusConfig.enabled || !JournalConfig.wrapped || minecraft.player == null) {
			ticksIn = 0;
			return;
		}
		ticksIn++;
		if (ticksIn < WAIT_TICKS || (ticksIn - WAIT_TICKS) % CHECK_TICKS != 0) {
			return;
		}
		if (seen == null) {
			seen = read();
		}
		YearMonth now = YearMonth.now(ZoneId.systemDefault());
		if (now.toString().equals(seen)) {
			return;
		}
		seen = now.toString();
		YearMonth last = now.minusMonths(1);
		if (MonthlyWrapped.hasShafts(FilobulusClient.journal().journal().visits(), last, ZoneId.systemDefault())) {
			GameCompat.say(minecraft, FilobulusClient.chatLine(
					"Your " + MonthlyWrapped.monthName(last) + " Wrapped is ready: /filo wrapped"));
		}
		write(seen);
	}

	private String read() {
		try {
			return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8).strip() : "";
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not read {}", file, e);
			return "";
		}
	}

	private void write(String month) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, month + "\n", StandardCharsets.UTF_8);
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not write {}", file, e);
		}
	}
}
