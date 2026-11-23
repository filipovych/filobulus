package io.github.filipovych.filobulus.client.screen;

import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.route.SharedRoute;
import io.github.filipovych.filobulus.shaft.ShaftLayout;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

// asks for the name and the author of a route that came in without them
public final class RouteNameScreen extends FlatScreen {
	private static final int WIDTH = 220;
	private static final int HEIGHT = 118;
	private static final int MAX_LENGTH = 32;

	private final Screen parent;
	private final SharedRoute route;
	private final Consumer<SharedRoute> onSave;
	// kept outside the boxes because a window resize builds them again
	private String name = "";
	private String author = "";

	public RouteNameScreen(Screen parent, SharedRoute route, Consumer<SharedRoute> onSave) {
		super(Component.literal("Name this route"));
		this.parent = parent;
		this.route = route;
		this.onSave = onSave;
	}

	@Override
	protected void init() {
		int left = (width - WIDTH) / 2 + 10;
		int top = (height - HEIGHT) / 2;
		EditBox nameBox = box(left, top + 40, "Name", name, text -> name = text);
		box(left, top + 72, "Author", author, text -> author = text);
		setInitialFocus(nameBox);
	}

	private EditBox box(int left, int top, String label, String value, Consumer<String> onChange) {
		EditBox box = new EditBox(font, left, top, WIDTH - 20, 16, Component.literal(label));
		box.setMaxLength(MAX_LENGTH);
		box.setValue(value);
		box.setResponder(onChange);
		return addRenderableWidget(box);
	}

	@Override
	protected void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int left = (width - WIDTH) / 2;
		int top = (height - HEIGHT) / 2;
		panel(graphics, left, top, WIDTH, HEIGHT);
		String shaft = ShaftLayout.byKey(route.shaft()).map(ShaftLayout::title).orElse(route.shaft());
		graphics.text(font, Component.literal("Name this route").withStyle(ChatFormatting.BOLD), left + 10, top + 8, ACCENT, true);
		graphics.text(font, route.stops().size() + " stops for " + shaft + " shafts", left + 10, top + 19, MUTED, true);
		graphics.text(font, "Name", left + 10, top + 31, FAINT, true);
		graphics.text(font, "Author, if it is not yours", left + 10, top + 63, FAINT, true);

		int y = top + HEIGHT - BUTTON_HEIGHT - 8;
		int saveWidth = buttonWidth("Save");
		int cancelWidth = buttonWidth("Cancel");
		button(graphics, left + WIDTH - 10 - saveWidth, y, saveWidth, "Save", ACCENT, mouseX, mouseY, this::save);
		button(graphics, left + WIDTH - 13 - saveWidth - cancelWidth, y, cancelWidth, "Cancel", MUTED, mouseX, mouseY,
				this::onClose);
	}

	private void save() {
		GameCompat.setScreen(minecraft, parent);
		onSave.accept(new SharedRoute(route.shaft(), name.strip(), author.strip(), route.stops()));
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
