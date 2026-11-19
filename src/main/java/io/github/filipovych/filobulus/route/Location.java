package io.github.filipovych.filobulus.route;

import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.Optional;

// shaft stays empty until the sidebar has been read
public record Location(Area area, Optional<ShaftType> shaft) {
	public enum Area {
		ELSEWHERE,
		GLACITE_TUNNELS,
		MINESHAFT
	}

	public static Location elsewhere() {
		return new Location(Area.ELSEWHERE, Optional.empty());
	}

	public static Location glaciteTunnels() {
		return new Location(Area.GLACITE_TUNNELS, Optional.empty());
	}

	public static Location mineshaft() {
		return new Location(Area.MINESHAFT, Optional.empty());
	}

	public static Location mineshaft(ShaftType type) {
		return new Location(Area.MINESHAFT, Optional.of(type));
	}
}
