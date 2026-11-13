package pl.tymek24.fortcodekits.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.model.Kit;
import pl.tymek24.fortcodekits.service.StatsManager;
import pl.tymek24.fortcodekits.util.TimeUtil;

public class StatsGUI extends AbstractGui {
  public StatsGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player) {
    this.open(player, 1);
  }

  public void open(Player player, int page) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("stats.size", 54);
    UUID uuid = player.getUniqueId();
    StatsManager stats = this.plugin.getStatsManager();
    Map<String, String> ph = this.buildPlaceholders(player, stats);
    String title = this.title("stats.title", "&8STATYSTYKI", ph);
    HashMap<Integer, String> slotToKit = new HashMap<Integer, String>();
    GuiHolder holder = new GuiHolder(GuiType.STATS, slotToKit, null, null, page, GuiType.PLAYER);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "stats.filler");

    ConfigurationSection overview = gui.getConfig().getConfigurationSection("stats.overview");
    if (overview != null) {
      for (String key : overview.getKeys(false)) {
        ConfigurationSection section = overview.getConfigurationSection(key);
        if (section == null) continue;
        int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= size) continue;
        ItemStack item = this.sectionItem(section, ph);
        if (item != null) inv.setItem(slot, item);
      }
    }

    List<Kit> kits = new java.util.ArrayList<Kit>(this.plugin.getKitManager().getKits());
    kits.sort(
        java.util.Comparator.comparingInt(
            (Kit kit) -> -stats.getPlayerKitClaims(uuid, kit.getId())));
    List<Integer> kitSlots = gui.getSlots("stats.kit-slots");
    int perPage = kitSlots.size();
    int pages = perPage == 0 ? 1 : (int) Math.ceil((double) kits.size() / (double) perPage);
    if (pages <= 0) pages = 1;
    int safePage = Math.max(1, Math.min(page, pages));
    int start = (safePage - 1) * perPage;
    int end = Math.min(kits.size(), start + perPage);
    ConfigurationSection kitItem = gui.getConfig().getConfigurationSection("stats.kit-item");
    int slotIndex = 0;
    for (int i = start; i < end && slotIndex < kitSlots.size(); i++) {
      int slot = kitSlots.get(slotIndex++);
      if (slot < 0 || slot >= size) continue;
      Kit kit = kits.get(i);
      Map<String, String> kitPh = new HashMap<String, String>(ph);
      kitPh.put("{kit_id}", kit.getId());
      kitPh.put("{kit_display}", kit.getDisplayName());
      kitPh.put(
          "{claims}", String.valueOf(stats.getPlayerKitClaims(uuid, kit.getId())));
      kitPh.put("{kit_claims}", String.valueOf(stats.getKitClaims(kit.getId())));
      if (kitItem != null) {
        ItemStack item = this.cfg().applyTemplate(kit.getIcon(), kitItem, kitPh);
        inv.setItem(slot, item);
        slotToKit.put(slot, kit.getId());
      }
    }

    ph.put("{page}", String.valueOf(safePage));
    ph.put("{pages}", String.valueOf(pages));
    this.navigation(inv, "stats.navigation", safePage, pages);
    this.button(inv, "stats.actions.history", ph);
    this.button(inv, "stats.actions.top", ph);
    this.button(inv, "stats.actions.back", ph);
    player.openInventory(inv);
    this.openSound(player);
  }

  private Map<String, String> buildPlaceholders(Player player, StatsManager stats) {
    Map<String, String> ph = this.ph();
    UUID uuid = player.getUniqueId();
    String mostClaimed = stats.getMostClaimedKitByPlayer(uuid);
    Kit mostClaimedKit = mostClaimed == null ? null : this.plugin.getKitManager().getKit(mostClaimed).orElse(null);
    String globalTop = stats.getMostClaimedKit();
    Kit globalTopKit = globalTop == null ? null : this.plugin.getKitManager().getKit(globalTop).orElse(null);
    List<StatsManager.HistoryEntry> recent = stats.playerHistory(uuid, 5);
    ph.put("{player}", player.getName());
    ph.put("{claims}", String.valueOf(stats.getPlayerTotalClaims(uuid)));
    ph.put("{favorites}", String.valueOf(this.plugin.getFavoriteManager().count(uuid)));
    ph.put(
        "{most_claimed}",
        mostClaimedKit == null
            ? this.text("stats.none", "&7BRAK")
            : mostClaimedKit.getDisplayName());
    ph.put(
        "{top_kit}",
        globalTopKit == null
            ? this.text("stats.none", "&7BRAK")
            : globalTopKit.getDisplayName());
    ph.put("{total_claims}", String.valueOf(stats.getTotalClaims()));
    StringBuilder recentBuilder = new StringBuilder();
    if (recent.isEmpty()) {
      recentBuilder.append(this.text("stats.none", "&7BRAK"));
    } else {
      for (int i = 0; i < recent.size(); i++) {
        StatsManager.HistoryEntry entry = recent.get(i);
        if (i > 0) recentBuilder.append('\n');
        Kit kit = this.plugin.getKitManager().getKit(entry.kit()).orElse(null);
        recentBuilder
            .append("&8» &f")
            .append(kit == null ? entry.kit() : kit.getDisplayName())
            .append(" &7(")
            .append(TimeUtil.formatTimestamp(entry.time()))
            .append(")");
      }
    }
    ph.put("{recent}", recentBuilder.toString());
    return ph;
  }
}
