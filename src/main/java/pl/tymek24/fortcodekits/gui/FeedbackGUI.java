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
import pl.tymek24.fortcodekits.model.Kit;

public class FeedbackGUI extends AbstractGui {
  public FeedbackGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player, Kit kit) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("feedback.size", 27);
    String title = this.title("feedback.title", "&bTyp wiadomosci: {kit_display}", Map.of("{kit_display}", kit.getDisplayName()));
    Map<Integer, String> slotToType = new HashMap<Integer, String>();
    GuiHolder holder = new GuiHolder(GuiType.FEEDBACK, slotToType, null, kit.getId(), 0, GuiType.EDITOR);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "feedback.filler");
    this.placeOptions(inv, kit, slotToType);
    this.button(inv, "feedback.back", Map.of());
    player.openInventory(inv);
    this.openSound(player);
  }

  private void placeOptions(Inventory inv, Kit kit, Map<Integer, String> slotToType) {
    ConfigurationSection options = this.cfg().getConfig().getConfigurationSection("feedback.options");
    if (options == null) {
      return;
    }
    String current = kit.getFeedbackType();
    String selectedText = this.text("text.feedback-selected", "&a(aktualny)");
    for (String key : options.getKeys(false)) {
      ConfigurationSection section = options.getConfigurationSection(key);
      if (section == null) {
        continue;
      }
      int slot = section.getInt("slot", -1);
      if (slot < 0 || slot >= inv.getSize()) {
        continue;
      }
      String type = section.getString("type", key);
      boolean selected =
          current == null || current.isBlank()
              ? type.equalsIgnoreCase("default")
              : current.equalsIgnoreCase(type);
      Map<String, String> ph = this.ph();
      ph.put("{type}", type);
      ph.put("{selected}", selected ? selectedText : "");
      ItemStack item = this.item(section.getCurrentPath() + ".item", ph);
      if (item == null) {
        continue;
      }
      inv.setItem(slot, item);
      slotToType.put(slot, type);
    }
  }
}
