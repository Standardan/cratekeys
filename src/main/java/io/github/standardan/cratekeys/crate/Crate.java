package io.github.standardan.cratekeys.crate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A crate type: its display/key text, the item used as its key, and its
 * weighted list of possible rewards.
 */
public final class Crate {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final String id;
    private final String displayNameMini;
    private final String keyNameMini;
    private final Material keyMaterial;
    private final List<Reward> rewards;
    private final int totalWeight;

    public Crate(String id, String displayNameMini, String keyNameMini,
                 Material keyMaterial, List<Reward> rewards) {
        this.id = id;
        this.displayNameMini = displayNameMini;
        this.keyNameMini = keyNameMini;
        this.keyMaterial = keyMaterial;
        this.rewards = rewards;
        this.totalWeight = rewards.stream().mapToInt(Reward::weight).sum();
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return MM.deserialize(displayNameMini);
    }

    public Component keyName() {
        return MM.deserialize(keyNameMini);
    }

    public Material keyMaterial() {
        return keyMaterial;
    }

    public List<Reward> rewards() {
        return rewards;
    }

    /** Pick a reward using the configured weights. */
    public Reward roll() {
        if (rewards.isEmpty()) {
            return null;
        }
        int target = ThreadLocalRandom.current().nextInt(Math.max(1, totalWeight));
        int cursor = 0;
        for (Reward reward : rewards) {
            cursor += reward.weight();
            if (target < cursor) {
                return reward;
            }
        }
        return rewards.get(rewards.size() - 1);
    }

    /** A random icon, for the scrolling animation filler. */
    public Reward randomReward() {
        return rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));
    }
}
