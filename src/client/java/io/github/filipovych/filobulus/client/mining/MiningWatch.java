package io.github.filipovych.filobulus.client.mining;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.Guard;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.MiningConfig;
import io.github.filipovych.filobulus.client.detect.GameText;
import io.github.filipovych.filobulus.detect.LocationParser;
import io.github.filipovych.filobulus.mining.BlockStrength;
import io.github.filipovych.filobulus.mining.Breakpoints;
import io.github.filipovych.filobulus.mining.ColdRate;
import io.github.filipovych.filobulus.mining.MiningText;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.shaft.ShaftType;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

// what the game shows about mining as text: mining speed, pity, cold and the mining speed boost lines
// the tab list and sidebar are read once a second and only in the tunnels and shafts, the block being mined every tick
// it only reads, nothing is sent
public final class MiningWatch {
	private static final int INTERVAL = 20;
	private static final long MSB_MAX_MILLIS = 60_000;
	private static final String MSB_USED = "You used your Mining Speed Boost Pickaxe Ability!";
	private static final String MSB_GONE = "Your Mining Speed Boost has expired!";

	private final ColdRate cold = new ColdRate();
	private int ticks;
	private OptionalInt speed = OptionalInt.empty();
	private Optional<MiningText.Pity> pity = Optional.empty();
	private long msbAt;
	private ClientLevel level;
	// the block being mined and the tick it was started on
	private BlockPos mining;
	private int startedTick;
	private int clientTicks;

	public void register() {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay && FilobulusConfig.enabled) {
				Guard.run("mining chat", () -> {
					for (String line : LocationParser.stripColor(message.getString()).split("\n")) {
						chat(line.strip());
					}
				});
			}
		});
	}

	private void chat(String line) {
		if (line.equals(MSB_USED)) {
			msbAt = System.currentTimeMillis();
		} else if (line.equals(MSB_GONE)) {
			msbAt = 0;
		}
		MiningText.profileId(line).ifPresent(id -> FilobulusClient.profit().profile(id));
	}

	public void tick(Minecraft minecraft) {
		clientTicks++;
		if (minecraft.level != level) {
			level = minecraft.level;
			cold.clear();
			msbAt = 0;
			mining = null;
		}
		boolean here = FilobulusConfig.enabled && minecraft.player != null
				&& FilobulusClient.tracker().location().area() != Area.ELSEWHERE;
		if (!here) {
			mining = null;
			return;
		}
		watchBlock(minecraft);
		if (ticks++ % INTERVAL != 0) {
			return;
		}
		List<String> tab = GameText.tabLines(minecraft);
		OptionalInt read = MiningText.miningSpeed(tab);
		if (read.isPresent()) {
			speed = read;
		}
		pity = MiningText.pity(tab).or(() -> pity);
		long now = System.currentTimeMillis();
		MiningText.cold(GameText.sidebarLines(minecraft)).ifPresent(value -> cold.read(value, now));
	}

	// a new block under the crosshair while it is being mined starts the clock of the swap cue
	private void watchBlock(Minecraft minecraft) {
		boolean destroying = minecraft.gameMode != null && minecraft.gameMode.isDestroying();
		if (!destroying || !(minecraft.hitResult instanceof BlockHitResult block) || block.getType() != HitResult.Type.BLOCK) {
			mining = null;
			return;
		}
		if (!block.getBlockPos().equals(mining)) {
			mining = block.getBlockPos();
			startedTick = clientTicks;
		}
	}

	// the block under the crosshair as a skyblock block, else the gemstone of the shaft we are in
	public Optional<BlockStrength> target(Minecraft minecraft) {
		if (minecraft.hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
				&& minecraft.level != null) {
			String id = BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(block.getBlockPos()).getBlock()).getPath();
			Optional<BlockStrength> seen = BlockStrength.ofBlock(id);
			if (seen.isPresent()) {
				return seen;
			}
		}
		return FilobulusClient.tracker().location().shaft().map(ShaftType::material)
				.flatMap(material -> BlockStrength.ofGem(material.name()));
	}

	// true from the moment the server should have the block broken until the client shows it gone
	public boolean swapNow(Minecraft minecraft) {
		if (mining == null || speed.isEmpty() || minecraft.player == null || minecraft.getConnection() == null) {
			return false;
		}
		Optional<BlockStrength> block = target(minecraft);
		if (block.isEmpty()) {
			return false;
		}
		int ticks = Breakpoints.ticks(block.get().strength(MiningConfig.alphaStrengths), speedNow(), block.get().plain());
		PlayerInfo info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
		int ping = info == null ? 0 : info.getLatency();
		int elapsed = clientTicks - startedTick;
		return ticks > 1 && elapsed >= Breakpoints.swapTick(ticks, ping) && elapsed <= ticks;
	}

	// the tab list speed, with mining speed boost added while it runs
	// unverified whether the tab list already has the boost in, the owner can check
	public int speedNow() {
		int base = speed.orElse(0);
		return msbActive() ? Math.round(base * (1 + MiningConfig.msbBonus / 100f)) : base;
	}

	public OptionalInt speed() {
		return speed;
	}

	public boolean msbActive() {
		return msbAt != 0 && System.currentTimeMillis() - msbAt < MSB_MAX_MILLIS;
	}

	public Optional<MiningText.Pity> pity() {
		return pity;
	}

	public ColdRate cold() {
		return cold;
	}
}
