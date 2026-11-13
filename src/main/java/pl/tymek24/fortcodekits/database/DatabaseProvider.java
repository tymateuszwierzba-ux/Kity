package pl.tymek24.fortcodekits.database;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DatabaseProvider {
  public CompletableFuture<Void> init();

  public CompletableFuture<Void> close();

  public CompletableFuture<Map<String, CooldownRecord>> loadPlayerData(UUID var1);

  public CompletableFuture<Void> saveCooldown(UUID var1, String var2, long var3);

  public CompletableFuture<Void> resetCooldown(UUID var1, String var2);

  public CompletableFuture<Void> resetAllForKit(String var1);

  public CompletableFuture<Void> resetAllForPlayer(UUID var1);

  public CompletableFuture<Void> resetAll();

  public record CooldownRecord(long nextClaimAt) {}
}
