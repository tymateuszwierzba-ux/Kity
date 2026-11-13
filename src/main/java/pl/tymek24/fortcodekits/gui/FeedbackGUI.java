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

public class FeedbackGUI {
  private final FortCodeKits plugin;

  public FeedbackGUI(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  public void open(Player player, Kit kit) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    int size = gui.getSize("feedback.size", 27);
    String rawTitle =
        gui.getConfig().getString("feedback.title", "&bTyp wiadomosci: {kit_display}");
    String title = this.plugin.formatText(rawTitle.replace("{kit_display}", kit.getDisplayName()));
    HashMap<Integer, String> slotToType = new HashMap<Integer, String>();
    GuiHolder holder =
        new GuiHolder(GuiType.FEEDBACK, slotToType, null, kit.getId(), 0, GuiType.EDITOR);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, (int) size, (String) title);
    holder.setInventory(inv);
    this.applyFiller(inv, gui, "feedback.filler");
    this.placeOptions(inv, gui, kit, slotToType);
    this.placeBack(inv, gui, "feedback.back");
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

  private void placeOptions(
      Inventory inv, GuiConfig gui, Kit kit, Map<Integer, String> slotToType) {
    ConfigurationSection options = gui.getConfig().getConfigurationSection("feedback.options");
    if (options == null) {
      return;
    }
    String current = kit.getFeedbackType();
    String selectedText = gui.getConfig().getString("text.feedback-selected", "&a(aktualny)");
    for (String key : options.getKeys(false)) {
      int slot;
      ConfigurationSection section = options.getConfigurationSection(key);
      if (section == null || (slot = section.getInt("slot", -1)) < 0 || slot >= inv.getSize())
        continue;
      String type = section.getString("type", key);
      HashMap<String, String> placeholders = new HashMap<String, String>();
      boolean selected =
          current == null || current.isBlank()
              ? type.equalsIgnoreCase("default")
              : current.equalsIgnoreCase(type);
      placeholders.put("{type}", type);
      placeholders.put("{selected}", selected ? selectedText : "");
      ItemStack item = gui.buildItem(section.getCurrentPath() + ".item", placeholders);
      if (item == null) continue;
      inv.setItem(slot, item);
      slotToType.put(slot, type);
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
