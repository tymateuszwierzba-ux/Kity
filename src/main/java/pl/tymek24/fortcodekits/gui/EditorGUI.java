package pl.tymek24.fortcodekits.gui;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.integration.LuckPermsHook;
import pl.tymek24.fortcodekits.model.Kit;
import pl.tymek24.fortcodekits.util.TimeUtil;

public class EditorGUI {
  private final FortCodeKits plugin;

  public EditorGUI(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  public void open(Player player, Kit kit) {
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    int size = gui.getSize("editor.size", 54);
    String rawTitle = gui.getConfig().getString("editor.title", "&6Edytor: {kit_display}");
    String title = this.plugin.formatText(rawTitle.replace("{kit_display}", kit.getDisplayName()));
    HashMap<Integer, EditorAction> actions = new HashMap<Integer, EditorAction>();
    GuiHolder holder = new GuiHolder(GuiType.EDITOR, null, actions, kit.getId(), 0, GuiType.ADMIN);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, (int) size, (String) title);
    holder.setInventory(inv);
    this.applyFiller(inv, gui, "editor.filler");
    this.placeKitInfo(inv, gui, kit);
    this.placeActions(inv, gui, kit, actions);
    player.openInventory(inv);
    this.plugin.playConfiguredSound(player, "sounds.open");
  }

  public void refresh(Player player, GuiHolder holder) {
    if (holder == null || holder.getType() != GuiType.EDITOR) {
      return;
    }
    if (holder.getKitId() == null) {
      return;
    }
    Inventory inv = holder.getInventory();
    if (inv == null) {
      return;
    }
    Kit kit = this.plugin.getKitManager().getKit(holder.getKitId()).orElse(null);
    if (kit == null) {
      return;
    }
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    this.placeKitInfo(inv, gui, kit);
    this.placeActions(inv, gui, kit, holder.getEditorActions());
  }

  private void applyFiller(Inventory inv, GuiConfig gui, String path) {
    if (!gui.getConfig().getBoolean(path + ".enabled", false)) {
      return;
    }
    ItemStack filler = gui.buildItem(path + ".item", Map.of());
    if (filler == null) {
      return;
    }
    for (int slot : gui.getSlots(path + ".slots")) {
      if (slot < 0 || slot >= inv.getSize()) continue;
      inv.setItem(slot, filler);
    }
  }

  private void placeKitInfo(Inventory inv, GuiConfig gui, Kit kit) {
    ConfigurationSection section = gui.getConfig().getConfigurationSection("editor.kit-item");
    if (section == null) {
      return;
    }
    int slot = section.getInt("slot", -1);
    if (slot < 0 || slot >= inv.getSize()) {
      return;
    }
    ItemStack item = gui.applyTemplate(kit.getIcon(), section, this.buildPlaceholders(gui, kit));
    inv.setItem(slot, item);
  }

  private void placeActions(
      Inventory inv, GuiConfig gui, Kit kit, Map<Integer, EditorAction> actions) {
    ConfigurationSection root = gui.getConfig().getConfigurationSection("editor.actions");
    if (root == null) {
      return;
    }
    Map<String, String> placeholders = this.buildPlaceholders(gui, kit);
    for (String key : root.getKeys(false)) {
      EditorAction action;
      ItemStack item;
      int slot;
      ConfigurationSection section = root.getConfigurationSection(key);
      if (section == null
          || (slot = section.getInt("slot", -1)) < 0
          || slot >= inv.getSize()
          || (item = gui.buildItem(section.getCurrentPath() + ".item", placeholders)) == null
          || (action = this.mapAction(key)) == null) continue;
      inv.setItem(slot, item);
      actions.put(slot, action);
    }
  }

  private Map<String, String> buildPlaceholders(GuiConfig gui, Kit kit) {
    HashMap<String, String> placeholders = new HashMap<String, String>();
    placeholders.put("{kit_id}", kit.getId());
    placeholders.put("{kit_display}", kit.getDisplayName());
    placeholders.put("{cooldown}", TimeUtil.formatDuration(kit.getCooldownSeconds()));
    placeholders.put("{status}", "");
    placeholders.put("{slot}", kit.getGuiSlot() == null ? "-" : String.valueOf(kit.getGuiSlot()));
    placeholders.put("{items}", String.valueOf(kit.getItems().size()));
    placeholders.put(
        "{one_time}",
        kit.isOneTime()
            ? gui.getConfig().getString("text.one-time-yes", "&aTak")
            : gui.getConfig().getString("text.one-time-no", "&cNie"));
    this.applyRankPlaceholders(gui, kit, placeholders);
    this.applyFeedbackPlaceholders(gui, kit, placeholders);
    return placeholders;
  }

  private EditorAction mapAction(String key) {
    return switch (key.toLowerCase()) {
      case "set-items" -> EditorAction.SET_ITEMS;
      case "set-icon" -> EditorAction.SET_ICON;
      case "set-name" -> EditorAction.SET_NAME;
      case "set-cooldown" -> EditorAction.SET_COOLDOWN;
      case "set-slot" -> EditorAction.SET_SLOT;
      case "set-rank" -> EditorAction.SET_RANK;
      case "set-feedback-type" -> EditorAction.SET_FEEDBACK_TYPE;
      case "toggle-one-time" -> EditorAction.TOGGLE_ONE_TIME;
      case "preview" -> EditorAction.PREVIEW;
      case "delete" -> EditorAction.DELETE;
      case "back" -> EditorAction.BACK;
      default -> null;
    };
  }

  private void applyRankPlaceholders(GuiConfig gui, Kit kit, Map<String, String> placeholders) {
    String prefixDisplay;
    String rankNone = gui.getConfig().getString("text.rank-none", "&7Brak");
    String rank = kit.getRequiredGroup();
    if (rank == null || rank.isBlank()) {
      placeholders.put("{rank}", rankNone);
      placeholders.put("{rank_prefix}", gui.getConfig().getString("text.rank-prefix-none", "&7-"));
      placeholders.put("{rank_display}", rankNone);
      return;
    }
    String prefixRaw = LuckPermsHook.getGroupPrefix(rank);
    String display =
        prefixDisplay =
            prefixRaw.isEmpty()
                ? gui.getConfig().getString("text.rank-prefix-none", "&7-")
                : prefixRaw;
    placeholders.put("{rank}", rank);
    placeholders.put("{rank_prefix}", prefixDisplay);
    placeholders.put("{rank_display}", display);
  }

  private void applyFeedbackPlaceholders(GuiConfig gui, Kit kit, Map<String, String> placeholders) {
    String type = kit.getFeedbackType();
    String defaultText = gui.getConfig().getString("text.feedback-default", "&7Domyslny");
    String noneText = gui.getConfig().getString("text.feedback-none", "&7Brak");
    String display =
        type == null || type.isBlank()
            ? defaultText
            : (type.equalsIgnoreCase("none") ? noneText : type);
    placeholders.put("{feedback_type}", type == null || type.isBlank() ? "default" : type);
    placeholders.put("{feedback_type_display}", display);
  }
}
