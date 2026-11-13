package pl.tymek24.fortcodekits.command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import pl.tymek24.fortcodekits.FortCodeKits;

public class CommandManager {
  public CommandManager(FortCodeKits plugin) {
    PluginCommand admin;
    KitsCommand command = new KitsCommand(plugin);
    PluginCommand kits = plugin.getCommand("kits");
    if (kits != null) {
      kits.setExecutor((CommandExecutor) command);
      kits.setTabCompleter((TabCompleter) command);
    }
    if ((admin = plugin.getCommand("adminkit")) != null) {
      admin.setExecutor((CommandExecutor) command);
      admin.setTabCompleter((TabCompleter) command);
    }
  }
}
