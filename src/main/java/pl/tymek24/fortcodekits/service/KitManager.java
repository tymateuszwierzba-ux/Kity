package pl.tymek24.fortcodekits.service;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.model.Kit;

public class KitManager {
  private final FortCodeKits plugin;
  private final Map<String, Kit> kits = new LinkedHashMap<String, Kit>();
  private File file;
  private YamlConfiguration config;

  public KitManager(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  public void load() {
    this.file = new File(this.plugin.getDataFolder(), "kits.yml");
    try {
      this.config = YamlConfiguration.loadConfiguration(this.file);
    } catch (RuntimeException ex) {
      this.config = null;
      this.kits.clear();
      this.plugin.getLogger().severe("Nie udalo sie odczytac kits.yml: " + ex.getMessage());
      return;
    }
    this.kits.clear();
    ConfigurationSection section = this.config.getConfigurationSection("kits");
    if (section == null) {
      return;
    }
    for (String key : section.getKeys(false)) {
      ConfigurationSection child = section.getConfigurationSection(key);
      if (child == null) continue;
      this.kits.put(key.toLowerCase(), Kit.fromConfig(key, child));
    }
  }

  public void save() {
    if (this.config == null) {
      this.plugin.getLogger().severe("kits.yml nie zostal odczytany, zapis pominieto");
      return;
    }
    this.config.set("kits", null);
    ConfigurationSection root = this.config.createSection("kits");
    for (Kit kit : this.kits.values()) {
      ConfigurationSection sec = root.createSection(kit.getId());
      kit.saveTo(sec);
    }
    try {
      this.config.save(this.file);
    } catch (IOException e) {
      this.plugin
          .getLogger()
          .severe("Nie uda\u0142o si\u0119 zapisa\u0107 kits.yml: " + e.getMessage());
    }
  }

  public Collection<Kit> getKits() {
    return this.kits.values();
  }

  public Optional<Kit> getKit(String id) {
    return Optional.ofNullable(this.kits.get(id.toLowerCase()));
  }

  public void upsert(Kit kit) {
    this.kits.put(kit.getId(), kit);
  }

  public void remove(String id) {
    this.kits.remove(id.toLowerCase());
  }
}
