package io.github.filipovych.filobulus.client.hud;

import io.github.filipovych.filobulus.shaft.ShaftMaterial;

// the hud title takes the colour of the shaft you are in
public final class ShaftColors {
	// argb, also what the title uses when the type is not known yet
	public static final int GLACITE = 0xFF7FD4FF;

	private ShaftColors() {
	}

	// the dark ones are lighter than in game so they can be read on the dark panel
	public static int of(ShaftMaterial material) {
		return switch (material) {
			case JASPER -> 0xFFFF55FF;
			case RUBY -> 0xFFFF5555;
			case AMBER -> 0xFFFFAA00;
			case TOPAZ -> 0xFFFFFF55;
			case JADE -> 0xFF55FF55;
			case PERIDOT -> 0xFF00AA00;
			case SAPPHIRE -> 0xFF55FFFF;
			case AQUAMARINE -> 0xFF3D6FD9;
			case AMETHYST -> 0xFFB84DE0;
			case CITRINE -> 0xFFCC4422;
			case ONYX -> 0xFF777783;
			case OPAL -> 0xFFFFFFFF;
			case TITANIUM -> 0xFFD9E2EA;
			case TUNGSTEN -> 0xFF8C9AA6;
			case UMBER -> 0xFFC8743A;
			case VANGUARD, LITTLEFOOT -> GLACITE;
		};
	}
}
