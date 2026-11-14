package pl.tymek24.fortcodekits.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SQLiteProvider extends AbstractSqlProvider {
  public SQLiteProvider(File dataFolder, Executor executor) {
    super(SQLiteProvider.buildSource(dataFolder), executor);
  }

  private static HikariDataSource buildSource(File folder) {
    File db = new File(folder, "kits.db");
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:sqlite:" + db.getAbsolutePath());
    config.setMaximumPoolSize(1);
    config.setPoolName("fortcode-kits-sqlite");
    return new HikariDataSource(config);
  }

  @Override
  public CompletableFuture<Void> init() {
    return CompletableFuture.runAsync(
        () -> {
          String sql =
              "CREATE TABLE IF NOT EXISTS kit_cooldowns (player_uuid TEXT NOT NULL,kit_id TEXT NOT"
                  + " NULL,next_claim_at INTEGER NOT NULL,PRIMARY KEY(player_uuid, kit_id))";
          try (Connection connection = this.dataSource.getConnection();
              Statement st = connection.createStatement(); ) {
            st.executeUpdate(sql);
          } catch (SQLException ex) {
            throw new RuntimeException(ex);
          }
        },
        this.executor);
  }
}
