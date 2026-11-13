package pl.tymek24.fortcodekits.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class MySQLProvider extends AbstractSqlProvider {
  public MySQLProvider(
      String host, int port, String database, String user, String password, Executor executor) {
    super(MySQLProvider.buildSource(host, port, database, user, password), executor);
  }

  private static HikariDataSource buildSource(
      String host, int port, String database, String user, String password) {
    try {
      Class.forName("com.mysql.cj.jdbc.Driver");
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(
        "jdbc:mysql://"
            + host
            + ":"
            + port
            + "/"
            + database
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000&socketTimeout=5000");
    config.setUsername(user);
    config.setPassword(password);
    config.setConnectionTimeout(5000L);
    config.setValidationTimeout(5000L);
    config.setMaximumPoolSize(10);
    config.setPoolName("fortcode-kits-mysql");
    return new HikariDataSource(config);
  }

  @Override
  public CompletableFuture<Void> init() {
    return CompletableFuture.runAsync(
        () -> {
          String sql =
              "CREATE TABLE IF NOT EXISTS kit_cooldowns (player_uuid VARCHAR(36) NOT NULL,kit_id"
                  + " VARCHAR(64) NOT NULL,next_claim_at BIGINT NOT NULL,PRIMARY KEY(player_uuid,"
                  + " kit_id))";
          try (Connection connection = this.dataSource.getConnection();
              Statement st = connection.createStatement(); ) {
            st.executeUpdate(sql);
          } catch (SQLException ex) {
            throw new RuntimeException(ex);
          }
        },
        this.executor);
  }

  @Override
  public CompletableFuture<Void> saveCooldown(UUID playerId, String kitId, long nextClaimAt) {
    return CompletableFuture.runAsync(
        () -> {
          try (Connection connection = this.dataSource.getConnection(); ) {
            boolean hasUsed = this.hasUsedCountColumn(connection);
            String sql =
                hasUsed
                    ? "INSERT INTO kit_cooldowns(player_uuid, kit_id, next_claim_at, used_count)"
                          + " VALUES(?,?,?,0) ON DUPLICATE KEY UPDATE next_claim_at ="
                          + " VALUES(next_claim_at), used_count = used_count"
                    : "INSERT INTO kit_cooldowns(player_uuid, kit_id, next_claim_at) VALUES(?,?,?)"
                          + " ON DUPLICATE KEY UPDATE next_claim_at = VALUES(next_claim_at)";
            try (PreparedStatement ps = connection.prepareStatement(sql); ) {
              ps.setString(1, playerId.toString());
              ps.setString(2, kitId.toLowerCase());
              ps.setLong(3, nextClaimAt);
              ps.executeUpdate();
            }
          } catch (SQLException ex) {
            throw new RuntimeException(ex);
          }
        },
        this.executor);
  }
}
