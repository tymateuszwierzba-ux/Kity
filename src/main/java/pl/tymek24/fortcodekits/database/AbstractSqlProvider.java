package pl.tymek24.fortcodekits.database;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class AbstractSqlProvider implements DatabaseProvider {
  protected final HikariDataSource dataSource;
  protected final Executor executor;
  private volatile Boolean hasUsedCountColumn;

  protected AbstractSqlProvider(HikariDataSource dataSource, Executor executor) {
    this.dataSource = dataSource;
    this.executor = executor;
  }

  @Override
  public CompletableFuture<Void> close() {
    return CompletableFuture.runAsync(this.dataSource::close, this.executor);
  }

  @Override
  public CompletableFuture<Map<String, DatabaseProvider.CooldownRecord>> loadPlayerData(
      UUID playerId) {
    return CompletableFuture.supplyAsync(
        () -> {
          HashMap<String, DatabaseProvider.CooldownRecord> result =
              new HashMap<String, DatabaseProvider.CooldownRecord>();
          String sql = "SELECT kit_id, next_claim_at FROM kit_cooldowns WHERE player_uuid = ?";
          try (Connection connection = this.dataSource.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql); ) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery(); ) {
              while (rs.next()) {
                result.put(
                    rs.getString("kit_id"),
                    new DatabaseProvider.CooldownRecord(rs.getLong("next_claim_at")));
              }
            }
          } catch (SQLException ex) {
            throw new RuntimeException(ex);
          }
          return result;
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
                          + " VALUES(?,?,?,0) ON CONFLICT(player_uuid, kit_id) DO UPDATE SET"
                          + " next_claim_at=excluded.next_claim_at, used_count=used_count"
                    : "INSERT INTO kit_cooldowns(player_uuid, kit_id, next_claim_at) VALUES(?,?,?)"
                          + " ON CONFLICT(player_uuid, kit_id) DO UPDATE SET"
                          + " next_claim_at=excluded.next_claim_at";
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

  @Override
  public CompletableFuture<Void> resetCooldown(UUID playerId, String kitId) {
    return CompletableFuture.runAsync(
        () -> {
          String sql = "DELETE FROM kit_cooldowns WHERE player_uuid = ? AND kit_id = ?";
          try (Connection connection = this.dataSource.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql); ) {
            ps.setString(1, playerId.toString());
            ps.setString(2, kitId.toLowerCase());
            ps.executeUpdate();
          } catch (SQLException ex) {
            throw new RuntimeException(ex);
          }
        },
        this.executor);
  }

  @Override
  public CompletableFuture<Void> resetAllForKit(String kitId) {
    return CompletableFuture.runAsync(
        () -> {
          String sql = "DELETE FROM kit_cooldowns WHERE kit_id = ?";
          try (Connection connection = this.dataSource.getConnection();
              PreparedStatement ps = connection.prepareStatement(sql); ) {
            ps.setString(1, kitId.toLowerCase());
            ps.executeUpdate();
          } catch (SQLException ex) {
            throw new RuntimeException(ex);
          }
        },
        this.executor);
  }

  protected boolean hasUsedCountColumn(Connection connection) throws SQLException {
    Boolean cached = this.hasUsedCountColumn;
    if (cached != null) {
      return cached;
    }
    boolean exists = false;
    DatabaseMetaData meta = connection.getMetaData();
    try (ResultSet rs = meta.getColumns(null, null, "kit_cooldowns", null); ) {
      while (rs.next()) {
        String name = rs.getString("COLUMN_NAME");
        if (!"used_count".equalsIgnoreCase(name)) continue;
        exists = true;
        break;
      }
    }
    this.hasUsedCountColumn = exists;
    return exists;
  }
}
