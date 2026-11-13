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
  private final Map<UUID, Map<String, DatabaseProvider.CooldownRecord>> cache =
      new ConcurrentHashMap<UUID, Map<String, DatabaseProvider.CooldownRecord>>();

  public CooldownManager(DatabaseManager databaseManager) {
    this.databaseManager = databaseManager;
  }

  public void loadPlayer(UUID uuid) {
    this.databaseManager
        .loadPlayerData(uuid)
        .thenAccept(map -> this.cache.put(uuid, new ConcurrentHashMap(map)));
  }

  public void unloadPlayer(UUID uuid) {
    this.cache.remove(uuid);
  }

  public long getRemaining(UUID uuid, String kitId) {
    DatabaseProvider.CooldownRecord rec =
        (DatabaseProvider.CooldownRecord)
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
    if (kit.isOneTime()) {
      return !this.hasClaimed(player.getUniqueId(), kit.getId());
    }
    return this.getRemaining(player.getUniqueId(), kit.getId()) <= 0L;
  }

  public void recordClaim(UUID uuid, Kit kit) {
    long next = Instant.now().getEpochSecond() + kit.getCooldownSeconds();
    this.cache
        .computeIfAbsent(uuid, v -> new ConcurrentHashMap())
        .put(kit.getId(), new DatabaseProvider.CooldownRecord(next));
    this.databaseManager.saveCooldown(uuid, kit.getId(), next);
  }

  public void reset(UUID uuid, String kitId) {
    this.cache.computeIfAbsent(uuid, v -> new ConcurrentHashMap()).remove(kitId.toLowerCase());
    this.databaseManager.resetCooldown(uuid, kitId);
  }
}
