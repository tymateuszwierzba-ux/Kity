package pl.tymek24.fortcodekits.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.gui.EditorAction;
import pl.tymek24.fortcodekits.gui.GuiConfig;
import pl.tymek24.fortcodekits.gui.GuiHolder;
import pl.tymek24.fortcodekits.gui.GuiSession;
import pl.tymek24.fortcodekits.gui.GuiType;
import pl.tymek24.fortcodekits.model.Kit;
import pl.tymek24.fortcodekits.model.KitRarity;
import pl.tymek24.fortcodekits.model.SortMode;

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
    if (event.getClickedInventory() != null
        && event.getClickedInventory() == player.getInventory()
        && guiHolder.getType() != GuiType.PREVIEW) {
      return;
    }
    if (this.isSpam(player.getUniqueId())) {
      return;
    }
    int slot = event.getRawSlot();
    boolean shift = event.isShiftClick();
    boolean right = event.isRightClick();
    boolean left = event.isLeftClick();
    boolean middle = event.getClick() == ClickType.MIDDLE;
    switch (guiHolder.getType()) {
      case PLAYER -> this.handlePlayerClick(player, guiHolder, slot, shift, right, middle);
      case ADMIN ->
          this.handleAdminClick(player, guiHolder, slot, shift, right, middle, event.isLeftClick());
      case PREVIEW -> this.handlePreviewClick(player, guiHolder, slot);
      case EDITOR -> this.handleEditorClick(player, guiHolder, slot);
      case RANK -> this.handleRankClick(player, guiHolder, slot);
      case FEEDBACK -> this.handleFeedbackClick(player, guiHolder, slot);
      case SETTINGS -> this.handleSettingsClick(player, guiHolder, slot);
      case STATS -> this.handleStatsClick(player, guiHolder, slot);
      case HISTORY -> this.handleHistoryClick(player, guiHolder, slot, shift);
      case FAVORITES -> this.handleFavoritesClick(player, guiHolder, slot, shift, right);
      case TOP -> this.handleTopClick(player, guiHolder, slot);
      case CONFIRM -> this.handleConfirmClick(player, guiHolder, slot, left);
    }
  }

  private void handlePlayerClick(
      Player player, GuiHolder holder, int slot, boolean shift, boolean right, boolean middle) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    String prefix = "player.footer";

    if (this.isButton(gui, prefix + ".prev", slot)) {
      this.plugin.getGuiManager().openPlayer(player, Math.max(1, holder.getPage() - 1));
      return;
    }
    if (this.isButton(gui, prefix + ".next", slot)) {
      this.plugin.getGuiManager().openPlayer(player, holder.getPage() + 1);
      return;
    }
    if (this.isButton(gui, prefix + ".sort", slot)) {
      SortMode mode = GuiSession.cycleSort(player.getUniqueId());
      Map<String, String> ph = new HashMap<String, String>();
      ph.put("{sort}", mode.label());
      this.plugin.sendMessage(player, "sort-changed", ph);
      this.plugin.getGuiManager().openPlayer(player, holder.getPage());
      this.plugin.playConfiguredSound(player, "sounds.click");
      return;
    }
    if (this.isButton(gui, prefix + ".filter", slot)) {
      List<String> categories = this.collectCategories();
      String category = GuiSession.cycleCategory(player.getUniqueId(), categories);
      Map<String, String> ph = new HashMap<String, String>();
      ph.put(
          "{filter}",
          category.isEmpty() ? this.plugin.msg("filter-all-display") : category.toUpperCase(Locale.ROOT));
      this.plugin.sendMessage(player, "filter-changed", ph);
      this.plugin.getGuiManager().openPlayer(player, holder.getPage());
      this.plugin.playConfiguredSound(player, "sounds.click");
      return;
    }
    if (this.isButton(gui, prefix + ".search", slot)) {
      this.plugin.getChatInputListener().requestInput(player, ChatInputType.SEARCH_PLAYER, null);
      return;
    }
    if (this.isButton(gui, prefix + ".favorites", slot)) {
      this.plugin.getGuiManager().openFavorites(player);
      this.plugin.playConfiguredSound(player, "sounds.click");
      return;
    }
    if (this.isButton(gui, prefix + ".stats", slot)) {
      this.plugin.getGuiManager().openStats(player);
      this.plugin.playConfiguredSound(player, "sounds.click");
      return;
    }
    if (this.isButton(gui, prefix + ".settings", slot)) {
      this.plugin.getGuiManager().openSettings(player);
      this.plugin.playConfiguredSound(player, "sounds.click");
      return;
    }
    if (this.isButton(gui, prefix + ".top", slot)) {
      this.plugin.getGuiManager().openTop(player);
      this.plugin.playConfiguredSound(player, "sounds.click");
      return;
    }
    if (this.isButton(gui, prefix + ".history", slot)) {
      this.plugin.getGuiManager().openHistory(player);
      this.plugin.playConfiguredSound(player, "sounds.click");
      return;
    }
    if (this.isButton(gui, prefix + ".clear-search", slot)) {
      GuiSession.setSearch(player.getUniqueId(), "");
      this.plugin.sendMessage(player, "search-cleared");
      this.plugin.getGuiManager().openPlayer(player, holder.getPage());
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
    if (middle) {
      this.toggleFavorite(player, kit);
      return;
    }
    if (right && !shift) {
      this.plugin.getGuiManager().openPreview(player, kit, GuiType.PLAYER);
      return;
    }
    if (shift) {
      this.plugin.getGuiManager().openPreview(player, kit, GuiType.PLAYER);
      return;
    }
    if (!this.plugin.hasKitPermission(player, kit)) {
      this.plugin.sendMessage(player, "no-permission");
      this.plugin.playConfiguredSound(player, "sounds.error");
      return;
    }
    this.plugin.giveKit(player, kit, false);
  }

  private void handleAdminClick(
      Player player,
      GuiHolder holder,
      int slot,
      boolean shift,
      boolean right,
      boolean middle,
      boolean left) {
    if (!player.hasPermission("fortcode.kits.admin")) {
      this.plugin.sendMessage(player, "no-permission");
      return;
    }
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    String prefix = "admin.footer";

    if (this.isButton(gui, "admin.actions.create", slot)) {
      this.plugin.getChatInputListener().requestInput(player, ChatInputType.CREATE_KIT, null);
      return;
    }
    if (this.isButton(gui, prefix + ".prev", slot)) {
      this.plugin.getGuiManager().openAdmin(player, Math.max(1, holder.getPage() - 1));
      return;
    }
    if (this.isButton(gui, prefix + ".next", slot)) {
      this.plugin.getGuiManager().openAdmin(player, holder.getPage() + 1);
      return;
    }
    if (this.isButton(gui, prefix + ".sort", slot)) {
      SortMode mode = GuiSession.cycleSort(player.getUniqueId());
      Map<String, String> ph = new HashMap<String, String>();
      ph.put("{sort}", mode.label());
      this.plugin.sendMessage(player, "sort-changed", ph);
      this.plugin.getGuiManager().openAdmin(player, holder.getPage());
      return;
    }
    if (this.isButton(gui, prefix + ".filter", slot)) {
      List<String> categories = this.collectCategories();
      String category = GuiSession.cycleCategory(player.getUniqueId(), categories);
      Map<String, String> ph = new HashMap<String, String>();
      ph.put(
          "{filter}",
          category.isEmpty() ? this.plugin.msg("filter-all-display") : category.toUpperCase(Locale.ROOT));
      this.plugin.sendMessage(player, "filter-changed", ph);
      this.plugin.getGuiManager().openAdmin(player, holder.getPage());
      return;
    }
    if (this.isButton(gui, prefix + ".search", slot)) {
      this.plugin.getChatInputListener().requestInput(player, ChatInputType.SEARCH_ADMIN, null);
      return;
    }
    if (this.isButton(gui, prefix + ".clear-search", slot)) {
      GuiSession.setSearch(player.getUniqueId(), "");
      this.plugin.sendMessage(player, "search-cleared");
      this.plugin.getGuiManager().openAdmin(player, holder.getPage());
      return;
    }
    if (this.isButton(gui, prefix + ".history", slot)) {
      this.plugin.getGuiManager().openHistory(player);
      return;
    }
    if (this.isButton(gui, prefix + ".stats", slot)) {
      this.plugin.getGuiManager().openStats(player);
      return;
    }
    if (this.isButton(gui, prefix + ".top", slot)) {
      this.plugin.getGuiManager().openTop(player);
      return;
    }
    if (this.isButton(gui, prefix + ".reload", slot)) {
      if (!player.hasPermission("fortcode.kits.reload")) {
        this.plugin.sendMessage(player, "no-permission");
        return;
      }
      this.plugin.reloadPlugin();
      this.plugin.sendMessage(player, "reload");
      this.plugin.getGuiManager().openAdmin(player, holder.getPage());
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
    if (right && shift) {
      Map<String, String> ph = new HashMap<String, String>();
      ph.put("{kit}", kit.getDisplayName());
      this.plugin.getGuiManager().openConfirm(
          player, "delete-kit", kit.getId(), GuiType.ADMIN, this.plugin.msg("confirm-delete-kit", ph));
      return;
    }
    if (shift && !right) {
      kit.setItems(this.plugin.collectInventoryItems(player));
      this.plugin.getKitManager().save();
      this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
      this.plugin.getGuiManager().refreshOpenGuis(kit);
      this.plugin.getGuiManager().openAdmin(player, holder.getPage());
      return;
    }
    if (right) {
      this.plugin.getGuiManager().openEditor(player, kit);
      return;
    }
    if (middle) {
      this.toggleFavorite(player, kit);
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
    switch (holder.getReturnType() == null ? GuiType.PLAYER : holder.getReturnType()) {
      case ADMIN -> this.plugin.getGuiManager().openAdmin(player);
      case EDITOR -> {
        if (holder.getKitId() == null) {
          this.plugin.getGuiManager().openAdmin(player);
          return;
        }
        this.plugin
            .getKitManager()
            .getKit(holder.getKitId())
            .ifPresent(kit -> this.plugin.getGuiManager().openEditor(player, kit));
      }
      case FAVORITES -> this.plugin.getGuiManager().openFavorites(player, holder.getPage());
      case HISTORY -> this.plugin.getGuiManager().openHistory(player, holder.getPage());
      case STATS -> this.plugin.getGuiManager().openStats(player, holder.getPage());
      case TOP -> this.plugin.getGuiManager().openTop(player);
      case RANK -> {
        if (holder.getKitId() != null) {
          this.plugin
              .getKitManager()
              .getKit(holder.getKitId())
              .ifPresent(kit -> this.plugin.getGuiManager().openRank(player, kit));
        }
      }
      case FEEDBACK -> {
        if (holder.getKitId() != null) {
          this.plugin
              .getKitManager()
              .getKit(holder.getKitId())
              .ifPresent(kit -> this.plugin.getGuiManager().openFeedback(player, kit));
        }
      }
      default -> this.plugin.getGuiManager().openPlayer(player, holder.getPage());
    }
  }

  private void handleEditorClick(Player player, GuiHolder holder, int slot) {
    if (!player.hasPermission("fortcode.kits.admin")) {
      this.plugin.sendMessage(player, "no-permission");
      return;
    }
    EditorAction action = holder.getEditorActions().get(slot);
    if (action == null || holder.getKitId() == null) {
      return;
    }
    Optional<Kit> opt = this.plugin.getKitManager().getKit(holder.getKitId());
    if (opt.isEmpty()) {
      return;
    }
    Kit kit = opt.get();
    switch (action) {
      case SET_ITEMS -> {
        kit.setItems(this.plugin.collectInventoryItems(player));
        this.plugin.getKitManager().save();
        this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
        this.plugin.getGuiManager().openEditor(player, kit);
        this.plugin.getGuiManager().refreshOpenGuis(kit);
      }
      case SET_ICON -> {
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
      }
      case SET_NAME ->
          this.plugin.getChatInputListener().requestInput(player, ChatInputType.SET_NAME, kit.getId());
      case SET_COOLDOWN ->
          this.plugin
              .getChatInputListener()
              .requestInput(player, ChatInputType.SET_COOLDOWN, kit.getId());
      case SET_SLOT ->
          this.plugin.getChatInputListener().requestInput(player, ChatInputType.SET_SLOT, kit.getId());
      case SET_PRICE ->
          this.plugin.getChatInputListener().requestInput(player, ChatInputType.SET_PRICE, kit.getId());
      case SET_CATEGORY ->
          this.plugin
              .getChatInputListener()
              .requestInput(player, ChatInputType.SET_CATEGORY, kit.getId());
      case SET_RANK -> this.plugin.getGuiManager().openRank(player, kit);
      case SET_FEEDBACK_TYPE -> this.plugin.getGuiManager().openFeedback(player, kit);
      case TOGGLE_ONE_TIME -> {
        kit.setOneTime(!kit.isOneTime());
        this.finishToggle(player, kit);
      }
      case TOGGLE_RARITY -> {
        kit.setRarity(KitRarity.next(kit.getRarity()));
        this.finishToggle(player, kit);
      }
      case TOGGLE_ENABLED -> {
        kit.setEnabled(!kit.isEnabled());
        this.finishToggle(player, kit);
      }
      case TOGGLE_HIDDEN -> {
        kit.setHidden(!kit.isHidden());
        this.finishToggle(player, kit);
      }
      case TOGGLE_WELCOME -> {
        kit.setWelcome(!kit.isWelcome());
        this.finishToggle(player, kit);
      }
      case DUPLICATE -> {
        String base = kit.getId() + "_copy";
        String newId = base;
        int index = 2;
        while (this.plugin.getKitManager().getKit(newId).isPresent()) {
          newId = base + index;
          index++;
        }
        Kit copy = kit.duplicate(newId);
        this.plugin.getKitManager().upsert(copy);
        this.plugin.getKitManager().save();
        Map<String, String> ph = new HashMap<String, String>();
        ph.put("{kit}", copy.getId());
        this.plugin.sendMessage(player, "kit-duplicated", ph);
        this.plugin.getGuiManager().openEditor(player, copy);
        this.plugin.getGuiManager().refreshOpenGuis(copy);
      }
      case RESET_KIT_COOLDOWNS -> {
        Map<String, String> ph = new HashMap<String, String>();
        ph.put("{kit}", kit.getDisplayName());
        this.plugin.getGuiManager().openConfirm(
            player,
            "reset-kit-cooldowns",
            kit.getId(),
            GuiType.EDITOR,
            this.plugin.msg("confirm-reset-kit", ph));
      }
      case VIEW_KIT_STATS -> {
        Map<String, String> ph = new HashMap<String, String>();
        ph.put("{kit}", kit.getDisplayName());
        ph.put(
            "{claims}",
            String.valueOf(this.plugin.getStatsManager().getKitClaims(kit.getId())));
        this.plugin.sendMessage(player, "kit-stats-line", ph);
      }
      case PREVIEW -> this.plugin.getGuiManager().openPreview(player, kit, GuiType.EDITOR);
      case DELETE -> {
        Map<String, String> ph = new HashMap<String, String>();
        ph.put("{kit}", kit.getDisplayName());
        this.plugin.getGuiManager().openConfirm(
            player, "delete-kit", kit.getId(), GuiType.EDITOR, this.plugin.msg("confirm-delete-kit", ph));
      }
      case BACK -> this.plugin.getGuiManager().openAdmin(player);
    }
  }

  private void finishToggle(Player player, Kit kit) {
    this.plugin.getKitManager().save();
    this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
    this.plugin.getGuiManager().openEditor(player, kit);
    this.plugin.getGuiManager().refreshOpenGuis(kit);
    this.plugin.playConfiguredSound(player, "sounds.click");
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
    if (slot == gui.getConfig().getInt("rank.back.slot", -1)) {
      this.plugin.getGuiManager().openEditor(player, kit);
      return;
    }
    if (slot == gui.getConfig().getInt("rank.none.slot", -1)) {
      kit.setRequiredGroup(null);
      this.plugin.getKitManager().save();
      this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
      this.plugin.getGuiManager().openEditor(player, kit);
      this.plugin.getGuiManager().refreshOpenGuis(kit);
      return;
    }
    if (slot == gui.getConfig().getInt("rank.navigation.prev.slot", -1)) {
      this.plugin.getGuiManager().openRank(player, kit, Math.max(1, holder.getPage() - 1));
      return;
    }
    if (slot == gui.getConfig().getInt("rank.navigation.next.slot", -1)) {
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
    if (slot == gui.getConfig().getInt("feedback.back.slot", -1)) {
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

  private void handleSettingsClick(Player player, GuiHolder holder, int slot) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    String key = holder.getKitSlots().get(slot);
    if (key != null) {
      boolean value = this.plugin.getPlayerSettingsManager().toggle(player.getUniqueId(), key);
      Map<String, String> ph = new HashMap<String, String>();
      ph.put("{setting}", key.toUpperCase(Locale.ROOT));
      ph.put(
          "{value}",
          value
              ? this.plugin.msg("settings-on-display")
              : this.plugin.msg("settings-off-display"));
      this.plugin.sendMessage(player, "setting-toggled", ph);
      this.plugin.getGuiManager().openSettings(player);
      if (value) {
        this.plugin.playConfiguredSound(player, "sounds.click");
      }
      return;
    }
    if (this.isButton(gui, "settings.actions.reset", slot)) {
      this.plugin.getPlayerSettingsManager().reset(player.getUniqueId());
      this.plugin.sendMessage(player, "settings-reset");
      this.plugin.getGuiManager().openSettings(player);
      return;
    }
    if (this.isButton(gui, "settings.actions.back", slot)) {
      this.plugin.getGuiManager().openPlayer(player);
    }
  }

  private void handleStatsClick(Player player, GuiHolder holder, int slot) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    if (this.isButton(gui, "stats.navigation.prev", slot)) {
      this.plugin.getGuiManager().openStats(player, Math.max(1, holder.getPage() - 1));
      return;
    }
    if (this.isButton(gui, "stats.navigation.next", slot)) {
      this.plugin.getGuiManager().openStats(player, holder.getPage() + 1);
      return;
    }
    if (this.isButton(gui, "stats.actions.history", slot)) {
      this.plugin.getGuiManager().openHistory(player);
      return;
    }
    if (this.isButton(gui, "stats.actions.top", slot)) {
      this.plugin.getGuiManager().openTop(player);
      return;
    }
    if (this.isButton(gui, "stats.actions.back", slot)) {
      this.plugin.getGuiManager().openPlayer(player);
      return;
    }
    String kitId = holder.getKitSlots().get(slot);
    if (kitId != null) {
      this.plugin
          .getKitManager()
          .getKit(kitId)
          .ifPresent(kit -> this.plugin.getGuiManager().openPreview(player, kit, GuiType.STATS));
    }
  }

  private void handleHistoryClick(Player player, GuiHolder holder, int slot, boolean shift) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    if (this.isButton(gui, "history.navigation.prev", slot)) {
      this.plugin.getGuiManager().openHistory(player, Math.max(1, holder.getPage() - 1));
      return;
    }
    if (this.isButton(gui, "history.navigation.next", slot)) {
      this.plugin.getGuiManager().openHistory(player, holder.getPage() + 1);
      return;
    }
    if (this.isButton(gui, "history.actions.clear", slot)) {
      if (!player.hasPermission("fortcode.kits.history.all")) {
        this.plugin.sendMessage(player, "no-permission");
        return;
      }
      this.plugin.getGuiManager().openConfirm(
          player,
          "clear-history",
          null,
          GuiType.HISTORY,
          this.plugin.msg("confirm-clear-history"));
      return;
    }
    if (this.isButton(gui, "history.actions.back", slot)) {
      if (player.hasPermission("fortcode.kits.admin")) {
        this.plugin.getGuiManager().openAdmin(player);
      } else {
        this.plugin.getGuiManager().openPlayer(player);
      }
      return;
    }
    String kitId = holder.getKitSlots().get(slot);
    if (kitId != null && !shift) {
      this.plugin
          .getKitManager()
          .getKit(kitId)
          .ifPresent(kit -> this.plugin.getGuiManager().openPreview(player, kit, GuiType.HISTORY));
    }
  }

  private void handleFavoritesClick(
      Player player, GuiHolder holder, int slot, boolean shift, boolean right) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    if (this.isButton(gui, "favorites.navigation.prev", slot)) {
      this.plugin.getGuiManager().openFavorites(player, Math.max(1, holder.getPage() - 1));
      return;
    }
    if (this.isButton(gui, "favorites.navigation.next", slot)) {
      this.plugin.getGuiManager().openFavorites(player, holder.getPage() + 1);
      return;
    }
    if (this.isButton(gui, "favorites.actions.back", slot)) {
      this.plugin.getGuiManager().openPlayer(player);
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
    if (right || shift) {
      this.plugin.getFavoriteManager().remove(player.getUniqueId(), kit.getId());
      this.plugin.getFavoriteManager().save();
      Map<String, String> ph = new HashMap<String, String>();
      ph.put("{kit}", kit.getDisplayName());
      this.plugin.sendMessage(player, "favorite-removed", ph);
      this.plugin.getGuiManager().openFavorites(player, holder.getPage());
      this.plugin.playConfiguredSound(player, "sounds.click");
      return;
    }
    if (!this.plugin.hasKitPermission(player, kit)) {
      this.plugin.sendMessage(player, "no-permission");
      this.plugin.playConfiguredSound(player, "sounds.error");
      return;
    }
    this.plugin.giveKit(player, kit, false);
  }

  private void handleTopClick(Player player, GuiHolder holder, int slot) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    if (this.isButton(gui, "top.actions.refresh", slot)) {
      this.plugin.getGuiManager().openTop(player);
      return;
    }
    if (this.isButton(gui, "top.actions.back", slot)) {
      if (player.hasPermission("fortcode.kits.admin")) {
        this.plugin.getGuiManager().openAdmin(player);
      } else {
        this.plugin.getGuiManager().openPlayer(player);
      }
      return;
    }
    String kitId = holder.getKitSlots().get(slot);
    if (kitId != null) {
      this.plugin
          .getKitManager()
          .getKit(kitId)
          .ifPresent(kit -> this.plugin.getGuiManager().openPreview(player, kit, GuiType.TOP));
    }
  }

  private void handleConfirmClick(Player player, GuiHolder holder, int slot, boolean left) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    boolean yes = this.isButton(gui, "confirm.actions.yes", slot);
    boolean no = this.isButton(gui, "confirm.actions.no", slot);
    if (!yes && !no) {
      return;
    }
    String action = holder.getConfirmAction();
    GuiType returnType = holder.getReturnType() == null ? GuiType.PLAYER : holder.getReturnType();
    if (no) {
      this.returnFromConfirm(player, returnType, holder.getKitId());
      return;
    }
    if (action == null) {
      this.returnFromConfirm(player, returnType, holder.getKitId());
      return;
    }
    switch (action) {
      case "delete-kit" -> {
        String kitId = holder.getKitId();
        if (kitId == null) {
          this.returnFromConfirm(player, returnType, null);
          return;
        }
        Optional<Kit> opt = this.plugin.getKitManager().getKit(kitId);
        if (opt.isPresent()) {
          Kit kit = opt.get();
          this.plugin.getKitManager().remove(kit.getId());
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-deleted", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuisAfterDelete(kit.getId());
        }
        this.plugin.getGuiManager().openAdmin(player);
        this.plugin.playConfiguredSound(player, "sounds.click");
      }
      case "reset-kit-cooldowns" -> {
        String kitId = holder.getKitId();
        if (kitId == null) {
          this.returnFromConfirm(player, returnType, null);
          return;
        }
        this.plugin.getCooldownManager().resetAllForKit(kitId);
        Map<String, String> ph = new HashMap<String, String>();
        ph.put("{kit}", kitId);
        this.plugin.sendMessage(player, "kit-cooldowns-reset", ph);
        this.returnFromConfirm(player, returnType, kitId);
        this.plugin.playConfiguredSound(player, "sounds.click");
      }
      case "clear-history" -> {
        this.plugin.getStatsManager().clearHistory();
        this.plugin.getStatsManager().save();
        this.plugin.sendMessage(player, "history-cleared");
        this.plugin.getGuiManager().openHistory(player);
        this.plugin.playConfiguredSound(player, "sounds.click");
      }
      case "reset-stats" -> {
        this.plugin.getStatsManager().resetPlayer(player.getUniqueId());
        this.plugin.getStatsManager().save();
        this.plugin.sendMessage(player, "stats-reset");
        this.plugin.getGuiManager().openStats(player);
        this.plugin.playConfiguredSound(player, "sounds.click");
      }
      default -> this.returnFromConfirm(player, returnType, holder.getKitId());
    }
  }

  private void returnFromConfirm(Player player, GuiType returnType, String kitId) {
    switch (returnType) {
      case ADMIN -> this.plugin.getGuiManager().openAdmin(player);
      case EDITOR -> {
        if (kitId != null) {
          this.plugin
              .getKitManager()
              .getKit(kitId)
              .ifPresent(kit -> this.plugin.getGuiManager().openEditor(player, kit));
        } else {
          this.plugin.getGuiManager().openAdmin(player);
        }
      }
      case HISTORY -> this.plugin.getGuiManager().openHistory(player);
      case STATS -> this.plugin.getGuiManager().openStats(player);
      default -> this.plugin.getGuiManager().openPlayer(player);
    }
  }

  private void toggleFavorite(Player player, Kit kit) {
    boolean added = this.plugin.getFavoriteManager().toggle(player.getUniqueId(), kit.getId());
    this.plugin.getFavoriteManager().save();
    Map<String, String> ph = new HashMap<String, String>();
    ph.put("{kit}", kit.getDisplayName());
    this.plugin.sendMessage(player, added ? "favorite-added" : "favorite-removed", ph);
    this.plugin.playConfiguredSound(player, "sounds.click");
    this.plugin.getGuiManager().refreshPlayerIfOpen(player);
  }

  private List<String> collectCategories() {
    Set<String> categories = new LinkedHashSet<String>();
    categories.add("all");
    for (Kit kit : this.plugin.getKitManager().getKits()) {
      if (kit.getCategory() != null && !kit.getCategory().isBlank()) {
        categories.add(kit.getCategory().toLowerCase(Locale.ROOT));
      }
    }
    return new ArrayList<String>(categories);
  }

  private boolean isButton(GuiConfig gui, String path, int slot) {
    int expected = gui.getConfig().getInt(path + ".slot", -1);
    return expected >= 0 && expected == slot;
  }

  private boolean isSpam(UUID uuid) {
    long now = System.currentTimeMillis();
    long prev = this.antiSpam.getOrDefault(uuid, 0L);
    this.antiSpam.put(uuid, now);
    return now - prev < 150L;
  }
}
