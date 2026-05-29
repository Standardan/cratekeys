package io.github.standardan.cratekeys;

import io.github.standardan.cratekeys.crate.Crate;
import io.github.standardan.cratekeys.crate.CrateManager;
import io.github.standardan.cratekeys.gui.SpinGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Opens crates when a player right-clicks a bound crate block with the right
 * key, and locks the spin GUI so nothing can be clicked out of it.
 */
public final class CrateListener implements Listener {

    private final Plugin plugin;
    private final CrateManager manager;

    public CrateListener(Plugin plugin, CrateManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Crate crate = manager.getCrateAtBlock(block.getLocation());
        if (crate == null) {
            return; // not a crate block - ignore
        }
        event.setCancelled(true); // don't open the underlying chest, etc.

        Player player = event.getPlayer();
        if (manager.isSpinning(player.getUniqueId())) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        Crate keyCrate = manager.getCrateFromKey(hand);
        if (keyCrate == null || !keyCrate.id().equals(crate.id())) {
            player.sendMessage(Component.text("You need a ", NamedTextColor.RED)
                    .append(crate.keyName())
                    .append(Component.text(" to open this.", NamedTextColor.RED)));
            return;
        }

        hand.setAmount(hand.getAmount() - 1); // consume one key
        new SpinGui(plugin, manager, player, crate).open();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SpinGui) {
            event.setCancelled(true); // can't take items mid-spin
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SpinGui) {
            event.setCancelled(true);
        }
    }
}
