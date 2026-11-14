package pl.tymek24.fortcodekits.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.tymek24.fortcodekits.FortCodeKits;

public class StatsManager {
  private final FortCodeKits plugin;
  private final File file;
  private final Map<UUID, Map<String, Integer>> playerClaims =
      new ConcurrentHashMap<UUID, Map<String, Integer>>();
  private final Map<String, Integer> kitClaims = new ConcurrentHashMap<String, Integer>();
  private final List<HistoryEntry> history =
      new java.util.concurrent.CopyOnWriteArrayList<HistoryEntry>();
  private volatile boolean dirty;

  public StatsManager(FortCodeKits plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "stats.yml");
    this.load();
  }

  public void load() {
    if (!this.file.exists()) {
      try {
        this.file.createNewFile();
      } catch (IOException ignored) {
      }
    }
    YamlConfiguration config = YamlConfiguration.loadConfiguration(this.file);
    InputStream resource = this.plugin.getResource("stats.yml");
    if (resource != null) {
      YamlConfiguration defaults =
          YamlConfiguration.loadConfiguration(
              new InputStreamReader(resource, StandardCharsets.UTF_8));
      config.setDefaults((Configuration) defaults);
      config.options().copyDefaults(true);
    }
    this.playerClaims.clear();
    this.kitClaims.clear();
    this.history.clear();
    this.dirty = false;
    ConfigurationSection players = config.getConfigurationSection("players");
    if (players != null) {
      for (String uuidKey : players.getKeys(false)) {
        UUID uuid;
        try {
          uuid = UUID.fromString(uuidKey);
        } catch (IllegalArgumentException ex) {
          continue;
        }
        ConfigurationSection kits = players.getConfigurationSection(uuidKey + ".kits");
        Map<String, Integer> inner = new ConcurrentHashMap<String, Integer>();
        if (kits != null) {
          for (String kitId : kits.getKeys(false)) {
            inner.put(kitId.toLowerCase(Locale.ROOT), kits.getInt(kitId, 0));
          }
        }
        this.playerClaims.put(uuid, inner);
      }
    }
    ConfigurationSection kitsRoot = config.getConfigurationSection("kits");
    if (kitsRoot != null) {
      for (String kitId : kitsRoot.getKeys(false)) {
        this.kitClaims.put(kitId.toLowerCase(Locale.ROOT), kitsRoot.getInt(kitId, 0));
      }
    }
    int maxHistory = Math.max(10, this.plugin.getConfig().getInt("history.max-entries", 200));
    List<Map<?, ?>> rawHistory = config.getMapList("history");
    for (Map<?, ?> raw : rawHistory) {
      Object playerObj = raw.get("player");
      Object kitObj = raw.get("kit");
      Object timeObj = raw.get("time");
      Object nameObj = raw.get("name");
      if (playerObj == null || kitObj == null || timeObj == null) continue;
      HistoryEntry entry =
          new HistoryEntry(
              String.valueOf(playerObj),
              String.valueOf(kitObj).toLowerCase(Locale.ROOT),
              nameObj == null ? "?" : String.valueOf(nameObj),
              Long.parseLong(String.valueOf(timeObj)));
      this.history.add(entry);
    }
    this.history.sort(Comparator.comparingLong(HistoryEntry::time).reversed());
    while (this.history.size() > maxHistory) {
      this.history.remove(this.history.size() - 1);
    }
  }

  public void save() {
    YamlConfiguration config = new YamlConfiguration();
    for (Map.Entry<UUID, Map<String, Integer>> playerEntry : this.playerClaims.entrySet()) {
      ConfigurationSection section =
          config.createSection("players." + playerEntry.getKey().toString());
      for (Map.Entry<String, Integer> kitEntry : playerEntry.getValue().entrySet()) {
        section.set("kits." + kitEntry.getKey(), kitEntry.getValue());
      }
    }
    for (Map.Entry<String, Integer> kitEntry : this.kitClaims.entrySet()) {
      config.set("kits." + kitEntry.getKey(), kitEntry.getValue());
    }
    List<Map<String, Object>> rawHistory = new ArrayList<Map<String, Object>>();
    for (HistoryEntry entry : this.history) {
      Map<String, Object> row = new LinkedHashMap<String, Object>();
      row.put("player", entry.player());
      row.put("name", entry.name());
      row.put("kit", entry.kit());
      row.put("time", entry.time());
      rawHistory.add(row);
    }
    config.set("history", rawHistory);
    try {
      config.save(this.file);
      this.dirty = false;
    } catch (IOException ex) {
      this.plugin.getLogger().warning("Nie udalo sie zapisac stats.yml: " + ex.getMessage());
    }
  }

  public void recordClaim(UUID uuid, String playerName, String kitId) {
    String id = kitId.toLowerCase(Locale.ROOT);
    this.playerClaims
        .computeIfAbsent(uuid, key -> new ConcurrentHashMap<String, Integer>())
        .merge(id, 1, Integer::sum);
    this.kitClaims.merge(id, 1, Integer::sum);
    int maxHistory = Math.max(10, this.plugin.getConfig().getInt("history.max-entries", 200));
    this.history.add(0, new HistoryEntry(uuid.toString(), id, playerName, System.currentTimeMillis()));
    while (this.history.size() > maxHistory) {
      this.history.remove(this.history.size() - 1);
    }
    this.dirty = true;
  }

  public int getPlayerKitClaims(UUID uuid, String kitId) {
    if (uuid == null || kitId == null) {
      return 0;
    }
    Map<String, Integer> inner = this.playerClaims.get(uuid);
    if (inner == null) {
      return 0;
    }
    Integer value = inner.get(kitId.toLowerCase(Locale.ROOT));
    return value == null ? 0 : value;
  }

  public int getPlayerTotalClaims(UUID uuid) {
    Map<String, Integer> inner = this.playerClaims.get(uuid);
    if (inner == null) {
      return 0;
    }
    int total = 0;
    for (Integer value : inner.values()) {
      total += value;
    }
    return total;
  }

  public int getKitClaims(String kitId) {
    if (kitId == null) {
      return 0;
    }
    Integer value = this.kitClaims.get(kitId.toLowerCase(Locale.ROOT));
    return value == null ? 0 : value;
  }

  public int getTotalClaims() {
    int total = 0;
    for (Integer value : this.kitClaims.values()) {
      total += value;
    }
    return total;
  }

  public String getMostClaimedKit() {
    String best = null;
    int bestCount = -1;
    for (Map.Entry<String, Integer> entry : this.kitClaims.entrySet()) {
      if (entry.getValue() > bestCount) {
        bestCount = entry.getValue();
        best = entry.getKey();
      }
    }
    return best;
  }

  public String getMostClaimedKitByPlayer(UUID uuid) {
    Map<String, Integer> inner = this.playerClaims.get(uuid);
    if (inner == null || inner.isEmpty()) {
      return null;
    }
    String best = null;
    int bestCount = -1;
    for (Map.Entry<String, Integer> entry : inner.entrySet()) {
      if (entry.getValue() > bestCount) {
        bestCount = entry.getValue();
        best = entry.getKey();
      }
    }
    return best;
  }

  public List<Map.Entry<String, Integer>> topKits(int limit) {
    List<Map.Entry<String, Integer>> list =
        new ArrayList<Map.Entry<String, Integer>>(this.kitClaims.entrySet());
    list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
    if (list.size() > limit) {
      return new ArrayList<Map.Entry<String, Integer>>(list.subList(0, limit));
    }
    return list;
  }

  public List<Map.Entry<String, Integer>> topPlayers(int limit) {
    Map<String, Integer> totals = new LinkedHashMap<String, Integer>();
    for (Map.Entry<UUID, Map<String, Integer>> entry : this.playerClaims.entrySet()) {
      int sum = 0;
      for (Integer value : entry.getValue().values()) {
        sum += value;
      }
      totals.put(entry.getKey().toString(), sum);
    }
    List<Map.Entry<String, Integer>> list =
        new ArrayList<Map.Entry<String, Integer>>(totals.entrySet());
    list.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
    if (list.size() > limit) {
      return new ArrayList<Map.Entry<String, Integer>>(list.subList(0, limit));
    }
    return list;
  }

  public List<HistoryEntry> history(int limit, String kitFilter) {
    List<HistoryEntry> result = new ArrayList<HistoryEntry>();
    String filter = kitFilter == null ? null : kitFilter.toLowerCase(Locale.ROOT);
    for (HistoryEntry entry : this.history) {
      if (filter != null && !filter.isEmpty() && !entry.kit().equals(filter)) continue;
      result.add(entry);
      if (result.size() >= limit) break;
    }
    return result;
  }

  public List<HistoryEntry> playerHistory(UUID uuid, int limit) {
    List<HistoryEntry> result = new ArrayList<HistoryEntry>();
    String id = uuid.toString();
    for (HistoryEntry entry : this.history) {
      if (!entry.player().equals(id)) continue;
      result.add(entry);
      if (result.size() >= limit) break;
    }
    return result;
  }

  public void clearHistory() {
    this.history.clear();
    this.dirty = true;
  }

  public void clearKit(String kitId) {
    String id = kitId.toLowerCase(Locale.ROOT);
    this.kitClaims.remove(id);
    for (Map<String, Integer> inner : this.playerClaims.values()) {
      inner.remove(id);
    }
    this.history.removeIf(entry -> entry.kit().equals(id));
    this.dirty = true;
  }

  public void resetPlayer(UUID uuid) {
    this.playerClaims.remove(uuid);
    String id = uuid.toString();
    this.history.removeIf(entry -> entry.player().equals(id));
    this.dirty = true;
  }

  public boolean isDirty() {
    return this.dirty;
  }

  public void autosave() {
    if (this.dirty) {
      this.save();
    }
  }

  public int historySize() {
    return this.history.size();
  }

  public record HistoryEntry(String player, String kit, String name, long time) {}
}
