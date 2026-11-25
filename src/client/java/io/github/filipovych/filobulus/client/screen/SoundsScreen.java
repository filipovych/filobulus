package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.sound.SoundBoard;
import io.github.filipovych.filobulus.sound.SoundChoices.Choice;
import io.github.filipovych.filobulus.sound.SoundSlot;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// /filo sounds: what every alert and every corpse tier plays and how loud
// .ogg files dropped in config/filobulus/sounds show up in every list
public final class SoundsScreen extends FlatScreen {
	private static final int MAX_WIDTH = 420;
	private static final int MARGIN = 8;
	private static final int ROW = 18;
	private static final int LIST_TOP = 34;
	private static final int FOOTER = 38;
	private static final int SOUND_COLUMN = 130;
	private static final int SOUND_WIDTH = 130;
	private static final int STEP = 10;
	private static final int ROW_HOVER = 0x12FFFFFF;

	// a line of the list: a heading or a slot
	private record Line(String heading, SoundSlot slot) {
	}

	private final Screen parent;
	private final SoundBoard board = FilobulusClient.sounds();
	private int scroll;
	private String status = "Put .ogg files in the sounds folder, they show up in every list here.";
	private int statusColor = FAINT;

	public SoundsScreen(Screen parent) {
		super(Component.literal("Filobulus sounds"));
		this.parent = parent;
		board.rescan();
	}

	private static List<Line> lines() {
		List<Line> lines = new ArrayList<>();
		String group = "";
		for (SoundSlot slot : SoundSlot.values()) {
			if (!slot.group().equals(group)) {
				group = slot.group();
				lines.add(new Line(group, null));
			}
			lines.add(new Line("", slot));
		}
		return lines;
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int panelWidth = Math.min(width - MARGIN * 2, MAX_WIDTH);
		int left = (width - panelWidth) / 2;
		int top = MARGIN;
		int bottom = height - MARGIN;
		int listTop = top + LIST_TOP;
		int listBottom = bottom - FOOTER;
		panel(graphics, left, top, panelWidth, bottom - top);
		graphics.text(font, Component.literal("Sounds").withStyle(ChatFormatting.BOLD), left + 8, top + 7, ACCENT, true);
		graphics.text(font, clip("Left click for the next sound, right click for the one before.", panelWidth - 16),
				left + 8, top + 19, MUTED, true);

		List<Line> lines = lines();
		scroll = Math.max(0, Math.min(scroll, lines.size() * ROW - (listBottom - listTop)));
		graphics.enableScissor(left + 1, listTop, left + panelWidth - 1, listBottom);
		for (int i = 0; i < lines.size(); i++) {
			int y = listTop + i * ROW - scroll;
			if (y + ROW <= listTop || y >= listBottom) {
				continue;
			}
			Line line = lines.get(i);
			if (line.slot() == null) {
				graphics.text(font, line.heading(), left + 8, y + 6, ACCENT, true);
				graphics.fill(left + 14 + font.width(line.heading()), y + 10, left + panelWidth - 8, y + 11, 0x16FFFFFF);
				continue;
			}
			boolean inList = mouseY >= listTop && mouseY < listBottom;
			drawSlot(graphics, line.slot(), left, y, panelWidth, inList ? mouseX : -1, mouseY);
		}
		graphics.disableScissor();
		scrollbar(graphics, left + panelWidth - 1, listTop, listBottom, lines.size() * ROW, scroll);
		clickable(left, top, panelWidth, LIST_TOP, null, null);
		clickable(left, listBottom, panelWidth, FOOTER, null, null);

		graphics.text(font, clip(status, panelWidth - 16), left + 8, listBottom + 5, statusColor, true);
		int y = bottom - BUTTON_HEIGHT - 6;
		int x = left + 8;
		if (button(graphics, x, y, buttonWidth("Open sounds folder"), "Open sounds folder", TEXT, mouseX, mouseY, this::openFolder)) {
			tooltip(graphics, "config/filobulus/sounds. Drop .ogg files in there, then press Reload.", mouseX, mouseY);
		}
		x += buttonWidth("Open sounds folder") + 3;
		button(graphics, x, y, buttonWidth("Reload"), "Reload", TEXT, mouseX, mouseY, this::reload);
		int doneWidth = buttonWidth("Done");
		button(graphics, left + panelWidth - 8 - doneWidth, y, doneWidth, "Done", ACCENT, mouseX, mouseY, this::onClose);
	}

	private void drawSlot(GuiGraphicsExtractor graphics, SoundSlot slot, int left, int y, int panelWidth, int mouseX, int mouseY) {
		if (mouseX >= left && mouseX < left + panelWidth && mouseY >= y && mouseY < y + ROW) {
			graphics.fill(left + 1, y, left + panelWidth - 1, y + ROW, ROW_HOVER);
		}
		Choice choice = board.choice(slot);
		graphics.text(font, slot.title(), left + 16, y + 5, TEXT, true);

		int soundLeft = left + SOUND_COLUMN;
		String title = "< " + clip(board.title(choice.key()), SOUND_WIDTH - font.width("<  >")) + " >";
		graphics.text(font, title, soundLeft, y + 5, choice.isFile() ? GOOD : MUTED, true);
		List<String> options = board.options();
		int at = Math.max(0, options.indexOf(choice.key()));
		clickable(soundLeft, y, SOUND_WIDTH, ROW,
				() -> choose(slot, new Choice(options.get((at + 1) % options.size()), choice.volume())),
				() -> choose(slot, new Choice(options.get((at + options.size() - 1) % options.size()), choice.volume())));

		// from the right edge: play, then the volume with its two buttons
		int x = left + panelWidth - 8 - buttonWidth("Play");
		button(graphics, x, y + 2, buttonWidth("Play"), "Play", ACCENT, mouseX, mouseY, () -> board.preview(slot));
		x -= 4 + buttonWidth("+");
		button(graphics, x, y + 2, buttonWidth("+"), "+", TEXT, mouseX, mouseY,
				() -> choose(slot, new Choice(choice.key(), choice.volume() + STEP)));
		String volume = choice.volume() + "%";
		x -= 34;
		graphics.centeredText(font, volume, x + 17, y + 5, choice.volume() == 0 ? BAD : TEXT);
		x -= buttonWidth("-");
		button(graphics, x, y + 2, buttonWidth("-"), "-", TEXT, mouseX, mouseY,
				() -> choose(slot, new Choice(choice.key(), choice.volume() - STEP)));
	}

	private void choose(SoundSlot slot, Choice choice) {
		board.choose(slot, choice);
		status = slot.title() + ": " + board.title(choice.key()) + ", " + choice.volume() + "%.";
		statusColor = MUTED;
	}

	private void openFolder() {
		if (board.openFolder()) {
			status = "Opened " + board.folder().toAbsolutePath() + ". Press Reload after adding files.";
			statusColor = GOOD;
		} else {
			minecraft.keyboardHandler.setClipboard(board.folder().toAbsolutePath().toString());
			status = "Could not open it, the path is on your clipboard.";
			statusColor = BAD;
		}
	}

	private void reload() {
		board.rescan();
		status = board.files().isEmpty() ? "No .ogg files in the sounds folder yet."
				: board.files().size() + " sound files found.";
		statusColor = board.files().isEmpty() ? MUTED : GOOD;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= (int) Math.signum(scrollY) * ROW * 2;
		return true;
	}

	@Override
	public void onClose() {
		GameCompat.setScreen(minecraft, parent);
	}
}
