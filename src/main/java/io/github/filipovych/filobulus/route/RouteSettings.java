package io.github.filipovych.filobulus.route;

import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.Optional;

public interface RouteSettings {
	boolean autoLoad(ShaftType type);

	String preferredRouteId(ShaftType type);

	Optional<String> tunnelsRouteId();
}
