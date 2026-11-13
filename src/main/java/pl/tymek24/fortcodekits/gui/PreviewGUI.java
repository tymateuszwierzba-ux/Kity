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

public class PreviewGUI extends AbstractGui {
  public PreviewGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player, Kit kit, GuiType returnType) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("preview.size", 54);
    String title = this.title("preview.title", "&8Podglad: {kit_display}", Map.of("{kit_display}", kit.getDisplayName()));
    GuiHolder holder = new GuiHolder(GuiType.PREVIEW, new HashMap<Integer, String>(), null, kit.getId(), 0, returnType);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "preview.filler");
    this.placeItems(inv, gui, kit);
    this.button(inv, "preview.back", Map.of());
    player.openInventory(inv);
    this.openSound(player);
  }

  private void placeItems(Inventory inv, GuiConfig gui, Kit kit) {
    List<Integer> itemSlots = gui.getSlots("preview.item-slots");
    ArrayList<ItemStack> items = new ArrayList<ItemStack>(kit.getItems());
    int slotIndex = 0;
    if (!itemSlots.isEmpty()) {
      for (ItemStack item : items) {
        if (slotIndex >= itemSlots.size()) {
          break;
        }
        int slot = itemSlots.get(slotIndex++);
        if (slot < 0 || slot >= inv.getSize()) {
          continue;
        }
        inv.setItem(slot, item);
      }
      return;
    }
    for (ItemStack item : items) {
      while (slotIndex < inv.getSize() && inv.getItem(slotIndex) != null) {
        slotIndex++;
      }
      if (slotIndex >= inv.getSize()) {
        break;
      }
      inv.setItem(slotIndex++, item);
    }
  }
}
