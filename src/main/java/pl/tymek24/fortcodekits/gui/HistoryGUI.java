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

public class HistoryGUI extends AbstractGui {
  public HistoryGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player) {
    this.open(player, 1);
  }

  public void open(Player player, int page) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("history.size", 54);
    boolean adminView = player.hasPermission("fortcode.kits.history.all");
    StatsManager stats = this.plugin.getStatsManager();
    List<StatsManager.HistoryEntry> entries =
        adminView
            ? stats.history(500, null)
            : stats.playerHistory(player.getUniqueId(), 500);
    Map<String, String> ph = new HashMap<String, String>();
    ph.put("{player}", player.getName());
    ph.put("{entries}", String.valueOf(entries.size()));
    ph.put("{scope}", adminView ? this.text("history.scope-all", "&cGLOBALNA") : this.text("history.scope-mine", "&aMOJA"));
    String title = this.title("history.title", "&8HISTORIA", ph);

    HashMap<Integer, String> slotToKit = new HashMap<Integer, String>();
    GuiHolder holder =
        new GuiHolder(GuiType.HISTORY, slotToKit, null, null, page, GuiType.PLAYER);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "history.filler");

    List<Integer> entrySlots = gui.getSlots("history.entry-slots");
    int perPage = entrySlots.size();
    int pages = perPage == 0 ? 1 : (int) Math.ceil((double) entries.size() / (double) perPage);
    if (pages <= 0) pages = 1;
    int safePage = Math.max(1, Math.min(page, pages));
    int start = (safePage - 1) * perPage;
    int end = Math.min(entries.size(), start + perPage);
    ConfigurationSection entryItem = gui.getConfig().getConfigurationSection("history.entry-item");
    int slotIndex = 0;
    for (int i = start; i < end && slotIndex < entrySlots.size(); i++) {
      int slot = entrySlots.get(slotIndex++);
      if (slot < 0 || slot >= size) continue;
      StatsManager.HistoryEntry entry = entries.get(i);
      Kit kit = this.plugin.getKitManager().getKit(entry.kit()).orElse(null);
      Map<String, String> itemPh = new HashMap<String, String>();
      itemPh.put("{kit_id}", entry.kit());
      itemPh.put("{kit_display}", kit == null ? entry.kit() : kit.getDisplayName());
      itemPh.put("{player}", entry.name());
      itemPh.put("{time}", TimeUtil.formatTimestamp(entry.time()));
      itemPh.put("{ago}", TimeUtil.formatAgo(entry.time()));
      itemPh.put("{date}", TimeUtil.formatDate(entry.time()));
      if (entryItem != null) {
        ItemStack item = this.sectionItem(entryItem, itemPh);
        if (item != null) inv.setItem(slot, item);
      }
      if (kit != null) slotToKit.put(slot, kit.getId());
    }

    ph.put("{page}", String.valueOf(safePage));
    ph.put("{pages}", String.valueOf(pages));
    this.navigation(inv, "history.navigation", safePage, pages);
    if (adminView) {
      this.button(inv, "history.actions.clear", ph);
    }
    this.button(inv, "history.actions.back", ph);
    player.openInventory(inv);
    this.openSound(player);
  }
}
