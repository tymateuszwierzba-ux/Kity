package pl.tymek24.fortcodekits.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.model.Kit;
import pl.tymek24.fortcodekits.service.StatsManager;

public class TopGUI extends AbstractGui {
  public TopGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("top.size", 54);
    StatsManager stats = this.plugin.getStatsManager();
    List<Map.Entry<String, Integer>> top = stats.topKits(27);
    Map<String, String> ph = new HashMap<String, String>();
    ph.put("{total_claims}", String.valueOf(stats.getTotalClaims()));
    ph.put("{entries}", String.valueOf(top.size()));
    String title = this.title("top.title", "&8RANKING KITOW", ph);

    HashMap<Integer, String> slotToKit = new HashMap<Integer, String>();
    GuiHolder holder = new GuiHolder(GuiType.TOP, slotToKit, null, null, 0, GuiType.PLAYER);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "top.filler");

    List<Integer> entrySlots = gui.getSlots("top.entry-slots");
    ConfigurationSection entryItem = gui.getConfig().getConfigurationSection("top.entry-item");
    ConfigurationSection firstItem = gui.getConfig().getConfigurationSection("top.first-item");
    int slotIndex = 0;
    int rank = 1;
    for (Map.Entry<String, Integer> entry : top) {
      if (slotIndex >= entrySlots.size()) break;
      int slot = entrySlots.get(slotIndex++);
      if (slot < 0 || slot >= size) continue;
      Kit kit = this.plugin.getKitManager().getKit(entry.getKey()).orElse(null);
      if (kit == null) continue;
      Map<String, String> itemPh = new HashMap<String, String>();
      itemPh.put("{rank}", String.valueOf(rank));
      itemPh.put("{kit_id}", kit.getId());
      itemPh.put("{kit_display}", kit.getDisplayName());
      itemPh.put("{claims}", String.valueOf(entry.getValue()));
      ConfigurationSection section = rank == 1 && firstItem != null ? firstItem : entryItem;
      if (section != null) {
        ItemStack item = this.cfg().applyTemplate(kit.getIcon(), section, itemPh);
        inv.setItem(slot, item);
        slotToKit.put(slot, kit.getId());
      }
      rank++;
    }

    this.button(inv, "top.actions.refresh", ph);
    this.button(inv, "top.actions.back", ph);
    player.openInventory(inv);
    this.openSound(player);
  }
}
