package io.github.filipovych.filobulus.client.profit;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.TrackerConfig;
import io.github.filipovych.filobulus.client.corpse.BazaarPrices;
import io.github.filipovych.filobulus.detect.LocationParser;
import io.github.filipovych.filobulus.journal.ShaftVisit;
import io.github.filipovych.filobulus.profit.MiningItems;
import io.github.filipovych.filobulus.profit.ProfitLedger;
import io.github.filipovych.filobulus.profit.ProfitTotals;
import io.github.filipovych.filobulus.profit.ProfitValue;
import io.github.filipovych.filobulus.profit.SackReader;
import io.github.filipovych.filobulus.route.Location.Area;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

// counts what the player gains: it reads the sack lines in chat and watches what the inventory holds
// it only reads, nothing is clicked, moved or sent
public final class ProfitWatch {
	private static final Set<String> MINING_ISLANDS = Set.of(
			"Dwarven Mines", "Crystal Hollows", "Mineshaft", "Gold Mine", "Deep Caverns");
	private static final List<String> BOUGHT = List.of("[Bazaar]", "[Auction]");
	// the inventory is compared a few times a second, that is plenty to see an item arrive
	private static final int INVENTORY_INTERVAL = 4;
	private static final int SHAFT_INTERVAL = 20;
	private static final int SLOW_INTERVAL = 100;
	// right after a change of world the inventory is filled in again, that is no loot
	private static final long SETTLE_MILLIS = 3000;
	private static final long SAVE_MILLIS = 60_000;

	private final ProfitStore store;
	private final BazaarPrices prices;
	private final ItemCatalog catalog = new ItemCatalog();
	private final Map<String, String> names = new ConcurrentHashMap<>();
	private final ProfitTotals session = new ProfitTotals();
	private final ProfitTotals allTime;
	private final ProfitLedger ledger;
	// what the inventory held at the last look, null when the next look only takes stock
	private Map<String, Long> held;
	private ClientLevel level;
	private long settledAt;
	private boolean active;
	private int ticks;
	private int savedVersion;
	private long savedAt;
	// the shaft we are in and what the session held when we got in
	private long shaftAt;
	private Map<String, Long> atShaftStart = Map.of();

	public ProfitWatch(ProfitStore store, BazaarPrices prices) {
		this.store = store;
		this.prices = prices;
		allTime = store.load(names);
		savedVersion = allTime.version();
		ledger = new ProfitLedger(List.of(session, allTime));
	}

	public void register() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			// overlay is the action bar
			if (!overlay && active) {
				chat(message);
			}
		});
	}

	public ProfitTotals totals() {
		return TrackerConfig.period == TrackerConfig.Period.SESSION ? session : allTime;
	}

	// true while the tracker counts, the panel shows then
	public boolean active() {
		return active;
	}

	// /filo profit reset, forgets what the period on show has counted
	public void reset() {
		totals().clear();
		atShaftStart = session.items();
		save();
	}

	public String name(String id) {
		String seen = names.get(id);
		return seen != null ? seen : catalog.name(id).orElseGet(() -> MiningItems.label(id));
	}

	// the price of one item the way the settings ask for it
	public OptionalDouble price(String id) {
		OptionalDouble bazaar = TrackerConfig.price == TrackerConfig.Price.SELL_ORDER ? prices.buy(id) : prices.sell(id);
		OptionalDouble npc = catalog.npc(id);
		if (TrackerConfig.price == TrackerConfig.Price.NPC) {
			return npc.isPresent() ? npc : bazaar;
		}
		return bazaar.isPresent() ? bazaar : npc;
	}

	public ProfitValue value() {
		return ProfitValue.of(totals().items(), this::price);
	}

	public long activeMillis() {
		return totals().activeMillis(System.currentTimeMillis(), idle());
	}

	public void tick(Minecraft minecraft) {
		Player player = minecraft.player;
		active = player != null && FilobulusConfig.enabled && TrackerConfig.profit && here();
		if (!active) {
			held = null;
			return;
		}
		prices.refresh();
		// the item list is only needed for names we have no table for and for npc prices, so it is not asked for by default
		if (TrackerConfig.items == TrackerConfig.Items.EVERYTHING || TrackerConfig.price == TrackerConfig.Price.NPC) {
			catalog.refresh();
		}
		ledger.idle(idle());
		long now = System.currentTimeMillis();
		if (minecraft.level != level) {
			level = minecraft.level;
			settledAt = now + SETTLE_MILLIS;
			held = null;
		}
		if (ticks++ % INVENTORY_INTERVAL == 0) {
			lookAtInventory(player, now);
		}
		if (ticks % SHAFT_INTERVAL == 0) {
			shaft();
		}
		if (ticks % SLOW_INTERVAL == 0 && allTime.version() != savedVersion && now - savedAt > SAVE_MILLIS) {
			save();
		}
	}

	// the game is closing
	public void close() {
		if (allTime.version() != savedVersion) {
			save();
		}
	}

	private void save() {
		savedVersion = allTime.version();
		savedAt = System.currentTimeMillis();
		store.save(allTime, names, allTime.activeMillis(savedAt, idle()));
	}

	private static long idle() {
		return TrackerConfig.idle * 1000L;
	}

	// the mining islands, or all of skyblock when everything is counted
	// the tunnels and the shafts are known from the mod's own location read
	private static boolean here() {
		String area = FilobulusClient.tracker().areaName();
		return FilobulusClient.tracker().location().area() != Area.ELSEWHERE || MINING_ISLANDS.contains(area)
				|| TrackerConfig.items == TrackerConfig.Items.EVERYTHING && !area.isEmpty();
	}

	private static boolean tracked(String id) {
		return TrackerConfig.items == TrackerConfig.Items.EVERYTHING || MiningItems.isMining(id);
	}

	private void chat(Component message) {
		String text = LocationParser.stripColor(message.getString()).strip();
		long now = System.currentTimeMillis();
		if (BOUGHT.stream().anyMatch(text::startsWith)) {
			ledger.quiet(now);
			return;
		}
		if (!SackReader.isSackLine(text)) {
			return;
		}
		Set<String> hovers = new LinkedHashSet<>();
		collectHovers(message, hovers, 0);
		for (String hover : hovers) {
			for (SackReader.Change change : SackReader.read(hover)) {
				Optional<String> id = MiningItems.id(change.name()).or(() -> catalog.id(change.name()));
				if (id.isPresent() && tracked(id.get())) {
					names.putIfAbsent(id.get(), change.name());
					ledger.sack(id.get(), change.amount(), now);
				}
			}
		}
	}

	// the list of items is not in the line itself, it is the text that shows when a part of it is hovered
	private static void collectHovers(Component part, Set<String> hovers, int depth) {
		if (part.getStyle().getHoverEvent() instanceof HoverEvent.ShowText shown) {
			hovers.add(LocationParser.stripColor(shown.value().getString()));
		}
		if (depth < 8) {
			for (Component sibling : part.getSiblings()) {
				collectHovers(sibling, hovers, depth + 1);
			}
		}
	}

	private void lookAtInventory(Player player, long now) {
		Map<String, Long> counts = new HashMap<>();
		for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
			count(stack, counts);
		}
		// an item on the cursor is still ours, so moving things around is no loss
		count(player.containerMenu.getCarried(), counts);
		Map<String, Long> before = held;
		held = counts;
		if (before == null) {
			return;
		}
		// with a chest, a shop or the bazaar open what comes and goes is not loot
		boolean counted = player.containerMenu == player.inventoryMenu && now >= settledAt;
		Set<String> ids = new HashSet<>(before.keySet());
		ids.addAll(counts.keySet());
		for (String id : ids) {
			long delta = counts.getOrDefault(id, 0L) - before.getOrDefault(id, 0L);
			if (delta != 0 && tracked(id)) {
				ledger.inventory(id, delta, now, counted);
			}
		}
	}

	private void count(ItemStack stack, Map<String, Long> counts) {
		if (stack.isEmpty()) {
			return;
		}
		// skyblock items carry their id in the custom data
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		String id = data == null ? "" : data.copyTag().getStringOr("id", "");
		if (id.isEmpty()) {
			return;
		}
		counts.merge(id, (long) stack.getCount(), Long::sum);
		if (!names.containsKey(id)) {
			names.put(id, LocationParser.stripColor(stack.getHoverName().getString()).replaceFirst("^[^A-Za-z0-9]+", "").strip());
			ProfitIcons.remember(id, stack);
		}
	}

	// tells the journal what the shaft we are in has paid so far
	private void shaft() {
		ShaftVisit open = FilobulusClient.shaftLog().open();
		if (open != null && open.enteredAt() != shaftAt) {
			shaftAt = open.enteredAt();
			atShaftStart = session.items();
		}
		// after leaving it goes on for a moment, the last sack message of a shaft comes late and the journal still takes it
		if (shaftAt == 0) {
			return;
		}
		ProfitValue mined = ProfitValue.since(session.items(), atShaftStart, this::price);
		FilobulusClient.shaftLog().mined(Math.round(mined.coins()));
	}
}
