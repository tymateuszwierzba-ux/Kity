package pl.tymek24.fortcodekits.gui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.tymek24.fortcodekits.FortCodeKits;

public class GuiConfig {
  private final FortCodeKits plugin;
  private final File file;
  private YamlConfiguration config;

  public GuiConfig(FortCodeKits plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "gui.yml");
    this.reload();
  }

  public void reload() {
    this.config = YamlConfiguration.loadConfiguration((File) this.file);
    InputStream resource = this.plugin.getResource("gui.yml");
    if (resource == null) {
      return;
    }
    YamlConfiguration defaults =
        YamlConfiguration.loadConfiguration(
            (Reader) new InputStreamReader(resource, StandardCharsets.UTF_8));
    this.config.setDefaults((Configuration) defaults);
    this.config.options().copyDefaults(true);
    this.normalizeLegacy();
    try {
      this.config.save(this.file);
    } catch (IOException e) {
      this.plugin.getLogger().warning("Nie udalo sie zapisac gui.yml: " + e.getMessage());
    }
  }

  private void normalizeLegacy() {
    ArrayList<Integer> fillerSlots;
    List<String> lore = this.config.getStringList("player.kit-item.lore");
    if (!lore.isEmpty()) {
      ArrayList<String> filtered = new ArrayList<String>();
      for (String line : lore) {
        if (line.contains("{permission}") || line.contains("{uses}")) continue;
        filtered.add(line);
      }
      if (!filtered.equals(lore)) {
        this.config.set("player.kit-item.lore", filtered);
      }
    }
    if (this.config.contains("editor.actions.set-permission")) {
      this.config.set("editor.actions.set-permission", null);
    }
    if (!(fillerSlots = new ArrayList<Integer>(this.config.getIntegerList("editor.filler.slots")))
        .contains(29)) {
      fillerSlots.add(29);
      this.config.set("editor.filler.slots", fillerSlots);
    }
  }

  public YamlConfiguration getConfig() {
    return this.config;
  }

  public int getSize(String path, int def) {
    int size = this.config.getInt(path, def);
    if (size < 9) {
      size = 9;
    }
    if (size > 54) {
      size = 54;
    }
    if (size % 9 != 0) {
      size = def;
    }
    return size;
  }

  public List<Integer> getSlots(String path) {
    ArrayList<Integer> slots = new ArrayList<Integer>(this.config.getIntegerList(path));
    if (!slots.isEmpty()) {
      return slots;
    }
    List<String> raw = this.config.getStringList(path);
    for (String entry : raw) {
      try {
        slots.add(Integer.parseInt(entry));
      } catch (NumberFormatException numberFormatException) {
      }
    }
    return slots;
  }

  public ItemStack buildItem(String path, Map<String, String> placeholders) {
    ConfigurationSection section = this.config.getConfigurationSection(path);
    if (section == null) {
      return null;
    }
    return this.buildItem(section, placeholders);
  }

  public ItemStack buildItem(ConfigurationSection section, Map<String, String> placeholders) {
    String materialName = section.getString("material", "STONE");
    Material material = Material.matchMaterial((String) materialName);
    if (material == null) {
      material = Material.BARRIER;
    }
    int amount = Math.max(1, Math.min(64, section.getInt("amount", 1)));
    ItemStack item = new ItemStack(material, amount);
    this.applyMeta(item, section, placeholders);
    return item;
  }

  public ItemStack applyTemplate(
      ItemStack base, ConfigurationSection section, Map<String, String> placeholders) {
    ItemStack item = base.clone();
    this.applyMeta(item, section, placeholders);
    return item;
  }

  private void applyMeta(
      ItemStack item, ConfigurationSection section, Map<String, String> placeholders) {
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      int cmd;
      if (section.contains("name")) {
        meta.setDisplayName(
            this.plugin.formatText(
                this.applyPlaceholders(section.getString("name", ""), placeholders)));
      }
      if (section.contains("lore")) {
        ArrayList<String> lore = new ArrayList<String>();
        for (String line : section.getStringList("lore")) {
          lore.add(this.plugin.formatText(this.applyPlaceholders(line, placeholders)));
        }
        meta.setLore(lore);
      }
      if ((cmd = section.getInt("custom-model-data", section.getInt("model-data", 0))) > 0) {
        meta.setCustomModelData(Integer.valueOf(cmd));
      }
      if (section.getBoolean("unbreakable", false)) {
        meta.setUnbreakable(true);
      }
      if (section.getBoolean("hide-flags", true)) {
        meta.addItemFlags(ItemFlag.values());
      }
      item.setItemMeta(meta);
    }
    if (section.getBoolean("glow", false)) {
      Enchantment glow =
          Enchantment.getByKey((NamespacedKey) NamespacedKey.minecraft((String) "unbreaking"));
      if (glow == null) {
        glow = Enchantment.getByKey((NamespacedKey) NamespacedKey.minecraft((String) "luck"));
      }
      if (glow != null) {
        item.addUnsafeEnchantment(glow, 1);
        ItemMeta glowMeta = item.getItemMeta();
        if (glowMeta != null) {
          glowMeta.addItemFlags(new ItemFlag[] {ItemFlag.HIDE_ENCHANTS});
          item.setItemMeta(glowMeta);
        }
      }
    }
  }

  private String applyPlaceholders(String text, Map<String, String> placeholders) {
    String result = text;
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      result = result.replace(entry.getKey(), entry.getValue());
    }
    return result;
  }
}
