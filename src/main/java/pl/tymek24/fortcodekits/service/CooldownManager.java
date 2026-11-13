package pl.tymek24.fortcodekits.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import pl.tymek24.fortcodekits.database.DatabaseManager;
import pl.tymek24.fortcodekits.database.DatabaseProvider;
import pl.tymek24.fortcodekits.model.Kit;

public class CooldownManager {
  private final DatabaseManager databaseManager;
  private final StatsManager statsManager;
  private final Map<UUID, Map<String, DatabaseProvider.CooldownRecord>> cache =
      new ConcurrentHashMap<UUID, Map<String, DatabaseProvider.CooldownRecord>>();

  public CooldownManager(DatabaseManager databaseManager, StatsManager statsManager) {
    this.databaseManager = databaseManager;
    this.statsManager = statsManager;
  }

  public void loadPlayer(UUID uuid) {
    this.databaseManager
        .loadPlayerData(uuid)
        .thenAccept(map -> this.cache.put(uuid, new ConcurrentHashMap<String, DatabaseProvider.CooldownRecord>(map)));
  }

  public void unloadPlayer(UUID uuid) {
    this.cache.remove(uuid);
  }

  public long getRemaining(UUID uuid, String kitId) {
    DatabaseProvider.CooldownRecord rec =
        this.cache.getOrDefault(uuid, Map.of()).get(kitId.toLowerCase());
    if (rec == null) {
      return 0L;
    }
    long now = Instant.now().getEpochSecond();
    return Math.max(0L, rec.nextClaimAt() - now);
  }

  public boolean hasClaimed(UUID uuid, String kitId) {
    return this.cache.getOrDefault(uuid, Map.of()).containsKey(kitId.toLowerCase());
  }

  public boolean canClaim(Player player, Kit kit) {
    UUID uuid = player.getUniqueId();
    if (kit.isOneTime() && this.hasClaimed(uuid, kit.getId())) {
      return false;
    }
    if (this.getRemaining(uuid, kit.getId()) > 0L) {
      return false;
    }
    if (kit.getClaimCountLimit() != null) {
      int used = this.statsManager.getPlayerKitClaims(uuid, kit.getId());
      if (used >= kit.getClaimCountLimit()) {
        return false;
      }
    }
    return true;
  }

  public String denyReason(Player player, Kit kit) {
    UUID uuid = player.getUniqueId();
    if (kit.isOneTime() && this.hasClaimed(uuid, kit.getId())) {
      return "one-time";
    }
    if (this.getRemaining(uuid, kit.getId()) > 0L) {
      return "cooldown";
    }
    if (kit.getClaimCountLimit() != null
        && this.statsManager.getPlayerKitClaims(uuid, kit.getId()) >= kit.getClaimCountLimit()) {
      return "claim-limit";
    }
    return "ok";
  }

  public void recordClaim(UUID uuid, Kit kit) {
    long next = Instant.now().getEpochSecond() + kit.getCooldownSeconds();
    this.cache
        .computeIfAbsent(uuid, v -> new ConcurrentHashMap<String, DatabaseProvider.CooldownRecord>())
        .put(kit.getId(), new DatabaseProvider.CooldownRecord(next));
    this.databaseManager.saveCooldown(uuid, kit.getId(), next);
  }

  public void reset(UUID uuid, String kitId) {
    this.cache
        .computeIfAbsent(uuid, v -> new ConcurrentHashMap<String, DatabaseProvider.CooldownRecord>())
        .remove(kitId.toLowerCase());
    this.databaseManager.resetCooldown(uuid, kitId);
  }

  public void resetAllForKit(String kitId) {
    String id = kitId.toLowerCase();
    for (Map<String, DatabaseProvider.CooldownRecord> inner : this.cache.values()) {
      inner.remove(id);
    }
    this.databaseManager.resetAllForKit(id);
  }

  public void resetAllForPlayer(UUID uuid) {
    this.cache.remove(uuid);
    this.databaseManager.resetAllForPlayer(uuid);
  }

  public void resetEverything() {
    this.cache.clear();
    this.databaseManager.resetAll();
  }
}
