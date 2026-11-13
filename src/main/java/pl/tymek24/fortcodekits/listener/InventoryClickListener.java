package pl.tymek24.fortcodekits.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.gui.EditorAction;
import pl.tymek24.fortcodekits.gui.GuiConfig;
import pl.tymek24.fortcodekits.gui.GuiHolder;
import pl.tymek24.fortcodekits.gui.GuiType;
import pl.tymek24.fortcodekits.model.Kit;

public class InventoryClickListener implements Listener {
  private final FortCodeKits plugin;
  private final Map<UUID, Long> antiSpam = new HashMap<UUID, Long>();

  public InventoryClickListener(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    HumanEntity humanEntity = event.getWhoClicked();
    if (!(humanEntity instanceof Player)) {
      return;
    }
    Player player = (Player) humanEntity;
    Inventory top = event.getView().getTopInventory();
    InventoryHolder holder = top.getHolder();
    if (!(holder instanceof GuiHolder)) {
      return;
    }
    GuiHolder guiHolder = (GuiHolder) holder;
    if (event.getRawSlot() >= top.getSize()) {
      return;
    }
    event.setCancelled(true);
    if (this.isSpam(player.getUniqueId())) {
      return;
    }
    int slot = event.getRawSlot();
    switch (guiHolder.getType()) {
      case PLAYER:
        {
          this.handlePlayerClick(player, guiHolder, slot, event.isShiftClick());
          break;
        }
      case ADMIN:
        {
          this.handleAdminClick(
              player, guiHolder, slot, event.isShiftClick(), event.isRightClick());
          break;
        }
      case PREVIEW:
        {
          this.handlePreviewClick(player, guiHolder, slot);
          break;
        }
      case EDITOR:
        {
          this.handleEditorClick(player, guiHolder, slot);
          break;
        }
      case RANK:
        {
          this.handleRankClick(player, guiHolder, slot);
          break;
        }
      case FEEDBACK:
        {
          this.handleFeedbackClick(player, guiHolder, slot);
          break;
        }
    }
  }

  private void handlePlayerClick(Player player, GuiHolder holder, int slot, boolean shift) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    int prevSlot = gui.getConfig().getInt("player.navigation.prev.slot", -1);
    int nextSlot = gui.getConfig().getInt("player.navigation.next.slot", -1);
    if (slot == prevSlot) {
      this.plugin.getGuiManager().openPlayer(player, Math.max(1, holder.getPage() - 1));
      return;
    }
    if (slot == nextSlot) {
      this.plugin.getGuiManager().openPlayer(player, holder.getPage() + 1);
      return;
    }
    String kitId = holder.getKitSlots().get(slot);
    if (kitId == null) {
      return;
    }
    Optional<Kit> opt = this.plugin.getKitManager().getKit(kitId);
    if (opt.isEmpty()) {
      return;
    }
    Kit kit = opt.get();
    if (shift) {
      this.plugin.getGuiManager().openPreview(player, kit, GuiType.PLAYER);
      return;
    }
    if (!this.plugin.hasKitPermission(player, kit)) {
      this.plugin.sendMessage(player, "no-permission");
      return;
    }
    this.plugin.giveKit(player, kit, false);
  }

  private void handleAdminClick(
      Player player, GuiHolder holder, int slot, boolean shift, boolean rightClick) {
    if (!player.hasPermission("fortcode.kits.admin")) {
      this.plugin.sendMessage(player, "no-permission");
      return;
    }
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    int createSlot = gui.getConfig().getInt("admin.actions.create.slot", -1);
    int prevSlot = gui.getConfig().getInt("admin.navigation.prev.slot", -1);
    int nextSlot = gui.getConfig().getInt("admin.navigation.next.slot", -1);
    if (slot == createSlot) {
      this.plugin.getChatInputListener().requestInput(player, ChatInputType.CREATE_KIT, null);
      return;
    }
    if (slot == prevSlot) {
      this.plugin.getGuiManager().openAdmin(player, Math.max(1, holder.getPage() - 1));
      return;
    }
    if (slot == nextSlot) {
      this.plugin.getGuiManager().openAdmin(player, holder.getPage() + 1);
      return;
    }
    String kitId = holder.getKitSlots().get(slot);
    if (kitId == null) {
      return;
    }
    Optional<Kit> opt = this.plugin.getKitManager().getKit(kitId);
    if (opt.isEmpty()) {
      return;
    }
    Kit kit = opt.get();
    if (rightClick && shift) {
      this.plugin.getKitManager().remove(kit.getId());
      this.plugin.getKitManager().save();
      this.plugin.sendMessage(player, "kit-deleted", Map.of("{kit}", kit.getDisplayName()));
      this.plugin.getGuiManager().refreshOpenGuisAfterDelete(kit.getId());
      this.plugin.getGuiManager().openAdmin(player, holder.getPage());
      return;
    }
    if (shift && !rightClick) {
      kit.setItems(this.plugin.collectInventoryItems(player));
      this.plugin.getKitManager().save();
      this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
      this.plugin.getGuiManager().refreshOpenGuis(kit);
      this.plugin.getGuiManager().openAdmin(player, holder.getPage());
      return;
    }
    if (rightClick) {
      this.plugin.getGuiManager().openEditor(player, kit);
      return;
    }
    this.plugin.giveKit(player, kit, true);
  }

  private void handlePreviewClick(Player player, GuiHolder holder, int slot) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    int backSlot = gui.getConfig().getInt("preview.back.slot", -1);
    if (slot != backSlot) {
      return;
    }
    if (holder.getReturnType() == GuiType.ADMIN) {
      this.plugin.getGuiManager().openAdmin(player);
    } else if (holder.getReturnType() == GuiType.EDITOR) {
      if (holder.getKitId() == null) {
        this.plugin.getGuiManager().openAdmin(player);
        return;
      }
      this.plugin
          .getKitManager()
          .getKit(holder.getKitId())
          .ifPresent(kit -> this.plugin.getGuiManager().openEditor(player, (Kit) kit));
    } else {
      this.plugin.getGuiManager().openPlayer(player);
    }
  }

  private void handleEditorClick(Player player, GuiHolder holder, int slot) {
    if (!player.hasPermission("fortcode.kits.admin")) {
      this.plugin.sendMessage(player, "no-permission");
      return;
    }
    EditorAction action = holder.getEditorActions().get(slot);
    if (action == null) {
      return;
    }
    if (holder.getKitId() == null) {
      return;
    }
    Optional<Kit> opt = this.plugin.getKitManager().getKit(holder.getKitId());
    if (opt.isEmpty()) {
      return;
    }
    Kit kit = opt.get();
    switch (action) {
      case SET_ITEMS:
        {
          kit.setItems(this.plugin.collectInventoryItems(player));
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().openEditor(player, kit);
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case SET_ICON:
        {
          ItemStack hand = player.getInventory().getItemInMainHand();
          if (hand.getType().isAir()) {
            this.plugin.sendMessage(player, "need-item-hand");
            return;
          }
          kit.setIcon(hand);
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().openEditor(player, kit);
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case SET_NAME:
        {
          this.plugin
              .getChatInputListener()
              .requestInput(player, ChatInputType.SET_NAME, kit.getId());
          break;
        }
      case SET_COOLDOWN:
        {
          this.plugin
              .getChatInputListener()
              .requestInput(player, ChatInputType.SET_COOLDOWN, kit.getId());
          break;
        }
      case SET_SLOT:
        {
          this.plugin
              .getChatInputListener()
              .requestInput(player, ChatInputType.SET_SLOT, kit.getId());
          break;
        }
      case SET_RANK:
        {
          this.plugin.getGuiManager().openRank(player, kit);
          break;
        }
      case SET_FEEDBACK_TYPE:
        {
          this.plugin.getGuiManager().openFeedback(player, kit);
          break;
        }
      case TOGGLE_ONE_TIME:
        {
          kit.setOneTime(!kit.isOneTime());
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().openEditor(player, kit);
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case PREVIEW:
        {
          this.plugin.getGuiManager().openPreview(player, kit, GuiType.EDITOR);
          break;
        }
      case DELETE:
        {
          this.plugin.getKitManager().remove(kit.getId());
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-deleted", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuisAfterDelete(kit.getId());
          this.plugin.getGuiManager().openAdmin(player);
          break;
        }
      case BACK:
        {
          this.plugin.getGuiManager().openAdmin(player);
          break;
        }
    }
  }

  private void handleRankClick(Player player, GuiHolder holder, int slot) {
    if (!player.hasPermission("fortcode.kits.admin")) {
      this.plugin.sendMessage(player, "no-permission");
      return;
    }
    if (holder.getKitId() == null) {
      return;
    }
    Optional<Kit> opt = this.plugin.getKitManager().getKit(holder.getKitId());
    if (opt.isEmpty()) {
      return;
    }
    Kit kit = opt.get();
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    int backSlot = gui.getConfig().getInt("rank.back.slot", -1);
    int noneSlot = gui.getConfig().getInt("rank.none.slot", -1);
    int prevSlot = gui.getConfig().getInt("rank.navigation.prev.slot", -1);
    int nextSlot = gui.getConfig().getInt("rank.navigation.next.slot", -1);
    if (slot == backSlot) {
      this.plugin.getGuiManager().openEditor(player, kit);
      return;
    }
    if (slot == noneSlot) {
      kit.setRequiredGroup(null);
      this.plugin.getKitManager().save();
      this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
      this.plugin.getGuiManager().openEditor(player, kit);
      this.plugin.getGuiManager().refreshOpenGuis(kit);
      return;
    }
    if (slot == prevSlot) {
      this.plugin.getGuiManager().openRank(player, kit, Math.max(1, holder.getPage() - 1));
      return;
    }
    if (slot == nextSlot) {
      this.plugin.getGuiManager().openRank(player, kit, holder.getPage() + 1);
      return;
    }
    String group = holder.getKitSlots().get(slot);
    if (group == null) {
      return;
    }
    kit.setRequiredGroup(group);
    this.plugin.getKitManager().save();
    this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
    this.plugin.getGuiManager().openEditor(player, kit);
    this.plugin.getGuiManager().refreshOpenGuis(kit);
  }

  private void handleFeedbackClick(Player player, GuiHolder holder, int slot) {
    if (!player.hasPermission("fortcode.kits.admin")) {
      this.plugin.sendMessage(player, "no-permission");
      return;
    }
    if (holder.getKitId() == null) {
      return;
    }
    Optional<Kit> opt = this.plugin.getKitManager().getKit(holder.getKitId());
    if (opt.isEmpty()) {
      return;
    }
    Kit kit = opt.get();
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    int backSlot = gui.getConfig().getInt("feedback.back.slot", -1);
    if (slot == backSlot) {
      this.plugin.getGuiManager().openEditor(player, kit);
      return;
    }
    String type = holder.getKitSlots().get(slot);
    if (type == null) {
      return;
    }
    kit.setFeedbackType(type);
    this.plugin.getKitManager().save();
    this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
    this.plugin.getGuiManager().openEditor(player, kit);
    this.plugin.getGuiManager().refreshOpenGuis(kit);
  }

  private boolean isSpam(UUID uuid) {
    long now = System.currentTimeMillis();
    long prev = this.antiSpam.getOrDefault(uuid, 0L);
    this.antiSpam.put(uuid, now);
    return now - prev < 200L;
  }
}
