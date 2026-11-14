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
    if (params == null) {
      return "";
    }
    String key = params.toLowerCase(java.util.Locale.ROOT);
    if (player == null) {
      return this.globalPlaceholder(key);
    }
    if (key.startsWith("cooldown_")) {
      String kit = key.substring("cooldown_".length());
      return TimeUtil.formatDuration(
          this.plugin.getCooldownManager().getRemaining(player.getUniqueId(), kit));
    }
    if (key.startsWith("available_")) {
      String kit = key.substring("available_".length());
      Kit model = this.plugin.getKitManager().getKit(kit).orElse(null);
      if (model == null) {
        return "false";
      }
      if (!model.isEnabled()) {
        return "false";
      }
      if (model.isOneTime()
          && this.plugin.getCooldownManager().hasClaimed(player.getUniqueId(), kit)) {
        return "false";
      }
      boolean available =
          this.plugin.getCooldownManager().getRemaining(player.getUniqueId(), kit) <= 0L;
      return available ? "true" : "false";
    }
    if (key.startsWith("price_")) {
      String kit = key.substring("price_".length());
      return this.plugin
          .getKitManager()
          .getKit(kit)
          .map(k -> this.plugin.formatPrice(k.getPrice()))
          .orElse("-");
    }
    if (key.startsWith("rarity_")) {
      String kit = key.substring("rarity_".length());
      return this.plugin
          .getKitManager()
          .getKit(kit)
          .map(k -> k.getRarity() == null ? "none" : k.getRarity().key())
          .orElse("none");
    }
    if (key.startsWith("category_")) {
      String kit = key.substring("category_".length());
      return this.plugin
          .getKitManager()
          .getKit(kit)
          .map(k -> k.getCategory() == null ? "" : k.getCategory())
          .orElse("");
    }
    if (key.startsWith("claims_")) {
      String kit = key.substring("claims_".length());
      return String.valueOf(this.plugin.getStatsManager().getPlayerKitClaims(player.getUniqueId(), kit));
    }
    if (key.startsWith("favorite_")) {
      String kit = key.substring("favorite_".length());
      return this.plugin.getFavoriteManager().isFavorite(player.getUniqueId(), kit) ? "true" : "false";
    }
    if (key.startsWith("enabled_")) {
      String kit = key.substring("enabled_".length());
      return this.plugin
          .getKitManager()
          .getKit(kit)
          .map(k -> k.isEnabled() ? "true" : "false")
          .orElse("false");
    }
    return switch (key) {
      case "favorites" -> String.valueOf(this.plugin.getFavoriteManager().count(player.getUniqueId()));
      case "total_claims" ->
          String.valueOf(this.plugin.getStatsManager().getPlayerTotalClaims(player.getUniqueId()));
      case "balance" ->
          String.valueOf(
              EconomyHook.isReady()
                  ? EconomyHook.format(EconomyHook.getBalance(player.getPlayer()))
                  : "-");
      case "top_kit" -> {
        String top = this.plugin.getStatsManager().getMostClaimedKit();
        yield top == null ? "" : top;
      }
      case "top_kit_claims" ->
          String.valueOf(this.plugin.getStatsManager().getKitClaims(this.plugin.getStatsManager().getMostClaimedKit()));
      case "total_server_claims" -> String.valueOf(this.plugin.getStatsManager().getTotalClaims());
      case "kits_count" -> String.valueOf(this.plugin.getKitManager().getKits().size());
      default -> this.globalPlaceholder(key);
    };
  }

  private String globalPlaceholder(String key) {
    return switch (key) {
      case "top_kit" -> {
        String top = this.plugin.getStatsManager() == null ? null : this.plugin.getStatsManager().getMostClaimedKit();
        yield top == null ? "" : top;
      }
      case "total_server_claims" ->
          this.plugin.getStatsManager() == null ? "0" : String.valueOf(this.plugin.getStatsManager().getTotalClaims());
      case "kits_count" ->
          this.plugin.getKitManager() == null ? "0" : String.valueOf(this.plugin.getKitManager().getKits().size());
      default -> "";
    };
  }
}
