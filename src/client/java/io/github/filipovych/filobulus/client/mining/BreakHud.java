package io.github.filipovych.filobulus.client.mining;

import io.github.filipovych.filobulus.client.FilobulusClient;
import io.github.filipovych.filobulus.client.config.MiningConfig;
import io.github.filipovych.filobulus.client.hud.HudDrawable;
import io.github.filipovych.filobulus.client.hud.HudPanel.Content;
import io.github.filipovych.filobulus.client.hud.HudPanel.Row;
import io.github.filipovych.filobulus.client.hud.HudWidget;
import io.github.filipovych.filobulus.client.hud.ShaftColors;
import io.github.filipovych.filobulus.client.hud.ShaftIcons;
import io.github.filipovych.filobulus.hud.HudAnchor;
import io.github.filipovych.filobulus.hud.HudPlacement;
import io.github.filipovych.filobulus.mining.BlockStrength;
import io.github.filipovych.filobulus.mining.Breakpoints;
import io.github.filipovych.filobulus.route.Location.Area;
import io.github.filipovych.filobulus.shaft.ShaftMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;

// the block you look at, or the shaft's gemstone: ticks at your mining speed and what the next breakpoint needs
// one small sum per frame, the speed itself is read once a second
public final class BreakHud implements HudWidget {
	// argb
	private static final int GOOD = 0xFF7CE38B;
	private static final int FAINT = 0xFF6B7782;
	private static final int BOOST = 0xFFFFAA00;

	// the last answer, worked out again only when the block or the speed changed
	private BlockStrength lastBlock;
	private int lastSpeed = -1;
	private boolean lastAlpha;
	private Content last;

	@Override
	public String name() {
		return "Breakpoints";
	}

	@Override
	public boolean enabled() {
		return MiningConfig.breakpoints;
	}

	@Override
	public void setEnabled(boolean on) {
		MiningConfig.breakpoints = on;
	}

	@Override
	public Optional<HudDrawable> content() {
		if (!MiningConfig.breakpoints || FilobulusClient.tracker().location().area() == Area.ELSEWHERE) {
			return Optional.empty();
		}
		MiningWatch watch = FilobulusClient.mining();
		Optional<BlockStrength> block = watch.target(Minecraft.getInstance());
		if (block.isEmpty()) {
			return Optional.empty();
		}
		int speed = watch.speedNow();
		if (block.get() != lastBlock || speed != lastSpeed || MiningConfig.alphaStrengths != lastAlpha || last == null) {
			lastBlock = block.get();
			lastSpeed = speed;
			lastAlpha = MiningConfig.alphaStrengths;
			last = panel(block.get(), speed, watch.speed().isPresent(), watch.msbActive());
		}
		return Optional.of(last);
	}

	private static Content panel(BlockStrength block, int speed, boolean known, boolean boosted) {
		int strength = block.strength(MiningConfig.alphaStrengths);
		List<Row> rows = new ArrayList<>();
		if (!known) {
			// the speed comes from the stats widget of the tab list
			rows.add(new Row("Speed", "? Stats in /tab", FAINT));
		} else {
			int ticks = Breakpoints.ticks(strength, speed, block.plain());
			rows.add(new Row("Ticks", ticks + (ticks == 1 ? " (instant)" : ""), boosted ? BOOST : GOOD));
			rows.add(new Row("Speed", number(speed) + (boosted ? " MSB" : ""), boosted ? BOOST : 0xFFFFFFFF));
			Breakpoints.Next next = Breakpoints.next(strength, speed, block.plain());
			if (next.ticks() > 0) {
				// while boosted the panel speed is x3, what the player can add is base speed
				float boost = boosted ? 1 + MiningConfig.msbBonus / 100f : 1;
				int more = (int) Math.ceil(next.speed() / boost) - Math.round(speed / boost);
				rows.add(new Row(next.ticks() + " ticks", "+" + number(Math.max(1, more))));
			}
			if (!boosted) {
				int withBoost = Math.round(speed * (1 + MiningConfig.msbBonus / 100f));
				rows.add(new Row("With MSB", String.valueOf(Breakpoints.ticks(strength, withBoost, block.plain())), BOOST));
			}
		}
		ShaftMaterial material = material(block);
		return new Content(ShaftIcons.of(material), block.title(),
				material == ShaftMaterial.LITTLEFOOT ? ShaftColors.GLACITE : ShaftColors.of(material), rows);
	}

	private static ShaftMaterial material(BlockStrength block) {
		String name = block.name();
		for (ShaftMaterial material : ShaftMaterial.values()) {
			if (name.equals(material.name())) {
				return material;
			}
		}
		return ShaftMaterial.LITTLEFOOT;
	}

	private static String number(int value) {
		return String.format(Locale.ROOT, "%,d", value);
	}

	@Override
	public HudDrawable sample() {
		return panel(BlockStrength.JASPER, 4200, true, false);
	}

	@Override
	public HudPlacement placement() {
		return new HudPlacement(MiningConfig.breakHudAnchorX, MiningConfig.breakHudX,
				MiningConfig.breakHudAnchorY, MiningConfig.breakHudY, MiningConfig.breakHudScale);
	}

	@Override
	public void place(HudPlacement placement) {
		MiningConfig.breakHudAnchorX = placement.anchorX();
		MiningConfig.breakHudX = placement.x();
		MiningConfig.breakHudAnchorY = placement.anchorY();
		MiningConfig.breakHudY = placement.y();
		MiningConfig.breakHudScale = placement.scale();
	}

	@Override
	public HudPlacement defaultPlacement() {
		// right edge, under the profit tracker
		return new HudPlacement(HudAnchor.END, -4, HudAnchor.CENTRE, 30, 1f);
	}
}
