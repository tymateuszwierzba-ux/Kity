package pl.tymek24.fortcodekits.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.model.Kit;
import pl.tymek24.fortcodekits.util.TimeUtil;

public class KitsPlaceholderExpansion extends PlaceholderExpansion {
  private final FortCodeKits plugin;

  public KitsPlaceholderExpansion(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  @NotNull
  public String getIdentifier() {
    return "fortcode_kits";
  }

  @NotNull
  public String getAuthor() {
    return "TYMEK24";
  }

  @NotNull
  public String getVersion() {
    return this.plugin.getDescription().getVersion();
  }

  public String onRequest(OfflinePlayer player, @NotNull String params) {
    if (player == null) {
      return "";
    }
    if (params.startsWith("cooldown_")) {
      String kit = params.substring("cooldown_".length());
      return TimeUtil.formatDuration(
          this.plugin.getCooldownManager().getRemaining(player.getUniqueId(), kit));
    }
    if (params.startsWith("available_")) {
      String kit = params.substring("available_".length());
      Kit model = this.plugin.getKitManager().getKit(kit).orElse(null);
      if (model != null
          && model.isOneTime()
          && this.plugin.getCooldownManager().hasClaimed(player.getUniqueId(), kit)) {
        return "false";
      }
      boolean available =
          this.plugin.getCooldownManager().getRemaining(player.getUniqueId(), kit) <= 0L;
      return available ? "true" : "false";
    }
    return "";
  }
}
