package pl.tymek24.fortcodekits.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.tymek24.fortcodekits.FortCodeKits;

public class FavoriteManager {
  private final FortCodeKits plugin;
  private final File file;
  private final Map<UUID, List<String>> favorites = new ConcurrentHashMap<UUID, List<String>>();

  public FavoriteManager(FortCodeKits plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "favorites.yml");
    this.load();
  }

  public void load() {
    if (!this.file.exists()) {
      this.plugin.saveResource("favorites.yml", false);
      if (!this.file.exists()) {
        try {
          this.file.createNewFile();
        } catch (IOException ignored) {
        }
      }
    }
    YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
    InputStream resource = this.plugin.getResource("favorites.yml");
    if (resource != null) {
      YamlConfiguration defaults =
          YamlConfiguration.loadConfiguration(
              new InputStreamReader(resource, StandardCharsets.UTF_8));
      config.setDefaults((Configuration) defaults);
      config.options().copyDefaults(true);
    }
    this.favorites.clear();
    for (String uuidKey : config.getKeys(false)) {
      UUID uuid;
      try {
        uuid = UUID.fromString(uuidKey);
      } catch (IllegalArgumentException ex) {
        continue;
      }
      List<String> raw = config.getStringList(uuidKey);
      List<String> cleaned = new ArrayList<String>();
      for (String entry : raw) {
        if (entry == null) continue;
        String value = entry.trim().toLowerCase(Locale.ROOT);
        if (!value.isEmpty() && !cleaned.contains(value)) {
          cleaned.add(value);
        }
      }
      this.favorites.put(uuid, cleaned);
    }
  }

  public void save() {
    YamlConfiguration config = new YamlConfiguration();
    for (Map.Entry<UUID, List<String>> entry : this.favorites.entrySet()) {
      if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
      config.set(entry.getKey().toString(), entry.getValue());
    }
    try {
      config.save(this.file);
    } catch (IOException ex) {
      this.plugin.getLogger().warning("Nie udalo sie zapisac favorites.yml: " + ex.getMessage());
    }
  }

  public void reload() {
    this.load();
  }

  public List<String> get(UUID uuid) {
    List<String> list = this.favorites.get(uuid);
    return list == null ? List.of() : new ArrayList<String>(list);
  }

  public boolean isFavorite(UUID uuid, String kitId) {
    List<String> list = this.favorites.get(uuid);
    return list != null && list.contains(kitId.toLowerCase(Locale.ROOT));
  }

  public int count(UUID uuid) {
    List<String> list = this.favorites.get(uuid);
    return list == null ? 0 : list.size();
  }

  public boolean add(UUID uuid, String kitId) {
    String id = kitId.toLowerCase(Locale.ROOT);
    List<String> list =
        this.favorites.computeIfAbsent(uuid, key -> new ArrayList<String>());
    if (list.contains(id)) {
      return false;
    }
    int max = Math.max(1, this.plugin.getConfig().getInt("favorites.max-per-player", 54));
    if (list.size() >= max) {
      return false;
    }
    list.add(id);
    return true;
  }

  public boolean remove(UUID uuid, String kitId) {
    List<String> list = this.favorites.get(uuid);
    if (list == null) {
      return false;
    }
    boolean removed = list.remove(kitId.toLowerCase(Locale.ROOT));
    if (list.isEmpty()) {
      this.favorites.remove(uuid);
    }
    return removed;
  }

  public boolean toggle(UUID uuid, String kitId) {
    if (this.isFavorite(uuid, kitId)) {
      this.remove(uuid, kitId);
      return false;
    }
    return this.add(uuid, kitId);
  }

  public Map<UUID, List<String>> snapshot() {
    return new LinkedHashMap<UUID, List<String>>(this.favorites);
  }

  public void unload(UUID uuid) {
    this.favorites.remove(uuid);
  }
}
