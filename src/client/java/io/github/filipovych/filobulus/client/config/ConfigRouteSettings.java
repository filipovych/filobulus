package io.github.filipovych.filobulus.client.config;

import io.github.filipovych.filobulus.route.RouteSettings;
import io.github.filipovych.filobulus.shaft.ShaftGroup;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.Optional;

public final class ConfigRouteSettings implements RouteSettings {
	@Override
	public boolean autoLoad(ShaftType type) {
		if (!FilobulusConfig.enabled) {
			return false;
		}
		for (ShaftGroup group : RoutesConfig.autoLoad) {
			if (group.contains(type)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String preferredRouteId(ShaftType type) {
		boolean normalJasper = type.material() == ShaftMaterial.JASPER && !type.isCrystal();
		return normalJasper ? RoutesConfig.jasperRoute.routeId() : "";
	}

	@Override
	public Optional<String> tunnelsRouteId() {
		// no tunnels routes yet
		return Optional.empty();
	}
}
