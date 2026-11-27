package io.github.filipovych.filobulus.client.mining;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.compat.GameCompat;
import io.github.filipovych.filobulus.client.SafeFile;
import io.github.filipovych.filobulus.client.config.FilobulusConfig;
import io.github.filipovych.filobulus.client.config.MiningConfig;
import io.github.filipovych.filobulus.mining.BlockStrength;
import io.github.filipovych.filobulus.mining.BreakTimes;
import io.github.filipovych.filobulus.mining.Breakpoints;
import io.github.filipovych.filobulus.mining.SwapCombo;
import io.github.filipovych.filobulus.mining.SwapCombo.Event;
import io.github.filipovych.filobulus.mining.SwapCombo.Rank;
import io.github.filipovych.filobulus.mining.SwapCombo.Timing;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.sound.SoundSlot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
	// ticks is when the server breaks it, end when it should show gone, key is what the measurement is filed under
	private record Mined(BlockPos pos, BlockState state, int started, int ticks, int end, String key) {
	}

	private record Left(Mined block, Timing timing, int deadline) {
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
	// said once per game start when the stats widget is missing, the combo can not time anything without it
	private boolean askedForSpeed;
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
			FilobulusClient.LOGGER.error("Could not read {}, a copy is kept next to it", file, e);
			SafeFile.copyBroken(file);
		}
	}

	private void save() {
		JsonObject root = new JsonObject();
		root.addProperty("best", combo.best());
		root.addProperty("at", System.currentTimeMillis());
		try {
			SafeFile.write(file, root + "\n");
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
		resolve(minecraft);
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
		MiningWatch watch = FilobulusClient.mining();
		int speed = watch.speedNow();
		if (block.isPresent() && speed <= 0 && !askedForSpeed) {
			askedForSpeed = true;
			GameCompat.say(minecraft, FilobulusClient.chatLine(
					"The swap combo needs your Mining Speed: switch the Stats widget on in /tab"));
		}
		if (block.isEmpty() || speed <= 0) {
			return new Mined(pos, state, ticks, 0, 0, "");
		}
		int blockTicks = watch.breakTicks(block.get(), speed);
		return new Mined(pos, state, ticks, blockTicks, Breakpoints.glideEnd(blockTicks, watch.ping(minecraft)),
				BreakTimes.key(block.get(), speed));
	}

	private void leave(Minecraft minecraft, Mined block) {
		int elapsed = ticks - block.started();
		Timing timing = SwapCombo.timing(elapsed, block.ticks(), block.end());
		if (gone(block)) {
			// it went while we were still on it, that is how long the server really took
			// no glide though, holding on a block until it goes is no swap so the combo does not count it
			learn(block, elapsed, minecraft);
		} else if (timing == Timing.EARLY || timing == Timing.GLIDE) {
			left.add(new Left(block, timing, block.end() + GRACE_TICKS + block.started()));
		}
	}

	private boolean gone(Mined block) {
		// the block itself, a glass pane also changes state when a neighbour breaks
		return level.getBlockState(block.pos()).getBlock() != block.state().getBlock();
	}

	// the tick it showed gone less the ping is about when the server broke it
	private void learn(Mined block, int elapsed, Minecraft minecraft) {
		if (!block.key().isEmpty()) {
			FilobulusClient.mining().times().record(block.key(), elapsed - Breakpoints.pingTicks(FilobulusClient.mining().ping(minecraft)));
		}
	}

	// a block left before it showed gone and then went counts, an early one that stays breaks the combo, an early one that broke anyway is let go
	private void resolve(Minecraft minecraft) {
		Iterator<Left> each = left.iterator();
		while (each.hasNext()) {
			Left left = each.next();
			if (gone(left.block())) {
				each.remove();
				learn(left.block(), ticks - left.block().started(), minecraft);
				if (left.timing() == Timing.GLIDE) {
					handle(combo.good(ticks));
				}
			} else if (ticks > left.deadline()) {
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
