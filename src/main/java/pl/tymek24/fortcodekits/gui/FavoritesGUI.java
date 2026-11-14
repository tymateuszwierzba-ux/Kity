package pl.tymek24.fortcodekits.gui;

import java.util.ArrayList;
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

public class FavoritesGUI extends AbstractGui {
  public FavoritesGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player) {
    this.open(player, 1);
  }

  public void open(Player player, int page) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("favorites.size", 54);
    UUID uuid = player.getUniqueId();
    List<String> favoriteIds = this.plugin.getFavoriteManager().get(uuid);
    List<Kit> kits = new ArrayList<Kit>();
    for (String id : favoriteIds) {
      this.plugin.getKitManager().getKit(id).ifPresent(kits::add);
    }
    Map<String, String> ph = new HashMap<String, String>();
    ph.put("{player}", player.getName());
    ph.put("{count}", String.valueOf(kits.size()));
    String title = this.title("favorites.title", "&8ULUBIONE", ph);

    HashMap<Integer, String> slotToKit = new HashMap<Integer, String>();
    GuiHolder holder =
        new GuiHolder(GuiType.FAVORITES, slotToKit, null, null, page, GuiType.PLAYER);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "favorites.filler");

    List<Integer> kitSlots = gui.getSlots("favorites.kit-slots");
    int perPage = kitSlots.size();
    int pages = perPage == 0 ? 1 : (int) Math.ceil((double) kits.size() / (double) perPage);
    if (pages <= 0) pages = 1;
    int safePage = Math.max(1, Math.min(page, pages));
    int start = (safePage - 1) * perPage;
    int end = Math.min(kits.size(), start + perPage);
    ConfigurationSection kitItem = gui.getConfig().getConfigurationSection("favorites.kit-item");
    int slotIndex = 0;
    for (int i = start; i < end && slotIndex < kitSlots.size(); i++) {
      int slot = kitSlots.get(slotIndex++);
      if (slot < 0 || slot >= size) continue;
      Kit kit = kits.get(i);
      Map<String, String> kitPh = this.plugin.getGuiManager().getPlayerGUI().kitPlaceholders(player, kit, gui);
      if (kitItem != null) {
        ItemStack item = gui.applyTemplate(kit.getIcon(), kitItem, kitPh);
        inv.setItem(slot, item);
        slotToKit.put(slot, kit.getId());
      }
    }

    ph.put("{page}", String.valueOf(safePage));
    ph.put("{pages}", String.valueOf(pages));
    this.navigation(inv, "favorites.navigation", safePage, pages);
    this.button(inv, "favorites.actions.remove-hint", ph);
    this.button(inv, "favorites.actions.back", ph);
    player.openInventory(inv);
    this.openSound(player);
  }
}
