package io.github.filipovych.filobulus.client.detect;

import io.github.filipovych.filobulus.client.hud.Hud;
import io.github.filipovych.filobulus.detect.LocationParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

// the text the game already shows in the tab list and the sidebar, colour codes stripped
public final class GameText {
	private GameText() {
	}

	// several watchers read the lists in the same tick, so each is read once a tick and shared
	private static long tabTick = -1;
	private static List<String> tab = List.of();
	private static long sidebarTick = -1;
	private static List<String> sidebar = List.of();

	public static List<String> tabLines(Minecraft minecraft) {
		if (Hud.ticks() == tabTick) {
			return tab;
		}
		tabTick = Hud.ticks();
		tab = readTab(minecraft);
		return tab;
	}

	private static List<String> readTab(Minecraft minecraft) {
		ClientPacketListener connection = minecraft.getConnection();
		if (connection == null) {
			return List.of();
		}
		List<String> lines = new ArrayList<>();
		for (PlayerInfo info : connection.getListedOnlinePlayers()) {
			// hypixel writes its tab widgets as display names, real players without one are skipped
			Component name = info.getTabListDisplayName();
			if (name != null) {
				lines.add(LocationParser.stripColor(name.getString()));
			}
		}
		return Collections.unmodifiableList(lines);
	}

	// skyblock items carry their id in the custom data, empty for anything else
	public static String skyblockId(ItemStack stack) {
		CustomData data = stack.isEmpty() ? null : stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? "" : data.copyTag().getStringOr("id", "");
	}

	public static List<String> sidebarLines(Minecraft minecraft) {
		if (Hud.ticks() == sidebarTick) {
			return sidebar;
		}
		sidebarTick = Hud.ticks();
		sidebar = readSidebar(minecraft);
		return sidebar;
	}

	private static List<String> readSidebar(Minecraft minecraft) {
		if (minecraft.level == null) {
			return List.of();
		}
		Scoreboard scoreboard = minecraft.level.getScoreboard();
		Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
		if (objective == null) {
			return List.of();
		}
		List<String> lines = new ArrayList<>();
		for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
			// the visible text of a line sits in the team prefix and suffix, the entry name is filler
			PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
			String text = team == null
					? entry.owner()
					: team.getPlayerPrefix().getString() + team.getPlayerSuffix().getString();
			lines.add(LocationParser.stripColor(text));
		}
		return Collections.unmodifiableList(lines);
	}
}
