package io.github.filipovych.filobulus.client.mining;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.MiningConfig;
import io.github.filipovych.filobulus.mining.BlockStrength;
import io.github.filipovych.filobulus.mining.Breakpoints;
import io.github.filipovych.filobulus.mining.SwapCombo;
import io.github.filipovych.filobulus.mining.SwapCombo.Event;
import io.github.filipovych.filobulus.mining.SwapCombo.Rank;
import io.github.filipovych.filobulus.mining.SwapCombo.Timing;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.sound.SoundSlot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

// feeds the swap combo: which block is mined, when it was left, and whether it was gone after
// with ping gliding the block disappears on our side after we already left it, so a left block waits a bit for that
// only reads the crosshair and the blocks around, nothing is sent
public final class ComboWatch {
	// how long a left block may take to show as gone, on top of the ping
	private static final int GRACE_TICKS = 6;

	// a block being mined, with the glide window worked out when it was started
	private record Mined(BlockPos pos, BlockState state, int started, int swapTick, int ticks) {
	}

	private record Left(BlockPos pos, BlockState state, Timing timing, int deadline) {
	}

	// what the hud shows, kept a moment after the combo ended so it can fade out
	public record Shown(Rank rank, double meter, int count, int best, boolean broken) {
	}

	private final Path file;
	private SwapCombo combo = new SwapCombo(0);
	private final List<Left> left = new ArrayList<>();
	private ClientLevel level;
	private Mined mining;
	private int ticks;
	private Shown last;
	private boolean demo;
	private long goodAt;
	private long rankUpAt;
	private long endedAt;

	public ComboWatch(Path file) {
		this.file = file;
	}

	public void load() {
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			combo = new SwapCombo(root.has("best") ? root.get("best").getAsInt() : 0);
		} catch (IOException | RuntimeException e) {
			FilobulusClient.LOGGER.error("Could not read {}", file, e);
		}
	}

	private void save() {
		JsonObject root = new JsonObject();
		root.addProperty("best", combo.best());
		root.addProperty("at", System.currentTimeMillis());
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling("combo.json.tmp");
			Files.writeString(temp, root + "\n", StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			FilobulusClient.LOGGER.error("Could not save {}", file, e);
		}
	}

	public void tick(Minecraft minecraft) {
		ticks++;
		if (minecraft.level != level) {
			level = minecraft.level;
			reset();
		}
		boolean here = MiningConfig.swapCombo && FilobulusConfig.enabled && minecraft.player != null && level != null
				&& FilobulusClient.tracker().location().area() != Area.ELSEWHERE;
		// the screenshot combo stays up outside the tunnels
		if (!here) {
			if (!demo) {
				reset();
			}
			return;
		}
		BlockPos pos = crosshair(minecraft);
		if (mining == null ? pos != null : !mining.pos().equals(pos)) {
			if (mining != null) {
				leave(minecraft, mining);
			}
			mining = pos == null ? null : start(minecraft, pos);
		}
		demo = false;
		resolve();
		handle(combo.tick(ticks));
	}

	// saves a new best that is still running when the game closes
	public void close() {
		if (combo.takeNewBest()) {
			save();
		}
	}

	private void reset() {
		if (combo.takeNewBest()) {
			save();
		}
		combo.stop();
		left.clear();
		mining = null;
		last = null;
	}

	private static BlockPos crosshair(Minecraft minecraft) {
		boolean destroying = minecraft.gameMode != null && minecraft.gameMode.isDestroying();
		if (destroying && minecraft.hitResult instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK) {
			return block.getBlockPos();
		}
		return null;
	}

	// the window is worked out from the block itself, the shaft gemstone fallback of the breakpoints panel would be wrong here
	private Mined start(Minecraft minecraft, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
		Optional<BlockStrength> block = BlockStrength.ofBlock(id);
		int speed = FilobulusClient.mining().speedNow();
		if (block.isEmpty() || speed <= 0) {
			return new Mined(pos, state, ticks, 0, 0);
		}
		int blockTicks = Breakpoints.ticks(block.get().strength(MiningConfig.alphaStrengths), speed, block.get().plain());
		return new Mined(pos, state, ticks, Breakpoints.swapTick(blockTicks, ping(minecraft)), blockTicks);
	}

	private void leave(Minecraft minecraft, Mined block) {
		Timing timing = SwapCombo.timing(ticks - block.started(), block.swapTick(), block.ticks());
		if (timing == Timing.EARLY || timing == Timing.GLIDE) {
			int wait = (int) Math.ceil(ping(minecraft) / 50d) + GRACE_TICKS;
			left.add(new Left(block.pos(), block.state(), timing, ticks + wait));
		}
	}

	// a glided block that is gone counts, an early one that stays breaks the combo, an early one that broke anyway is let go
	private void resolve() {
		Iterator<Left> each = left.iterator();
		while (each.hasNext()) {
			Left block = each.next();
			// the block itself, a glass pane also changes state when a neighbour breaks
			if (level.getBlockState(block.pos()).getBlock() != block.state().getBlock()) {
				each.remove();
				if (block.timing() == Timing.GLIDE) {
					handle(combo.good(ticks));
				}
			} else if (ticks > block.deadline()) {
				each.remove();
				handle(combo.broken());
			}
		}
	}

	private void handle(Event event) {
		long now = System.currentTimeMillis();
		switch (event) {
			case GOOD -> goodAt = now;
			case RANK_UP -> {
				goodAt = now;
				rankUpAt = now;
				if (MiningConfig.comboSounds) {
					// a step higher for every rank
					FilobulusClient.sounds().play(SoundSlot.COMBO_RANK, 0.8f + 0.15f * combo.rank().ordinal());
				}
			}
			case BROKEN -> {
				endedAt = now;
				last = new Shown(last == null ? Rank.FROST : last.rank(), 0, last == null ? 0 : last.count(), combo.best(), true);
				if (MiningConfig.comboSounds) {
					FilobulusClient.sounds().play(SoundSlot.COMBO_BROKEN);
				}
				if (combo.takeNewBest()) {
					save();
				}
				return;
			}
			case ENDED -> {
				endedAt = now;
				last = new Shown(Rank.FROST, 0, last == null ? 0 : last.count(), combo.best(), false);
				if (combo.takeNewBest()) {
					save();
				}
				return;
			}
			default -> {
			}
		}
		if (combo.active()) {
			last = new Shown(combo.rank(), combo.meter(), combo.count(), combo.best(), false);
		}
	}

	private static int ping(Minecraft minecraft) {
		if (minecraft.getConnection() == null || minecraft.player == null) {
			return 0;
		}
		PlayerInfo info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
		return info == null ? 0 : info.getLatency();
	}

	public boolean active() {
		return combo.active();
	}

	// the live state while the combo runs, the frozen last state while it fades
	public Optional<Shown> shown() {
		if (combo.active()) {
			return Optional.of(new Shown(combo.rank(), combo.meter(), combo.count(), combo.best(), false));
		}
		return Optional.ofNullable(last);
	}

	public long goodAt() {
		return goodAt;
	}

	public long rankUpAt() {
		return rankUpAt;
	}

	public long endedAt() {
		return endedAt;
	}

	// for the screenshot script, puts a combo on screen without mining
	public void demo(int swaps) {
		demo = true;
		combo.stop();
		for (int i = 0; i < swaps; i++) {
			handle(combo.good(ticks));
		}
		// a made up combo is no new best
		combo.takeNewBest();
	}
}
