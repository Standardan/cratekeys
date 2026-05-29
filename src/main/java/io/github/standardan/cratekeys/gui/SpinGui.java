package io.github.standardan.cratekeys.gui;

import io.github.standardan.cratekeys.crate.Crate;
import io.github.standardan.cratekeys.crate.CrateManager;
import io.github.standardan.cratekeys.crate.Reward;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * The animated crate-opening GUI. It scrolls reward icons across a track and
 * lands on the (already decided) winning reward, then hands out the prize.
 *
 * Implements InventoryHolder so the click listener can recognise "this is my
 * GUI - cancel all clicks" without guessing by title.
 */
public final class SpinGui implements InventoryHolder {

    private static final int CENTER = 13;

    private final Plugin plugin;
    private final CrateManager manager;
    private final Player player;
    private final Crate crate;
    private final Reward winner;
    private final Inventory inventory;

    public SpinGui(Plugin plugin, CrateManager manager, Player player, Crate crate) {
        this.plugin = plugin;
        this.manager = manager;
        this.player = player;
        this.crate = crate;
        this.winner = crate.roll();
        this.inventory = Bukkit.createInventory(this, 27, crate.displayName());
        decorate();
    }

    private void decorate() {
        ItemStack gray = pane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, gray);
        }
        inventory.setItem(4, pane(Material.LIME_STAINED_GLASS_PANE));   // top selector
        inventory.setItem(22, pane(Material.LIME_STAINED_GLASS_PANE));  // bottom selector
        for (int slot = 9; slot <= 17; slot++) {
            inventory.setItem(slot, crate.randomReward().icon());
        }
    }

    public void open() {
        manager.setSpinning(player.getUniqueId(), true);
        player.openInventory(inventory);

        new BukkitRunnable() {
            int stepsLeft = 40;

            @Override
            public void run() {
                if (stepsLeft <= 0) {
                    finish();
                    cancel();
                    return;
                }
                // Scroll the track left, feed a new random icon on the right.
                for (int slot = 9; slot < 17; slot++) {
                    inventory.setItem(slot, inventory.getItem(slot + 1));
                }
                inventory.setItem(17, crate.randomReward().icon());
                player.playSound(player.getLocation(), "minecraft:ui.button.click", 0.5f, 1.2f);
                stepsLeft--;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void finish() {
        inventory.setItem(CENTER, winner.wonIcon());
        player.playSound(player.getLocation(), "minecraft:entity.player.levelup", 1f, 1f);
        player.sendMessage(Component.text("You won ", NamedTextColor.GREEN)
                .append(winner.displayName())
                .append(Component.text("!", NamedTextColor.GREEN)));
        manager.giveReward(player, winner);
        manager.setSpinning(player.getUniqueId(), false);

        // Auto-close the GUI a few seconds later if they're still looking at it.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() == this) {
                player.closeInventory();
            }
        }, 60L);
    }

    private ItemStack pane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
