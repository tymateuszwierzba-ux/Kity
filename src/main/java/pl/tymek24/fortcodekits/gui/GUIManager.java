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

  public void startAutoRefresh() {
    if (this.refreshTask != null) {
      return;
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
                      case PLAYER:
                        {
                          this.playerGUI.refresh(player, holder);
                          break;
                        }
                      case ADMIN:
                        {
                          this.adminGUI.refresh(player, holder);
                          break;
                        }
                      case EDITOR:
                        {
                          this.editorGUI.refresh(player, holder);
                          break;
                        }
                    }
                  }
                },
                interval,
                interval);
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

  public PlayerGUI getPlayerGUI() {
    return this.playerGUI;
  }

  public AdminGUI getAdminGUI() {
    return this.adminGUI;
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
        case PLAYER:
          {
            this.openPlayer(player, holder.getPage());
            break;
          }
        case ADMIN:
          {
            this.openAdmin(player, holder.getPage());
            break;
          }
        case EDITOR:
          {
            if (kit == null
                || holder.getKitId() == null
                || !holder.getKitId().equalsIgnoreCase(kit.getId())) break;
            this.openEditor(player, kit);
            break;
          }
        case RANK:
          {
            if (kit == null
                || holder.getKitId() == null
                || !holder.getKitId().equalsIgnoreCase(kit.getId())) break;
            this.openRank(player, kit, holder.getPage());
            break;
          }
        case FEEDBACK:
          {
            if (kit == null
                || holder.getKitId() == null
                || !holder.getKitId().equalsIgnoreCase(kit.getId())) break;
            this.openFeedback(player, kit);
            break;
          }
        case PREVIEW:
          {
            if (kit == null
                || holder.getKitId() == null
                || !holder.getKitId().equalsIgnoreCase(kit.getId())) break;
            this.openPreview(player, kit, holder.getReturnType());
            break;
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
        case PLAYER:
          {
            this.openPlayer(player, holder.getPage());
            break;
          }
        case ADMIN:
          {
            this.openAdmin(player, holder.getPage());
            break;
          }
        case EDITOR:
          {
            if (holder.getKitId() == null || !holder.getKitId().equalsIgnoreCase(kitId)) break;
            if (player.hasPermission("fortcode.kits.admin")) {
              this.openAdmin(player);
              break;
            }
            this.openPlayer(player);
            break;
          }
        case RANK:
          {
            if (holder.getKitId() == null || !holder.getKitId().equalsIgnoreCase(kitId)) break;
            if (player.hasPermission("fortcode.kits.admin")) {
              this.openAdmin(player);
              break;
            }
            this.openPlayer(player);
            break;
          }
        case FEEDBACK:
          {
            if (holder.getKitId() == null || !holder.getKitId().equalsIgnoreCase(kitId)) break;
            if (player.hasPermission("fortcode.kits.admin")) {
              this.openAdmin(player);
              break;
            }
            this.openPlayer(player);
            break;
          }
        case PREVIEW:
          {
            if (holder.getKitId() == null || !holder.getKitId().equalsIgnoreCase(kitId)) break;
            if (holder.getReturnType() == GuiType.ADMIN
                || holder.getReturnType() == GuiType.EDITOR) {
              if (player.hasPermission("fortcode.kits.admin")) {
                this.openAdmin(player);
                break;
              }
              this.openPlayer(player);
              break;
            }
            this.openPlayer(player);
            break;
          }
      }
    }
  }
}
