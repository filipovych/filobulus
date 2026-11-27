package io.github.filipovych.filobulus.client.alert;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.AlertsConfig;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.journal.ShaftJournal;
import io.github.filipovych.filobulus.journal.ShaftRecap;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.route.Location;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import io.github.filipovych.filobulus.shaft.ShaftType;
import io.github.filipovych.filobulus.sound.SoundSlot;
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
		if (jasper && AlertsConfig.jasperMoment && AlertsConfig.alerts) {
			// the moment says jasper itself and has the sound, the plain alert would only cover it
			JasperHud.play(type.code(), dryBefore());
			return;
		}
		Alerts.show("shaft", type.displayName().toUpperCase(Locale.ROOT), type.code(), ShaftColors.of(type.material()),
				jasper ? 1 : 0, jasper ? Alerts.LONG : Alerts.SHORT, jasper ? SoundSlot.JASPER_SHAFT : null);
	}

	// the shafts without jasper right before this one, the journal may already have this one in it or not
	private static int dryBefore() {
		ShaftVisit open = FilobulusClient.shaftLog().open();
		ShaftJournal journal = FilobulusClient.journal().journal();
		return open != null && open.isJasper() ? ShaftRecap.of(open, journal.visits()).dry() : journal.sinceJasper();
	}
}
