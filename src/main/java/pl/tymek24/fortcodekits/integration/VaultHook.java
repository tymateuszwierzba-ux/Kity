package pl.tymek24.fortcodekits.integration;

import org.bukkit.Bukkit;

public final class VaultHook {
  private VaultHook() {}

  public static boolean isVaultAvailable() {
    return Bukkit.getPluginManager().getPlugin("Vault") != null;
  }
}
