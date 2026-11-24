package io.github.filipovych.filobulus.corpse;

// what the ice shows when it cracks open, worst to best
public enum CrackTier {
	DIRT,
	GLACITE,
	RUBY,
	AQUAMARINE,
	JASPER;

	public String title() {
		return name();
	}

	// roi is what the loot is worth divided by what the key cost, 1 is break even
	public static CrackTier of(double roi) {
		if (roi < 0.35) {
			return DIRT;
		}
		if (roi < 0.7) {
			return GLACITE;
		}
		if (roi < 1.0) {
			return RUBY;
		}
		if (roi < 2.5) {
			return AQUAMARINE;
		}
		return JASPER;
	}
}
