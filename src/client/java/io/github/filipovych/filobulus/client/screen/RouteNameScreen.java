package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.hud.RouteIcons;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

// asks for a name, and when it is about a route also for its author
// a route of the player's own gets an icon, the island it shows on and whether it goes round again
public final class RouteNameScreen extends FlatScreen {
	private static final int MAX_LENGTH = 32;
	private static final int CELL = 18;
	private static final int SELECTED = 0x407FD4FF;

	// icon, area and loop only mean something for a route of the player's own
	public record Answer(String name, String author, String icon, String area, boolean loop) {
	}

	// what a route of the player's own has on top, here is the island we are on or empty
	public record Own(String icon, String area, boolean loop, String here) {
	}

	private final Screen parent;
	private final String heading;
	private final String about;
	private final Own own;
	private final Consumer<Answer> onSave;
	private final Runnable onDelete;
	private final List<String> icons = RouteIcons.keys();
	// kept outside the boxes because a window resize builds them again
	private String name;
	// null when no author is asked for
	private String author;
	private String icon = RouteIcons.DEFAULT;
	private String area = "";
	private boolean loop = true;
	// the icon follows the name until the player picks one
	private boolean iconPicked;
	private boolean sureDelete;

	// author null leaves that box out, own null is a shaft route or something that is not a route, onDelete null hides the button
	public RouteNameScreen(Screen parent, String heading, String about, String name, String author, Own own,
			Consumer<Answer> onSave, Runnable onDelete) {
		super(Component.literal(heading));
		this.parent = parent;
		this.heading = heading;
		this.about = about;
		this.name = name;
		this.author = author;
		this.own = own;
		this.onSave = onSave;
		this.onDelete = onDelete;
		if (own != null) {
			icon = own.icon().isEmpty() ? RouteIcons.guess(name) : own.icon();
			iconPicked = !own.icon().isEmpty();
			area = own.area();
			loop = own.loop();
		}
	}

	private int panelWidth() {
		return own == null ? 220 : 260;
	}

	private int columns() {
		return (panelWidth() - 20) / CELL;
	}

	private int gridTop() {
		return author == null ? 76 : 108;
	}

	private int panelHeight() {
		int fields = author == null ? 64 : 96;
		int extras = own == null ? 0 : 12 + (icons.size() + columns() - 1) / columns() * CELL + 28;
		return fields + extras + BUTTON_HEIGHT + 14;
	}

	@Override
	protected void init() {
		int left = (width - panelWidth()) / 2 + 10;
		int top = (height - panelHeight()) / 2;
		EditBox nameBox = box(left, top + 40, "Name", name, text -> name = text);
		if (author != null) {
			box(left, top + 72, "Author", author, text -> author = text);
		}
		setInitialFocus(nameBox);
	}

	private EditBox box(int left, int top, String label, String value, Consumer<String> onChange) {
		EditBox box = new EditBox(font, left, top, panelWidth() - 20, 16, Component.literal(label));
		box.setMaxLength(MAX_LENGTH);
		box.setValue(value);
		box.setResponder(onChange);
		return addRenderableWidget(box);
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int panelWidth = panelWidth();
		int panelHeight = panelHeight();
		int left = (width - panelWidth) / 2;
		int top = (height - panelHeight) / 2;
		panel(graphics, left, top, panelWidth, panelHeight);
		graphics.text(font, Component.literal(heading).withStyle(ChatFormatting.BOLD), left + 10, top + 8, ACCENT, true);
		graphics.text(font, clip(about, panelWidth - 20), left + 10, top + 19, MUTED, true);
		graphics.text(font, "Name", left + 10, top + 31, FAINT, true);
		if (author != null) {
			graphics.text(font, "Author, if it is not yours", left + 10, top + 63, FAINT, true);
		}
		if (own != null) {
			drawOwn(graphics, left + 10, top + gridTop(), mouseX, mouseY);
		}

		int y = top + panelHeight - BUTTON_HEIGHT - 8;
		int saveWidth = buttonWidth("Save");
		int cancelWidth = buttonWidth("Cancel");
		button(graphics, left + panelWidth - 10 - saveWidth, y, saveWidth, "Save", ACCENT, mouseX, mouseY, this::save);
		button(graphics, left + panelWidth - 13 - saveWidth - cancelWidth, y, cancelWidth, "Cancel", MUTED, mouseX, mouseY,
				this::onClose);
		if (onDelete != null) {
			String label = sureDelete ? "Sure?" : "Delete";
			button(graphics, left + 10, y, buttonWidth(label), label, BAD, mouseX, mouseY, this::delete);
		}
	}

	private void drawOwn(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY) {
		if (!iconPicked) {
			icon = RouteIcons.guess(name);
		}
		graphics.text(font, "Icon", left, top - 10, FAINT, true);
		int columns = columns();
		for (int i = 0; i < icons.size(); i++) {
			String key = icons.get(i);
			int x = left + i % columns * CELL;
			int y = top + i / columns * CELL;
			boolean hovered = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
			if (key.equals(icon)) {
				graphics.fill(x, y, x + CELL, y + CELL, SELECTED);
				graphics.outline(x, y, CELL, CELL, ACCENT);
			} else if (hovered) {
				graphics.fill(x, y, x + CELL, y + CELL, HOVER);
			}
			graphics.item(RouteIcons.of(key), x + 1, y + 1);
			clickable(x, y, CELL, CELL, () -> {
				icon = key;
				iconPicked = true;
			}, null);
		}
		int y = top + (icons.size() + columns - 1) / columns * CELL + 8;
		graphics.text(font, "Shows in", left, y + 3, FAINT, true);
		String where = area.isEmpty() ? "Everywhere" : area;
		int whereLeft = left + font.width("Shows in") + 6;
		if (button(graphics, whereLeft, y, buttonWidth(where), where, TEXT, mouseX, mouseY, this::nextArea)) {
			tooltip(graphics, "The island this route is drawn on. Click to switch between the island you are on and everywhere. "
					+ "It never shows inside a mineshaft.", mouseX, mouseY);
		}
		int loopLeft = left + panelWidth() - 20 - PILL_WIDTH;
		graphics.text(font, "Loop", loopLeft - font.width("Loop") - 6, y + 3, FAINT, true);
		pill(graphics, loopLeft, y + 1, loop, () -> loop = !loop);
	}

	// everywhere, the island the route was made for, the island we stand on
	private void nextArea() {
		List<String> areas = new ArrayList<>(List.of(""));
		for (String each : List.of(own.area(), own.here())) {
			if (!each.isEmpty() && !areas.contains(each)) {
				areas.add(each);
			}
		}
		area = areas.get((areas.indexOf(area) + 1) % areas.size());
	}

	private void save() {
		GameCompat.setScreen(minecraft, parent);
		onSave.accept(new Answer(name.strip(), author == null ? "" : author.strip(), icon, area, loop));
	}

	private void delete() {
		if (!sureDelete) {
			sureDelete = true;
			return;
		}
		GameCompat.setScreen(minecraft, parent);
		onDelete.run();
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.isConfirmation()) {
			save();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		GameCompat.setScreen(minecraft, parent);
	}
}
