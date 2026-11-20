package io.github.filipovych.filobulus.client.detect;

import io.github.filipovych.filobulus.detect.LocationParser;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

// the text the game already shows in the tab list and the sidebar, colour codes stripped
public final class GameText {
	private GameText() {
	}

	public static List<String> tabLines(Minecraft minecraft) {
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
		return lines;
	}

	public static List<String> sidebarLines(Minecraft minecraft) {
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
		return lines;
	}
}
