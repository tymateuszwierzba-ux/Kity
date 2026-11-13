package pl.tymek24.fortcodekits.database;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bukkit.configuration.file.FileConfiguration;
import pl.tymek24.fortcodekits.FortCodeKits;

public class DatabaseManager {
  private final FortCodeKits plugin;
  private final ExecutorService dbExecutor;
  private DatabaseProvider provider;

  public DatabaseManager(FortCodeKits plugin) {
    this.plugin = plugin;
    this.dbExecutor = Executors.newFixedThreadPool(2);
  }

  public CompletableFuture<Void> init() {
    FileConfiguration cfg = this.plugin.getConfig();
    String backend =
        Objects.requireNonNullElse(cfg.getString("database.type"), "sqlite")
            .toLowerCase(Locale.ROOT);
    if ("mysql".equals(backend)) {
      String host = cfg.getString("database.mysql.host", "localhost");
      int port = cfg.getInt("database.mysql.port", 3306);
      String database = cfg.getString("database.mysql.database", "minecraft");
      String user = cfg.getString("database.mysql.user", "root");
      String password = cfg.getString("database.mysql.password", "");
      String endpoint = host + ":" + port + "/" + database;
      try {
        this.provider = new MySQLProvider(host, port, database, user, password, this.dbExecutor);
      } catch (Exception ex2) {
        this.plugin
            .getLogger()
            .warning(
                "MySQL ("
                    + endpoint
                    + ") jest niedostepny, fallback na SQLite: "
                    + DatabaseManager.rootMessage(ex2));
        this.provider = new SQLiteProvider(this.plugin.getDataFolder(), (Executor) this.dbExecutor);
        return this.provider.init();
      }
      return this.provider
          .init()
          .exceptionallyCompose(
              ex -> {
                this.plugin
                    .getLogger()
                    .warning(
                        "MySQL init ("
                            + endpoint
                            + ") nieudany, fallback na SQLite: "
                            + DatabaseManager.rootMessage(ex));
                this.provider =
                    new SQLiteProvider(this.plugin.getDataFolder(), (Executor) this.dbExecutor);
                return this.provider.init();
              });
    }
    this.provider = new SQLiteProvider(this.plugin.getDataFolder(), (Executor) this.dbExecutor);
    return this.provider.init();
  }

  public CompletableFuture<Map<String, DatabaseProvider.CooldownRecord>> loadPlayerData(UUID uuid) {
    return this.provider.loadPlayerData(uuid);
  }

  public CompletableFuture<Void> saveCooldown(UUID uuid, String kitId, long next) {
    return this.provider.saveCooldown(uuid, kitId, next);
  }

  public CompletableFuture<Void> resetCooldown(UUID uuid, String kitId) {
    return this.provider.resetCooldown(uuid, kitId);
  }

  public CompletableFuture<Void> resetAllForKit(String kitId) {
    return this.provider.resetAllForKit(kitId);
  }

  public CompletableFuture<Void> resetAllForPlayer(UUID uuid) {
    return this.provider.resetAllForPlayer(uuid);
  }

  public CompletableFuture<Void> resetAll() {
    return this.provider.resetAll();
  }

  public CompletableFuture<Void> close() {
    return this.provider == null
        ? CompletableFuture.completedFuture(null)
        : this.provider.close().whenComplete((v, t) -> this.dbExecutor.shutdownNow());
  }

  private static String rootMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return Objects.toString(current.getMessage(), current.getClass().getSimpleName());
  }
}
