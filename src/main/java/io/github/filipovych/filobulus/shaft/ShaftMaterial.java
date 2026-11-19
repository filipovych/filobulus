package io.github.filipovych.filobulus.shaft;

public enum ShaftMaterial {
	// prefix = first four letters of the type code
	TOPAZ("TOPA", "Topaz", true),
	SAPPHIRE("SAPP", "Sapphire", true),
	AMETHYST("AMET", "Amethyst", true),
	AMBER("AMBE", "Amber", true),
	JADE("JADE", "Jade", true),
	RUBY("RUBY", "Ruby", true),
	ONYX("ONYX", "Onyx", true),
	AQUAMARINE("AQUA", "Aquamarine", true),
	CITRINE("CITR", "Citrine", true),
	PERIDOT("PERI", "Peridot", true),
	JASPER("JASP", "Jasper", true),
	OPAL("OPAL", "Opal", true),
	TITANIUM("TITA", "Titanium", false),
	UMBER("UMBE", "Umber", false),
	TUNGSTEN("TUNG", "Tungsten", false),
	VANGUARD("FAIR", "Vanguard", false),
	LITTLEFOOT("LITT", "Littlefoot's Den", false);

	private final String prefix;
	private final String displayName;
	private final boolean gemstone;

	ShaftMaterial(String prefix, String displayName, boolean gemstone) {
		this.prefix = prefix;
		this.displayName = displayName;
		this.gemstone = gemstone;
	}

	public String prefix() {
		return prefix;
	}

	public String displayName() {
		return displayName;
	}

	public boolean isGemstone() {
		return gemstone;
	}
}
