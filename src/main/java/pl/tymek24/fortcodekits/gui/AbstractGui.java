package pl.tymek24.fortcodekits.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;

public abstract class AbstractGui {
  protected final FortCodeKits plugin;

  protected AbstractGui(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  protected GuiConfig cfg() {
    return this.plugin.getGuiManager().getGuiConfig();
  }

  protected String title(String path, String fallback) {
    return this.title(path, fallback, Map.of());
  }

  protected String title(String path, String fallback, Map<String, String> placeholders) {
    GuiConfig gui = this.cfg();
    String raw = gui.getConfig().getString(path, fallback);
    return this.plugin.formatText(this.apply(raw, placeholders));
  }

  protected void fill(Inventory inv, String path) {
    GuiConfig gui = this.cfg();
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

  protected ItemStack item(String path, Map<String, String> placeholders) {
    return this.cfg().buildItem(path, placeholders);
  }

  protected ItemStack sectionItem(ConfigurationSection section, Map<String, String> placeholders) {
    if (section == null) {
      return null;
    }
    return this.cfg().buildItem(section, placeholders);
  }

  protected void button(Inventory inv, String path, Map<String, String> placeholders) {
    GuiConfig gui = this.cfg();
    int slot = gui.getConfig().getInt(path + ".slot", -1);
    if (slot < 0 || slot >= inv.getSize()) {
      return;
    }
    ItemStack item = gui.buildItem(path + ".item", placeholders);
    if (item != null) {
      inv.setItem(slot, item);
    }
  }

  protected void buttonIf(Inventory inv, String path, boolean condition, Map<String, String> ph) {
    if (condition) {
      this.button(inv, path, ph);
    }
  }

  protected void navigation(Inventory inv, String path, int page, int pages) {
    Map<String, String> ph = new HashMap<String, String>();
    ph.put("{page}", String.valueOf(page));
    ph.put("{pages}", String.valueOf(pages));
    this.buttonIf(inv, path + ".prev", page > 1, ph);
    this.buttonIf(inv, path + ".next", page < pages, ph);
  }

  protected int slotOf(String path) {
    return this.cfg().getConfig().getInt(path + ".slot", -1);
  }

  protected boolean inSlot(Inventory inv, String path, int slot) {
    int expected = this.slotOf(path);
    return expected >= 0 && expected == slot && expected < inv.getSize();
  }

  protected String text(String path, String fallback) {
    return this.cfg().getConfig().getString(path, fallback);
  }

  protected List<String> lore(String path) {
    return this.cfg().getConfig().getStringList(path);
  }

  protected String apply(String raw, Map<String, String> placeholders) {
    String result = raw == null ? "" : raw;
    if (placeholders != null) {
      for (Map.Entry<String, String> entry : placeholders.entrySet()) {
        if (entry.getKey() == null || entry.getValue() == null) continue;
        result = result.replace(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  protected void openSound(Player player) {
    this.plugin.playConfiguredSound(player, "sounds.open");
  }

  protected Map<String, String> ph() {
    return new HashMap<String, String>();
  }
}
