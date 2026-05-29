package io.github.standardan.cratekeys.crate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads crate definitions from config, mints and recognises key items, tracks
 * which blocks are crates, and dispatches rewards.
 */
public final class CrateManager {

    private final Plugin plugin;
    private final NamespacedKey keyTag;

    private final Map<String, Crate> crates = new HashMap<>();
    private final Map<String, String> blockBindings = new HashMap<>(); // location -> crate id
    private final Set<UUID> spinning = ConcurrentHashMap.newKeySet();

    public CrateManager(Plugin plugin) {
        this.plugin = plugin;
        this.keyTag = new NamespacedKey(plugin, "crate_key");
    }

    public void load() {
        crates.clear();
        blockBindings.clear();

        ConfigurationSection cratesSec = plugin.getConfig().getConfigurationSection("crates");
        if (cratesSec != null) {
            for (String id : cratesSec.getKeys(false)) {
                ConfigurationSection c = cratesSec.getConfigurationSection(id);
                if (c == null) continue;
                Material keyMat = Material.matchMaterial(c.getString("key-material", "TRIPWIRE_HOOK"));
                List<Reward> rewards = new ArrayList<>();
                for (Map<?, ?> raw : c.getMapList("rewards")) {
                    String name = raw.get("name") != null ? String.valueOf(raw.get("name")) : "Reward";
                    int weight = raw.get("weight") instanceof Number n ? n.intValue() : 1;
                    Object iconObj = raw.get("icon");
                    Material icon = Material.matchMaterial(iconObj != null ? String.valueOf(iconObj) : "PAPER");
                    List<String> cmds = new ArrayList<>();
                    if (raw.get("commands") instanceof List<?> list) {
                        list.forEach(o -> cmds.add(String.valueOf(o)));
                    }
                    rewards.add(new Reward(name, weight, icon, cmds));
                }
                crates.put(id, new Crate(id,
                        c.getString("display-name", id),
                        c.getString("key-name", id + " Key"),
                        keyMat != null ? keyMat : Material.TRIPWIRE_HOOK,
                        rewards));
            }
        }

        ConfigurationSection blocks = plugin.getConfig().getConfigurationSection("blocks");
        if (blocks != null) {
            for (String locKey : blocks.getKeys(false)) {
                blockBindings.put(locKey, blocks.getString(locKey));
            }
        }
    }

    public Crate getCrate(String id) {
        return crates.get(id);
    }

    public java.util.Collection<Crate> crates() {
        return crates.values();
    }

    // --- keys ---------------------------------------------------------------

    public ItemStack createKey(Crate crate, int amount) {
        ItemStack key = new ItemStack(crate.keyMaterial(), amount);
        ItemMeta meta = key.getItemMeta();
        meta.displayName(crate.keyName().decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Right-click a " + crate.id() + " crate to open.",
                NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        meta.getPersistentDataContainer().set(keyTag, PersistentDataType.STRING, crate.id());
        meta.setEnchantmentGlintOverride(true);
        key.setItemMeta(meta);
        return key;
    }

    /** The crate this item is a key for, or null if it isn't a key. */
    public Crate getCrateFromKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(keyTag, PersistentDataType.STRING);
        return id == null ? null : crates.get(id);
    }

    // --- crate blocks -------------------------------------------------------

    public void bindBlock(Location loc, String crateId) {
        String key = locationKey(loc);
        blockBindings.put(key, crateId);
        plugin.getConfig().set("blocks." + key, crateId);
        plugin.saveConfig();
    }

    public boolean unbindBlock(Location loc) {
        String key = locationKey(loc);
        if (blockBindings.remove(key) != null) {
            plugin.getConfig().set("blocks." + key, null);
            plugin.saveConfig();
            return true;
        }
        return false;
    }

    public Crate getCrateAtBlock(Location loc) {
        String id = blockBindings.get(locationKey(loc));
        return id == null ? null : crates.get(id);
    }

    private String locationKey(Location loc) {
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_"
                + loc.getBlockY() + "_" + loc.getBlockZ();
    }

    // --- spins & rewards ----------------------------------------------------

    public boolean isSpinning(UUID id) {
        return spinning.contains(id);
    }

    public void setSpinning(UUID id, boolean value) {
        if (value) spinning.add(id);
        else spinning.remove(id);
    }

    /** Run a reward's commands from console, substituting the player's name. */
    public void giveReward(Player player, Reward reward) {
        for (String cmd : reward.commands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }
    }
}
