package io.github.filipovych.filobulus.client.alert;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.Locale;

// says which shaft this is the moment its type is read, once per shaft
public final class ShaftAlert {
	private ShaftType told;

	public void tick() {
		Location location = FilobulusClient.tracker().location();
		if (location.area() != Area.MINESHAFT) {
			told = null;
			return;
		}
		ShaftType type = location.shaft().orElse(null);
		if (type == null || type.equals(told)) {
			return;
		}
		told = type;
		if (!AlertsConfig.shaftType) {
			return;
		}
		boolean jasper = type.material() == ShaftMaterial.JASPER;
		Alerts.show("shaft", type.displayName().toUpperCase(Locale.ROOT), type.code(), ShaftColors.of(type.material()),
				jasper ? 1 : 0, jasper ? Alerts.LONG : Alerts.SHORT, jasper ? AlertsConfig.jasperSound.event() : null);
	}
}
