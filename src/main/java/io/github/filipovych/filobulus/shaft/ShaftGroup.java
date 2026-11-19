package io.github.filipovych.filobulus.shaft;

// the choices in the auto-load setting, _1 and _2 of a gemstone count as one
public enum ShaftGroup {
	JASPER(ShaftMaterial.JASPER, false),
	JASPER_CRYSTAL(ShaftMaterial.JASPER, true),
	OPAL(ShaftMaterial.OPAL, false),
	OPAL_CRYSTAL(ShaftMaterial.OPAL, true),
	RUBY(ShaftMaterial.RUBY, false),
	RUBY_CRYSTAL(ShaftMaterial.RUBY, true),
	PERIDOT(ShaftMaterial.PERIDOT, false),
	PERIDOT_CRYSTAL(ShaftMaterial.PERIDOT, true),
	ONYX(ShaftMaterial.ONYX, false),
	ONYX_CRYSTAL(ShaftMaterial.ONYX, true),
	AQUAMARINE(ShaftMaterial.AQUAMARINE, false),
	AQUAMARINE_CRYSTAL(ShaftMaterial.AQUAMARINE, true),
	CITRINE(ShaftMaterial.CITRINE, false),
	CITRINE_CRYSTAL(ShaftMaterial.CITRINE, true),
	TOPAZ(ShaftMaterial.TOPAZ, false),
	SAPPHIRE(ShaftMaterial.SAPPHIRE, false),
	AMETHYST(ShaftMaterial.AMETHYST, false),
	AMBER(ShaftMaterial.AMBER, false),
	JADE(ShaftMaterial.JADE, false);

	private final ShaftMaterial material;
	private final boolean crystal;

	ShaftGroup(ShaftMaterial material, boolean crystal) {
		this.material = material;
		this.crystal = crystal;
	}

	public boolean contains(ShaftType type) {
		return type.material() == material && type.isCrystal() == crystal;
	}

	@Override
	public String toString() {
		return crystal ? material.displayName() + " Crystal" : material.displayName();
	}
}
