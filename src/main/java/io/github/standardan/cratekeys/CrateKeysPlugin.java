package io.github.standardan.cratekeys;

import io.github.standardan.cratekeys.command.CrateCommand;
import io.github.standardan.cratekeys.crate.CrateManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class CrateKeysPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        CrateManager manager = new CrateManager(this);
        manager.load();

        getServer().getPluginManager().registerEvents(new CrateListener(this, manager), this);

        PluginCommand command = Objects.requireNonNull(getCommand("crate"), "crate missing from plugin.yml");
        CrateCommand handler = new CrateCommand(this, manager);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        getLogger().info("CrateKeys enabled with " + manager.crates().size() + " crate(s).");
    }
}
