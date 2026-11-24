package io.github.filipovych.filobulus.corpse;

import java.util.Optional;

public enum CorpseType {
	// lapis opens without a key, vanguard takes the skeleton key
	LAPIS("Lapis", null),
	TUNGSTEN("Tungsten", "TUNGSTEN_KEY"),
	UMBER("Umber", "UMBER_KEY"),
	VANGUARD("Vanguard", "SKELETON_KEY");

	private final String displayName;
	private final String keyId;

	CorpseType(String displayName, String keyId) {
		this.displayName = displayName;
		this.keyId = keyId;
	}

	public Optional<String> keyId() {
		return Optional.ofNullable(keyId);
	}

	public String displayName() {
		return displayName;
	}

	// TUNGSTEN, the way the chat header writes it
	public static Optional<CorpseType> byName(String name) {
		for (CorpseType type : values()) {
			if (type.name().equals(name)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}
}
