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

// on the first game start of a new month, says in chat that last month's wrapped is ready
// config/filobulus/wrapped.txt keeps the month it last looked, like 2026-11, so it only says it once
public final class WrappedNotice {
	// the server fills the chat on join, so wait a bit after the player is in
	private static final int WAIT_TICKS = 200;

	private final Path file;
	private int ticksIn;
	private boolean done;

	public WrappedNotice(Path file) {
		this.file = file;
	}

	public void tick(Minecraft minecraft) {
		if (done || !FilobulusConfig.enabled || !JournalConfig.wrapped || minecraft.player == null) {
			ticksIn = 0;
			return;
		}
		if (++ticksIn < WAIT_TICKS) {
			return;
		}
		done = true;
		YearMonth now = YearMonth.now(ZoneId.systemDefault());
		if (now.toString().equals(read())) {
			return;
		}
		YearMonth last = now.minusMonths(1);
		if (MonthlyWrapped.hasShafts(FilobulusClient.journal().journal().visits(), last, ZoneId.systemDefault())) {
			GameCompat.say(minecraft, FilobulusClient.chatLine(
					"Your " + MonthlyWrapped.monthName(last) + " Wrapped is ready: /filo wrapped"));
		}
		write(now.toString());
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
