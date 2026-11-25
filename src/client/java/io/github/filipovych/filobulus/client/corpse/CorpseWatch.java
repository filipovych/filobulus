package io.github.filipovych.filobulus.client.corpse;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.CorpsesConfig;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.JournalConfig;
import io.github.filipovych.filobulus.corpse.CorpseLoot;
import io.github.filipovych.filobulus.corpse.CorpseLootReader;
import io.github.filipovych.filobulus.corpse.CorpseVerdict;
import io.github.filipovych.filobulus.detect.LocationParser;
import io.github.filipovych.filobulus.route.Location.Area;
import java.util.Optional;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

// reads the loot of a corpse from chat, starts the reveal for it and tells the journal
// it only listens, the chat itself is left alone
public final class CorpseWatch {
	// the key saved line can come right after the loot, so the reveal waits a moment for it
	private static final int WAIT_TICKS = 2;
	// or just before it
	private static final long KEY_SAVED_MILLIS = 3000;

	private final CorpseLootReader reader = new CorpseLootReader();
	private final BazaarPrices prices = FilobulusClient.prices();
	private CorpseLoot loot;
	private int wait;
	private long keySavedAt;
	private ClientLevel level;

	public void register() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			// overlay is the action bar
			if (overlay || !on() || FilobulusClient.tracker().location().area() != Area.MINESHAFT) {
				return;
			}
			for (String line : LocationParser.stripColor(message.getString()).split("\n")) {
				read(line);
			}
		});
	}

	public void tick(Minecraft minecraft) {
		CrackHud.tick();
		if (minecraft.level != level) {
			// a block cut off by a change of lobby is not finished by the next one
			level = minecraft.level;
			reader.reset();
			loot = null;
		}
		if (!on()) {
			loot = null;
			return;
		}
		// the prices are wanted the moment a corpse is looted, so they are fetched on the way to the shaft
		if (FilobulusClient.tracker().location().area() != Area.ELSEWHERE) {
			prices.refresh();
		}
		if (loot == null || wait-- > 0) {
			return;
		}
		boolean keySaved = System.currentTimeMillis() - keySavedAt <= KEY_SAVED_MILLIS;
		keySavedAt = 0;
		// empty for a lapis corpse, which costs no key, and while there are no prices
		Optional<CorpseVerdict> verdict = CorpseVerdict.of(loot, keySaved, prices);
		if (JournalConfig.journal) {
			// a corpse with a key we can not price is written down as looted with no coins
			double coins = verdict.map(CorpseVerdict::profit)
					.orElse(loot.type().keyId().isPresent() ? 0 : CorpseVerdict.worth(loot, prices));
			FilobulusClient.shaftLog().looted(loot.type(), Math.round(coins));
		}
		if (CorpsesConfig.reveal) {
			verdict.ifPresent(CrackHud::play);
		}
		loot = null;
	}

	private void read(String line) {
		if (CorpseLootReader.keySaved(line)) {
			keySavedAt = System.currentTimeMillis();
		}
		reader.read(line).ifPresent(found -> {
			loot = found;
			wait = WAIT_TICKS;
		});
	}

	private static boolean on() {
		// the journal wants the loot too, with the reveal switched off
		return FilobulusConfig.enabled && (CorpsesConfig.reveal || JournalConfig.journal);
	}
}
