package io.github.filipovych.filobulus.shaft;

public enum ShaftVariant {
	// _1 and _2 are just two layouts of the same shaft, _C is crystal, _L is only LITT_L
	ONE('1'),
	TWO('2'),
	CRYSTAL('C'),
	DEN('L');

	private final char suffix;

	ShaftVariant(char suffix) {
		this.suffix = suffix;
	}

	public char suffix() {
		return suffix;
	}
}
