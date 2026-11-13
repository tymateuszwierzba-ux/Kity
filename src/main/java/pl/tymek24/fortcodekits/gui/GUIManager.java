package pl.tymek24.fortcodekits.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.model.Kit;

public class GUIManager {
  private final FortCodeKits plugin;
  private final GuiConfig guiConfig;
  private final PlayerGUI playerGUI;
  private final AdminGUI adminGUI;
  private final PreviewGUI previewGUI;
  private final EditorGUI editorGUI;
  private final RankGUI rankGUI;
  private final FeedbackGUI feedbackGUI;
  private final SettingsGUI settingsGUI;
  private final StatsGUI statsGUI;
  private final HistoryGUI historyGUI;
  private final FavoritesGUI favoritesGUI;
  private final TopGUI topGUI;
  private final ConfirmGUI confirmGUI;
  private BukkitTask refreshTask;

  public GUIManager(FortCodeKits plugin) {
    this.plugin = plugin;
    this.guiConfig = new GuiConfig(plugin);
    this.playerGUI = new PlayerGUI(plugin);
    this.adminGUI = new AdminGUI(plugin);
    this.previewGUI = new PreviewGUI(plugin);
    this.editorGUI = new EditorGUI(plugin);
    this.rankGUI = new RankGUI(plugin);
    this.feedbackGUI = new FeedbackGUI(plugin);
    this.settingsGUI = new SettingsGUI(plugin);
    this.statsGUI = new StatsGUI(plugin);
    this.historyGUI = new HistoryGUI(plugin);
    this.favoritesGUI = new FavoritesGUI(plugin);
    this.topGUI = new TopGUI(plugin);
    this.confirmGUI = new ConfirmGUI(plugin);
  }

  public void reload() {
    this.guiConfig.reload();
  }

  public GuiConfig getGuiConfig() {
    return this.guiConfig;
  }

  public void openPlayer(Player player) {
    this.playerGUI.open(player);
  }

  public void openPlayer(Player player, int page) {
    this.playerGUI.open(player, page);
  }

  public void openAdmin(Player player) {
    this.adminGUI.open(player);
  }

  public void openAdmin(Player player, int page) {
    this.adminGUI.open(player, page);
  }

  public void openPreview(Player player, Kit kit, GuiType returnType) {
    this.previewGUI.open(player, kit, returnType);
  }

  public void openEditor(Player player, Kit kit) {
    this.editorGUI.open(player, kit);
  }

  public void openRank(Player player, Kit kit) {
    this.rankGUI.open(player, kit);
  }

  public void openRank(Player player, Kit kit, int page) {
    this.rankGUI.open(player, kit, page);
  }

  public void openFeedback(Player player, Kit kit) {
    this.feedbackGUI.open(player, kit);
  }

  public void openSettings(Player player) {
    this.settingsGUI.open(player);
  }

  public void openStats(Player player) {
    this.statsGUI.open(player);
  }

  public void openStats(Player player, int page) {
    this.statsGUI.open(player, page);
  }

  public void openHistory(Player player) {
    this.historyGUI.open(player);
  }

  public void openHistory(Player player, int page) {
    this.historyGUI.open(player, page);
  }

  public void openFavorites(Player player) {
    this.favoritesGUI.open(player);
  }

  public void openFavorites(Player player, int page) {
    this.favoritesGUI.open(player, page);
  }

  public void openTop(Player player) {
    this.topGUI.open(player);
  }

  public void openConfirm(
      Player player, String action, String kitId, GuiType returnType, String message) {
    this.confirmGUI.open(player, action, kitId, returnType, message);
  }

  public PlayerGUI getPlayerGUI() {
    return this.playerGUI;
  }

  public AdminGUI getAdminGUI() {
    return this.adminGUI;
  }

  public StartAutoRefreshResult startAutoRefresh() {
    if (this.refreshTask != null) {
      return StartAutoRefreshResult.ALREADY_RUNNING;
    }
    long interval = Math.max(1L, this.plugin.getTextAnimationManager().getRefreshTicks());
    this.refreshTask =
        Bukkit.getScheduler()
            .runTaskTimer(
                this.plugin,
                () -> {
                  for (Player player : Bukkit.getOnlinePlayers()) {
                    Inventory top = player.getOpenInventory().getTopInventory();
                    InventoryHolder inventoryHolder = top.getHolder();
                    if (!(inventoryHolder instanceof GuiHolder)) {
                      continue;
                    }
                    GuiHolder holder = (GuiHolder) inventoryHolder;
                    switch (holder.getType()) {
                      case PLAYER -> this.playerGUI.refresh(player, holder);
                      case ADMIN -> this.adminGUI.refresh(player, holder);
                      case EDITOR -> this.editorGUI.refresh(player, holder);
                      default -> {
                      }
                    }
                  }
                },
                interval,
                interval);
    return StartAutoRefreshResult.STARTED;
  }

  public void stopAutoRefresh() {
    if (this.refreshTask == null) {
      return;
    }
    this.refreshTask.cancel();
    this.refreshTask = null;
  }

  public void restartAutoRefresh() {
    this.stopAutoRefresh();
    this.startAutoRefresh();
  }

  public void refreshPlayerIfOpen(Player player) {
    Inventory top = player.getOpenInventory().getTopInventory();
    InventoryHolder inventoryHolder = top.getHolder();
    if (!(inventoryHolder instanceof GuiHolder)) {
      return;
    }
    GuiHolder holder = (GuiHolder) inventoryHolder;
    if (holder.getType() == GuiType.PLAYER) {
      this.playerGUI.refresh(player, holder);
    }
  }

  public void refreshOpenGuis(Kit kit) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      Inventory top = player.getOpenInventory().getTopInventory();
      InventoryHolder inventoryHolder = top.getHolder();
      if (!(inventoryHolder instanceof GuiHolder)) continue;
      GuiHolder holder = (GuiHolder) inventoryHolder;
      switch (holder.getType()) {
        case PLAYER -> this.openPlayer(player, holder.getPage());
        case ADMIN -> this.openAdmin(player, holder.getPage());
        case EDITOR -> {
          if (kit == null
              || holder.getKitId() == null
              || !holder.getKitId().equalsIgnoreCase(kit.getId())) break;
          this.openEditor(player, kit);
        }
        case RANK -> {
          if (kit == null
              || holder.getKitId() == null
              || !holder.getKitId().equalsIgnoreCase(kit.getId())) break;
          this.openRank(player, kit, holder.getPage());
        }
        case FEEDBACK -> {
          if (kit == null
              || holder.getKitId() == null
              || !holder.getKitId().equalsIgnoreCase(kit.getId())) break;
          this.openFeedback(player, kit);
        }
        case PREVIEW -> {
          if (kit == null
              || holder.getKitId() == null
              || !holder.getKitId().equalsIgnoreCase(kit.getId())) break;
          this.openPreview(player, kit, holder.getReturnType());
        }
        case STATS -> this.openStats(player, holder.getPage());
        case HISTORY -> this.openHistory(player, holder.getPage());
        case FAVORITES -> this.openFavorites(player, holder.getPage());
        case TOP -> this.openTop(player);
        case SETTINGS -> this.openSettings(player);
        default -> {
        }
      }
    }
  }

  public void refreshOpenGuisAfterDelete(String kitId) {
    if (kitId == null) {
      return;
    }
    for (Player player : Bukkit.getOnlinePlayers()) {
      Inventory top = player.getOpenInventory().getTopInventory();
      InventoryHolder inventoryHolder = top.getHolder();
      if (!(inventoryHolder instanceof GuiHolder)) continue;
      GuiHolder holder = (GuiHolder) inventoryHolder;
      switch (holder.getType()) {
        case PLAYER -> this.openPlayer(player, holder.getPage());
        case ADMIN -> this.openAdmin(player, holder.getPage());
        case EDITOR, RANK, FEEDBACK -> {
          if (holder.getKitId() == null || !holder.getKitId().equalsIgnoreCase(kitId)) break;
          if (player.hasPermission("fortcode.kits.admin")) {
            this.openAdmin(player);
          } else {
            this.openPlayer(player);
          }
        }
        case PREVIEW -> {
          if (holder.getKitId() == null || !holder.getKitId().equalsIgnoreCase(kitId)) break;
          if (holder.getReturnType() == GuiType.ADMIN
              || holder.getReturnType() == GuiType.EDITOR) {
            if (player.hasPermission("fortcode.kits.admin")) {
              this.openAdmin(player);
            } else {
              this.openPlayer(player);
            }
          } else {
            this.openPlayer(player);
          }
        }
        case FAVORITES -> this.openFavorites(player, holder.getPage());
        case STATS -> this.openStats(player, holder.getPage());
        default -> {
        }
      }
    }
  }

  public enum StartAutoRefreshResult {
    STARTED,
    ALREADY_RUNNING
  }
}
