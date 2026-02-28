package org.CoreBytes.opdash.client.Lootbox;

import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

public final class PrestigeBoxManager {

    // Name can be changed centrally and will show up in the screen title.
    public static String BOX_DISPLAY_NAME = "Prestige Box";

    private static final List<WeightedReward> REWARDS = new ArrayList<>();

    static {
        // 30 rewards total, all centered around Prestige XP.
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("50 Prestige XP", "Common", 0xFFB8C0FF), 55));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("60 Prestige XP", "Common", 0xFFB8C0FF), 52));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("70 Prestige XP", "Common", 0xFFB8C0FF), 50));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("80 Prestige XP", "Common", 0xFFB8C0FF), 47));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("90 Prestige XP", "Common", 0xFFB8C0FF), 45));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("100 Prestige XP", "Common", 0xFFB8C0FF), 42));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("110 Prestige XP", "Common", 0xFFB8C0FF), 40));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("120 Prestige XP", "Common", 0xFFB8C0FF), 38));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("130 Prestige XP", "Common", 0xFFB8C0FF), 36));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("140 Prestige XP", "Common", 0xFFB8C0FF), 34));

        REWARDS.add(new WeightedReward(new PrestigeBoxReward("160 Prestige XP", "Rare", 0xFF55C1FF), 26));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("180 Prestige XP", "Rare", 0xFF55C1FF), 24));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("200 Prestige XP", "Rare", 0xFF55C1FF), 22));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("220 Prestige XP", "Rare", 0xFF55C1FF), 20));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("240 Prestige XP", "Rare", 0xFF55C1FF), 18));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("260 Prestige XP", "Rare", 0xFF55C1FF), 17));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("280 Prestige XP", "Rare", 0xFF55C1FF), 16));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("300 Prestige XP", "Rare", 0xFF55C1FF), 15));

        REWARDS.add(new WeightedReward(new PrestigeBoxReward("350 Prestige XP", "Epic", 0xFFB06BFF), 11));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("400 Prestige XP", "Epic", 0xFFB06BFF), 10));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("450 Prestige XP", "Epic", 0xFFB06BFF), 9));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("500 Prestige XP", "Epic", 0xFFB06BFF), 8));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("550 Prestige XP", "Epic", 0xFFB06BFF), 7));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("600 Prestige XP", "Epic", 0xFFB06BFF), 6));

        REWARDS.add(new WeightedReward(new PrestigeBoxReward("750 Prestige XP", "Legendary", 0xFFFF9D2E), 4));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("900 Prestige XP", "Legendary", 0xFFFF9D2E), 3));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("1050 Prestige XP", "Legendary", 0xFFFF9D2E), 2));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("1200 Prestige XP", "Legendary", 0xFFFF9D2E), 1));

        REWARDS.add(new WeightedReward(new PrestigeBoxReward("1500 Prestige XP", "Mythic", 0xFFFF5F7A), 1));
        REWARDS.add(new WeightedReward(new PrestigeBoxReward("2000 Prestige XP", "Mythic", 0xFFFF5F7A), 1));
    }

    private PrestigeBoxManager() {
    }

    public static PrestigeBoxReward rollReward(Random random) {
        int totalWeight = 0;
        for (WeightedReward reward : REWARDS) {
            totalWeight += reward.weight;
        }

        if (totalWeight <= 0) {
            return new PrestigeBoxReward("Fallback Reward", "Common", 0xFFFFFFFF);
        }

        int roll = random.nextInt(totalWeight);
        int running = 0;
        for (WeightedReward reward : REWARDS) {
            running += reward.weight;
            if (roll < running) {
                return reward.reward;
            }
        }

        return REWARDS.getLast().reward;
    }

    private static final class WeightedReward {
        private final PrestigeBoxReward reward;
        private final int weight;

        private WeightedReward(PrestigeBoxReward reward, int weight) {
            this.reward = reward;
            this.weight = Math.max(0, weight);
        }
    }
}
