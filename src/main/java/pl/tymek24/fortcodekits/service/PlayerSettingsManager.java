package pl.tymek24.fortcodekits.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.tymek24.fortcodekits.FortCodeKits;

public class PlayerSettingsManager {
  public static final String KEY_SOUNDS = "sounds";
  public static final String KEY_PARTICLES = "particles";
  public static final String KEY_CLAIM_ANIMATION = "claim-animation";
  public static final String KEY_TEXT_ANIMATION = "text-animation";
  public static final String KEY_TITLE_FEEDBACK = "title-feedback";
  public static final String KEY_BOSSBAR_FEEDBACK = "bossbar-feedback";
  public static final String KEY_ACTIONBAR_FEEDBACK = "actionbar-feedback";

  private static final String[] ALL_KEYS = {
    KEY_SOUNDS,
    KEY_PARTICLES,
    KEY_CLAIM_ANIMATION,
    KEY_TEXT_ANIMATION,
    KEY_TITLE_FEEDBACK,
    KEY_BOSSBAR_FEEDBACK,
    KEY_ACTIONBAR_FEEDBACK
  };

  private final FortCodeKits plugin;
  private final File file;
  private final Map<UUID, Map<String, Boolean>> settings =
      new ConcurrentHashMap<UUID, Map<String, Boolean>>();

  public PlayerSettingsManager(FortCodeKits plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "playersettings.yml");
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
    InputStream resource = this.plugin.getResource("playersettings.yml");
    if (resource != null) {
      YamlConfiguration defaults =
          YamlConfiguration.loadConfiguration(
              new InputStreamReader(resource, StandardCharsets.UTF_8));
      config.setDefaults((Configuration) defaults);
      config.options().copyDefaults(true);
    }
    this.settings.clear();
    for (String uuidKey : config.getKeys(false)) {
      UUID uuid;
      try {
        uuid = UUID.fromString(uuidKey);
      } catch (IllegalArgumentException ex) {
        continue;
      }
      Map<String, Boolean> values = new ConcurrentHashMap<String, Boolean>();
      for (String key : ALL_KEYS) {
        String path = uuidKey + "." + key;
        if (config.contains(path)) {
          values.put(key, config.getBoolean(path));
        }
      }
      if (!values.isEmpty()) {
        this.settings.put(uuid, values);
      }
    }
  }

  public void save() {
    YamlConfiguration config = new YamlConfiguration();
    for (Map.Entry<UUID, Map<String, Boolean>> entry : this.settings.entrySet()) {
      for (Map.Entry<String, Boolean> value : entry.getValue().entrySet()) {
        config.set(entry.getKey().toString() + "." + value.getKey(), value.getValue());
      }
    }
    try {
      config.save(this.file);
    } catch (IOException ex) {
      this.plugin
          .getLogger()
          .warning("Nie udalo sie zapisac playersettings.yml: " + ex.getMessage());
    }
  }

  public boolean get(UUID uuid, String key) {
    Map<String, Boolean> values = this.settings.get(uuid);
    if (values == null) {
      return this.defaultFor(key);
    }
    Boolean value = values.get(key);
    return value == null ? this.defaultFor(key) : value;
  }

  public boolean toggle(UUID uuid, String key) {
    boolean next = !this.get(uuid, key);
    this.settings
        .computeIfAbsent(uuid, id -> new ConcurrentHashMap<String, Boolean>())
        .put(key, next);
    this.save();
    return next;
  }

  public void reset(UUID uuid) {
    this.settings.remove(uuid);
    this.save();
  }

  public boolean defaultFor(String key) {
    return this.plugin.getConfig().getBoolean("player-settings.defaults." + key, true);
  }

  public void unload(UUID uuid) {
    this.settings.remove(uuid);
  }
}
