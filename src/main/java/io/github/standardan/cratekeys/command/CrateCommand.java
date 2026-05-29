package io.github.standardan.cratekeys.command;

import io.github.standardan.cratekeys.CrateKeysPlugin;
import io.github.standardan.cratekeys.crate.Crate;
import io.github.standardan.cratekeys.crate.CrateManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Admin command: give keys, bind/unbind crate blocks, reload, list.
 */
public final class CrateCommand implements CommandExecutor, TabCompleter {

    private final CrateKeysPlugin plugin;
    private final CrateManager manager;

    public CrateCommand(CrateKeysPlugin plugin, CrateManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/crate <givekey|setblock|removeblock|reload|list>",
                    NamedTextColor.GRAY));
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "givekey" -> giveKey(sender, args);
            case "setblock" -> setBlock(sender, args);
            case "removeblock" -> removeBlock(sender);
            case "reload" -> {
                plugin.reloadConfig();
                manager.load();
                sender.sendMessage(Component.text("CrateKeys reloaded.", NamedTextColor.GREEN));
            }
            case "list" -> {
                String ids = manager.crates().stream().map(Crate::id).reduce((a, b) -> a + ", " + b).orElse("(none)");
                sender.sendMessage(Component.text("Crates: " + ids, NamedTextColor.AQUA));
            }
            default -> sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
        return true;
    }

    private void giveKey(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /crate givekey <player> <crate> [amount]", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not online.", NamedTextColor.RED));
            return;
        }
        Crate crate = manager.getCrate(args[2]);
        if (crate == null) {
            sender.sendMessage(Component.text("No crate named '" + args[2] + "'.", NamedTextColor.RED));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ignored) {
                sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
                return;
            }
        }
        target.getInventory().addItem(manager.createKey(crate, amount));
        sender.sendMessage(Component.text("Gave " + amount + " " + crate.id() + " key(s) to "
                + target.getName() + ".", NamedTextColor.GREEN));
    }

    private void setBlock(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Run this in-game, looking at a block.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /crate setblock <crate>", NamedTextColor.RED));
            return;
        }
        Crate crate = manager.getCrate(args[1]);
        if (crate == null) {
            player.sendMessage(Component.text("No crate named '" + args[1] + "'.", NamedTextColor.RED));
            return;
        }
        Block block = player.getTargetBlockExact(5);
        if (block == null) {
            player.sendMessage(Component.text("Look at a block within 5 blocks.", NamedTextColor.RED));
            return;
        }
        manager.bindBlock(block.getLocation(), crate.id());
        player.sendMessage(Component.text("Bound that block as a " + crate.id() + " crate.", NamedTextColor.GREEN));
    }

    private void removeBlock(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Run this in-game, looking at a crate block.");
            return;
        }
        Block block = player.getTargetBlockExact(5);
        if (block == null || !manager.unbindBlock(block.getLocation())) {
            player.sendMessage(Component.text("That block isn't a crate.", NamedTextColor.RED));
            return;
        }
        player.sendMessage(Component.text("Removed that crate block.", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("givekey", "setblock", "removeblock", "reload", "list"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setblock")) {
            return filter(crateIds(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("givekey")) {
            return filter(crateIds(), args[2]);
        }
        return List.of();
    }

    private List<String> crateIds() {
        List<String> ids = new ArrayList<>();
        manager.crates().forEach(c -> ids.add(c.id()));
        return ids;
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream().filter(s -> s.startsWith(prefix.toLowerCase(Locale.ROOT))).toList();
    }
}
