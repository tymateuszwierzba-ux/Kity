package pl.tymek24.fortcodekits.security;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.tymek24.fortcodekits.FortCodeKits;

public class LicenseGuard {
  private final FortCodeKits plugin;
  private final File file;
  private LicenseState state;

  public LicenseGuard(FortCodeKits plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "license.yml");
    this.state = new LicenseState(true, false, "not-checked", this.resolveRuntimeServerId(), "", "never");
  }

  public LicenseState reloadAndValidate() {
    if (!this.file.exists()) {
      this.plugin.saveResource("license.yml", false);
    }

    YamlConfiguration loaded = YamlConfiguration.loadConfiguration(this.file);
    InputStream resource = this.plugin.getResource("license.yml");
    if (resource != null) {
      YamlConfiguration defaults =
          YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
      loaded.setDefaults(defaults);
      loaded.options().copyDefaults(true);
      try {
        loaded.save(this.file);
      } catch (IOException ex) {
        this.plugin.getLogger().warning("Nie udalo sie zapisac license.yml: " + ex.getMessage());
      }
    }

    boolean enabled = loaded.getBoolean("license.enabled", false);
    String runtimeServerId = this.resolveRuntimeServerId();
    String owner = this.normalize(loaded.getString("license.owner", ""));
    String serverIdRaw = this.normalize(loaded.getString("license.server-id", "auto"));
    String expiresRaw = this.normalize(loaded.getString("license.expires-at", "never"));
    String keyRaw = this.normalizeKey(loaded.getString("license.key", ""));

    if (!enabled) {
      this.state = new LicenseState(true, false, "license-disabled", runtimeServerId, owner, expiresRaw);
      return this.state;
    }

    String serverId = serverIdRaw.equals("auto") ? runtimeServerId : serverIdRaw;
    if (owner.isBlank()) {
      this.state = new LicenseState(false, true, "owner-empty", runtimeServerId, owner, expiresRaw);
      return this.state;
    }

    if (serverId.isBlank()) {
      this.state = new LicenseState(false, true, "server-id-empty", runtimeServerId, owner, expiresRaw);
      return this.state;
    }

    if (!serverId.equals("*") && !serverId.equalsIgnoreCase(runtimeServerId)) {
      this.state =
          new LicenseState(
              false,
              true,
              "server-id-mismatch (expected: " + serverId + ", runtime: " + runtimeServerId + ")",
              runtimeServerId,
              owner,
              expiresRaw);
      return this.state;
    }

    if (!expiresRaw.equals("never")) {
      try {
        LocalDate expiry = LocalDate.parse(expiresRaw);
        if (LocalDate.now().isAfter(expiry)) {
          this.state = new LicenseState(false, true, "license-expired", runtimeServerId, owner, expiresRaw);
          return this.state;
        }
      } catch (DateTimeParseException ex) {
        this.state = new LicenseState(false, true, "invalid-expiry-format", runtimeServerId, owner, expiresRaw);
        return this.state;
      }
    }

    String payload = this.buildPayload(owner, serverId, expiresRaw);
    String expectedKey = this.generateKey(payload);
    if (keyRaw.isBlank()) {
      this.state = new LicenseState(false, true, "key-empty", runtimeServerId, owner, expiresRaw);
      return this.state;
    }

    if (!this.constantTimeEquals(expectedKey, keyRaw)) {
      this.state = new LicenseState(false, true, "invalid-key", runtimeServerId, owner, expiresRaw);
      return this.state;
    }

    this.state = new LicenseState(true, true, "ok", runtimeServerId, owner, expiresRaw);
    return this.state;
  }

  public boolean isValid() {
    return this.state != null && this.state.valid();
  }

  public LicenseState getState() {
    return this.state;
  }

  public String getRequiredKeyHint(String owner, String serverId, String expiresAt) {
    String payload = this.buildPayload(this.normalize(owner), this.normalize(serverId), this.normalize(expiresAt));
    return this.generateKey(payload);
  }

  private String buildPayload(String owner, String serverId, String expiresAt) {
    String pluginId = this.plugin.getDescription().getName().toLowerCase(Locale.ROOT);
    return owner + "|" + serverId + "|" + expiresAt + "|" + pluginId;
  }

  private String resolveRuntimeServerId() {
    String host = this.normalize(this.plugin.getServer().getIp());
    if (host.isBlank()) {
      host = "0.0.0.0";
    }
    int port = this.plugin.getServer().getPort();
    return host + ":" + port;
  }

  private String generateKey(String payload) {
    String full = this.sha256(payload + "|" + this.secret());
    return full.substring(0, 32).toUpperCase(Locale.ROOT);
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder out = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        out.append(String.format("%02x", b));
      }
      return out.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }

  private String secret() {
    int[] encoded = {
      50, 53, 48, 57, 57, 40, 62, 44, 35, 62, 59, 49, 52, 44, 95, 46, 39, 56, 40, 62, 95, 40, 48,
      54, 39, 48, 33
    };
    char[] chars = new char[encoded.length];
    for (int i = 0; i < encoded.length; i++) {
      chars[i] = (char) (encoded[i] ^ 90);
    }
    return new String(chars);
  }

  private boolean constantTimeEquals(String expected, String current) {
    if (expected == null || current == null) {
      return false;
    }

    String left = this.normalizeKey(expected);
    String right = this.normalizeKey(current);
    if (left.length() != right.length()) {
      return false;
    }

    int diff = 0;
    for (int i = 0; i < left.length(); i++) {
      diff |= left.charAt(i) ^ right.charAt(i);
    }
    return diff == 0;
  }

  private String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeKey(String value) {
    if (value == null) {
      return "";
    }
    return value.replaceAll("[^a-zA-Z0-9]", "").trim().toUpperCase(Locale.ROOT);
  }

  public record LicenseState(
      boolean valid,
      boolean enabled,
      String reason,
      String runtimeServerId,
      String owner,
      String expiresAt) {}
}
