package io.github.filipovych.filobulus.corpse;

import java.util.List;

// what one corpse gave, read from the loot block in chat
public record CorpseLoot(CorpseType type, List<Reward> rewards) {
	// name is the reward as chat names it, without the icon in front and the amount behind
	public record Reward(String name, long amount) {
	}

	public CorpseLoot {
		rewards = List.copyOf(rewards);
	}
}
