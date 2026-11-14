package pl.tymek24.fortcodekits.gui;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;

public class ConfirmGUI extends AbstractGui {
  public ConfirmGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player, String action, String kitId, GuiType returnType, String message) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("confirm.size", 27);
    Map<String, String> ph = new HashMap<String, String>();
    ph.put("{action}", action == null ? "?" : action.toUpperCase());
    ph.put("{kit}", kitId == null ? "-" : kitId);
    ph.put("{message}", message == null ? "" : message);
    String title = this.title("confirm.title", "&8POTWIERDZENIE", ph);
    GuiHolder holder =
        new GuiHolder(GuiType.CONFIRM, new HashMap<Integer, String>(), null, kitId, 0, returnType);
    holder.setConfirmAction(action);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "confirm.filler");
    ConfigurationSection info = gui.getConfig().getConfigurationSection("confirm.info");
    if (info != null) {
      int slot = info.getInt("slot", -1);
      if (slot >= 0 && slot < size) {
        ItemStack item = this.sectionItem(info, ph);
        if (item != null) inv.setItem(slot, item);
      }
    }
    this.button(inv, "confirm.actions.yes", ph);
    this.button(inv, "confirm.actions.no", ph);
    player.openInventory(inv);
    this.openSound(player);
  }
}
