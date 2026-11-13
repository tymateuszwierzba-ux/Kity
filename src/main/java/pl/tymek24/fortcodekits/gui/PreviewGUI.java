package pl.tymek24.fortcodekits.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.model.Kit;

public class PreviewGUI {
  private final FortCodeKits plugin;

  public PreviewGUI(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  public void open(Player player, Kit kit, GuiType returnType) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    int size = gui.getSize("preview.size", 54);
    String rawTitle = gui.getConfig().getString("preview.title", "&8Podglad: {kit_display}");
    String title = this.plugin.formatText(rawTitle.replace("{kit_display}", kit.getDisplayName()));
    GuiHolder holder =
        new GuiHolder(
            GuiType.PREVIEW, new HashMap<Integer, String>(), null, kit.getId(), 0, returnType);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, (int) size, (String) title);
    holder.setInventory(inv);
    this.applyFiller(inv, gui, "preview.filler");
    this.placeItems(inv, gui, kit);
    this.placeBack(inv, gui, "preview.back");
    player.openInventory(inv);
    this.plugin.playConfiguredSound(player, "sounds.open");
  }

  private void applyFiller(Inventory inv, GuiConfig gui, String path) {
    if (!gui.getConfig().getBoolean(path + ".enabled", false)) {
      return;
    }
    ItemStack filler = gui.buildItem(path + ".item", Map.of());
    if (filler == null) {
      return;
    }
    for (int slot : gui.getSlots(path + ".slots")) {
      if (slot < 0 || slot >= inv.getSize()) continue;
      inv.setItem(slot, filler);
    }
  }

  private void placeItems(Inventory inv, GuiConfig gui, Kit kit) {
    List<Integer> itemSlots = gui.getSlots("preview.item-slots");
    ArrayList<ItemStack> items = new ArrayList<ItemStack>(kit.getItems());
    int slotIndex = 0;
    if (!itemSlots.isEmpty()) {
      for (int i = 0; i < items.size() && slotIndex < itemSlots.size(); ++i) {
        int slot;
        if ((slot = itemSlots.get(slotIndex++).intValue()) < 0 || slot >= inv.getSize()) continue;
        inv.setItem(slot, (ItemStack) items.get(i));
      }
      return;
    }
    for (ItemStack item : items) {
      while (slotIndex < inv.getSize() && inv.getItem(slotIndex) != null) {
        ++slotIndex;
      }
      if (slotIndex >= inv.getSize()) break;
      inv.setItem(slotIndex++, item);
    }
  }

  private void placeBack(Inventory inv, GuiConfig gui, String path) {
    int slot = gui.getConfig().getInt(path + ".slot", -1);
    if (slot < 0 || slot >= inv.getSize()) {
      return;
    }
    ItemStack item = gui.buildItem(path + ".item", Map.of());
    if (item != null) {
      inv.setItem(slot, item);
    }
  }
}
