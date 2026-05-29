package io.github.standardan.cratekeys.crate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * A single possible crate reward: a display name, an icon, a relative weight,
 * and the console commands that hand out the actual prize.
 */
public record Reward(String nameMini, int weight, Material iconMaterial, List<String> commands) {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public Component displayName() {
        return MM.deserialize(nameMini).decoration(TextDecoration.ITALIC, false);
    }

    /** The icon shown in the spin GUI for this reward. */
    public ItemStack icon() {
        ItemStack item = new ItemStack(iconMaterial == null ? Material.PAPER : iconMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName());
        item.setItemMeta(meta);
        return item;
    }

    /** A "won!" version of the icon, with lore and a glow. */
    public ItemStack wonIcon() {
        ItemStack item = icon();
        ItemMeta meta = item.getItemMeta();
        meta.lore(List.of(Component.text("You won this!", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)));
        meta.setEnchantmentGlintOverride(true); // Paper: glow with no real enchant
        item.setItemMeta(meta);
        return item;
    }
}
