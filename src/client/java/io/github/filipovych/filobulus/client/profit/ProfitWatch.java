package io.github.filipovych.filobulus.client.profit;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.Guard;
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
import io.github.filipovych.filobulus.profit.TradeLines;
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

// counts what the player gains while mining: it reads the sack lines in chat and watches what the inventory holds
// it only reads, nothing is clicked, moved or sent
public final class ProfitWatch {
	private static final Set<String> MINING_ISLANDS = Set.of(
			"Dwarven Mines", "Crystal Hollows", "Mineshaft", "Gold Mine", "Deep Caverns");
	private static final List<String> TRADES = List.of("[Bazaar]", "[Auction]");
	// the inventory is compared a few times a second, that is plenty to see an item arrive
	private static final int INVENTORY_INTERVAL = 4;
	private static final int SHAFT_INTERVAL = 20;
	private static final int SLOW_INTERVAL = 100;
	// right after a change of world the inventory is filled in again, that is no loot
	private static final long SETTLE_MILLIS = 3000;
	private static final long SAVE_MILLIS = 60_000;
	// while warping the tab list has no island for a few seconds, the tracker stays up that long
	private static final long WARP_MILLIS = 20_000;
	// mining and moving keep the clock running, the ledger is told at most this often
	private static final long ACTIVE_MILLIS = 1000;
	// the last sack message adds up the half minute before it, it still counts a little after leaving
	private static final long LATE_SACKS_MILLIS = 30_000;

	private final ProfitStore base;
	private ProfitStore store;
	// the skyblock profile the all time total belongs to, empty until the join line said
	private String profile = "";
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
	// when the tracker last knew it was somewhere it counts
	private long hereAt;
	private boolean active;
	// a menu was open at some point since the last look at the inventory
	private boolean menuSeen;
	private int ticks;
	private int savedVersion;
	private long savedAt;
	private long activeAt;
	// where the player stood and looked at the last tick, a change is activity
	private double x;
	private double y;
	private double z;
	private float yaw;
	private float pitch;
	// the shaft we are in and what the session held when we got in
	private long shaftAt;
	private Map<String, Long> atShaftStart = Map.of();

	public ProfitWatch(ProfitStore store, BazaarPrices prices) {
		this.base = store;
		this.store = store;
		this.prices = prices;
		allTime = store.load(names);
		savedVersion = allTime.version();
		ledger = new ProfitLedger(List.of(session, allTime));
	}

	public void register() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			// overlay is the action bar
			if (!overlay && (active || late())) {
				Guard.run("profit chat", () -> chat(message));
			}
		});
	}

	private boolean late() {
		return FilobulusConfig.enabled && TrackerConfig.profit && System.currentTimeMillis() - hereAt <= LATE_SACKS_MILLIS;
	}

	public ProfitTotals totals() {
		return TrackerConfig.period == TrackerConfig.Period.SESSION ? session : allTime;
	}

	// true while the tracker counts, the panel shows then
	public boolean active() {
		return active;
	}

	// the join line said which skyblock profile we are on, each keeps its own all time total
	// the first profile seen takes over what was counted before profiles were known
	public void profile(String id) {
		if (id.equals(profile)) {
			return;
		}
		ProfitStore next = base.forProfile(id);
		if (!profile.isEmpty() || next.exists()) {
			save();
			allTime.replaceWith(next.load(names));
			savedVersion = allTime.version();
		}
		profile = id;
		store = next;
		save();
	}

	// /filo profit reset, forgets what the period on show has counted
	public void reset() {
		totals().clear();
		atShaftStart = session.items();
		save();
	}

	public String name(String id) {
		String seen = names.get(id);
		return seen != null ? seen : MiningItems.label(id);
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
		return ProfitValue.of(totals().items(), this::price).plus(totals().corpseCoins());
	}

	// a corpse was opened, coins is its loot with the key taken off, the items are not counted again when they arrive
	public void corpse(double coins, List<Map.Entry<String, Long>> items) {
		if (active) {
			ledger.corpse(coins, items, System.currentTimeMillis());
		}
	}

	public long activeMillis() {
		return totals().activeMillis(System.currentTimeMillis(), pause());
	}

	// true while the clock stands still because nothing happened for a while
	public boolean paused() {
		return totals().paused(System.currentTimeMillis(), pause());
	}

	// the dev client only, puts a gain in as if a sack message said so, to look at the panel
	public void pretend(String id, long amount) {
		names.putIfAbsent(id, MiningItems.label(id));
		ledger.sack(id, amount, System.currentTimeMillis(), 30_000);
	}

	public void tick(Minecraft minecraft) {
		Player player = minecraft.player;
		long now = System.currentTimeMillis();
		if (mining()) {
			hereAt = now;
		}
		// a warp leaves the island unknown for a moment, that is no reason to hide the panel
		boolean warping = FilobulusClient.tracker().areaName().isEmpty() && now - hereAt < WARP_MILLIS;
		active = player != null && FilobulusConfig.enabled && TrackerConfig.profit && (now - hereAt == 0 || warping);
		if (!active) {
			held = null;
			return;
		}
		prices.refresh();
		// the item list is only needed for npc prices, so it is not asked for by default
		if (TrackerConfig.price == TrackerConfig.Price.NPC) {
			catalog.refresh();
		}
		ledger.pause(pause());
		ledger.tick(now);
		if (minecraft.level != level) {
			// the counts from before the warp are kept, once the new world settled they are compared again
			level = minecraft.level;
			settledAt = now + SETTLE_MILLIS;
		}
		boolean mining = minecraft.gameMode != null && minecraft.gameMode.isDestroying();
		if ((moved(player) || mining) && now - activeAt >= ACTIVE_MILLIS) {
			activeAt = now;
			ledger.active(now);
		}
		if (player.containerMenu != player.inventoryMenu) {
			menuSeen = true;
		}
		if (ticks++ % INVENTORY_INTERVAL == 0 && now >= settledAt) {
			lookAtInventory(player, now);
		}
		if (ticks % SHAFT_INTERVAL == 0) {
			shaft();
		}
		if (ticks % SLOW_INTERVAL == 0 && allTime.version() != savedVersion && now - savedAt > SAVE_MILLIS) {
			save();
		}
	}

	// the player walked or looked around since the last tick
	private boolean moved(Player player) {
		boolean moved = player.getX() != x || player.getY() != y || player.getZ() != z
				|| player.getYRot() != yaw || player.getXRot() != pitch;
		x = player.getX();
		y = player.getY();
		z = player.getZ();
		yaw = player.getYRot();
		pitch = player.getXRot();
		return moved;
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
		store.save(allTime, names, allTime.activeMillis(savedAt, pause()));
	}

	private static long pause() {
		return TrackerConfig.pause * 1000L;
	}

	// the mining islands, the tunnels and the shafts are known from the mod's own location read
	private static boolean mining() {
		return FilobulusClient.tracker().location().area() != Area.ELSEWHERE
				|| MINING_ISLANDS.contains(FilobulusClient.tracker().areaName());
	}

	private void chat(Component message) {
		String text = LocationParser.stripColor(message.getString()).strip();
		long now = System.currentTimeMillis();
		Optional<TradeLines.Trade> trade = TradeLines.read(text);
		if (trade.isPresent()) {
			trade(trade.get(), now);
			return;
		}
		// an order filled or an auction sold, nothing moved, only a sack line taking items out is no craft for a while
		if (TRADES.stream().anyMatch(text::startsWith)) {
			ledger.traded(now);
			return;
		}
		if (!SackReader.isSackLine(text)) {
			return;
		}
		Set<String> hovers = new LinkedHashSet<>();
		collectHovers(message, hovers, 0);
		for (String hover : hovers) {
			long window = Math.min(SackReader.windowMillis(hover), pause());
			for (SackReader.Change change : SackReader.read(hover)) {
				Optional<String> id = MiningItems.id(change.name());
				if (id.isPresent()) {
					names.putIfAbsent(id.get(), change.name());
					ledger.sack(id.get(), change.amount(), now, window);
				}
			}
		}
	}

	private void trade(TradeLines.Trade trade, long now) {
		Optional<String> id = MiningItems.id(trade.name());
		if (id.isEmpty()) {
			ledger.traded(now);
		} else if (trade.bought()) {
			ledger.bought(id.get(), trade.amount(), now);
		} else {
			ledger.sold(id.get(), trade.amount(), now);
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
		// also when it was closed again between two looks
		boolean counted = !menuSeen && player.containerMenu == player.inventoryMenu;
		menuSeen = false;
		Set<String> ids = new HashSet<>(before.keySet());
		ids.addAll(counts.keySet());
		Map<String, Long> deltas = new HashMap<>();
		for (String id : ids) {
			long delta = counts.getOrDefault(id, 0L) - before.getOrDefault(id, 0L);
			if (delta != 0) {
				deltas.put(id, delta);
			}
		}
		if (!deltas.isEmpty()) {
			ledger.inventory(deltas, now, counted);
		}
	}

	// only mining loot is counted, so other items are not even looked up
	private void count(ItemStack stack, Map<String, Long> counts) {
		if (stack.isEmpty()) {
			return;
		}
		// skyblock items carry their id in the custom data
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		String id = data == null ? "" : data.copyTag().getStringOr("id", "");
		if (id.isEmpty() || !MiningItems.isMining(id)) {
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
