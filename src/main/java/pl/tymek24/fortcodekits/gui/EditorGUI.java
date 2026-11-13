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
import pl.tymek24.fortcodekits.model.Kit;

public class EditorGUI extends AbstractGui {
  public EditorGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player, Kit kit) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("editor.size", 54);
    Map<String, String> ph = this.buildPlaceholders(kit);
    String title = this.title("editor.title", "&8EDYTOR: {kit_display}", ph);
    HashMap<Integer, EditorAction> actions = new HashMap<Integer, EditorAction>();
    GuiHolder holder = new GuiHolder(GuiType.EDITOR, null, actions, kit.getId(), 0, GuiType.ADMIN);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "editor.filler");
    this.placeKitInfo(inv, kit, ph);
    this.placeActions(inv, kit, ph, actions);
    player.openInventory(inv);
    this.openSound(player);
  }

  public void refresh(Player player, GuiHolder holder) {
    if (holder == null || holder.getType() != GuiType.EDITOR || holder.getKitId() == null) {
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
    Map<String, String> ph = this.buildPlaceholders(kit);
    this.placeKitInfo(inv, kit, ph);
    this.placeActions(inv, kit, ph, holder.getEditorActions());
  }

  private void placeKitInfo(Inventory inv, Kit kit, Map<String, String> ph) {
    ConfigurationSection section = this.cfg().getConfig().getConfigurationSection("editor.kit-item");
    if (section == null) {
      return;
    }
    int slot = section.getInt("slot", -1);
    if (slot < 0 || slot >= inv.getSize()) {
      return;
    }
    ItemStack item = this.cfg().applyTemplate(kit.getIcon(), section, ph);
    inv.setItem(slot, item);
  }

  private void placeActions(
      Inventory inv, Kit kit, Map<String, String> ph, Map<Integer, EditorAction> actions) {
    ConfigurationSection root = this.cfg().getConfig().getConfigurationSection("editor.actions");
    if (root == null) {
      return;
    }
    for (String key : root.getKeys(false)) {
      ConfigurationSection section = root.getConfigurationSection(key);
      if (section == null) continue;
      int slot = section.getInt("slot", -1);
      if (slot < 0 || slot >= inv.getSize()) continue;
      EditorAction action = this.mapAction(key);
      if (action == null) continue;
      ItemStack item = this.sectionItem(section, ph);
      if (item == null) continue;
      inv.setItem(slot, item);
      actions.put(slot, action);
    }
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
      case "set-price" -> EditorAction.SET_PRICE;
      case "set-category" -> EditorAction.SET_CATEGORY;
      case "toggle-one-time" -> EditorAction.TOGGLE_ONE_TIME;
      case "toggle-rarity" -> EditorAction.TOGGLE_RARITY;
      case "toggle-enabled" -> EditorAction.TOGGLE_ENABLED;
      case "toggle-hidden" -> EditorAction.TOGGLE_HIDDEN;
      case "toggle-welcome" -> EditorAction.TOGGLE_WELCOME;
      case "duplicate" -> EditorAction.DUPLICATE;
      case "reset-cooldowns" -> EditorAction.RESET_KIT_COOLDOWNS;
      case "kit-stats" -> EditorAction.VIEW_KIT_STATS;
      case "preview" -> EditorAction.PREVIEW;
      case "delete" -> EditorAction.DELETE;
      case "back" -> EditorAction.BACK;
      default -> null;
    };
  }

  public Map<String, String> buildPlaceholders(Kit kit) {
    Map<String, String> ph = this.ph();
    ph.put("{kit_id}", kit.getId());
    ph.put("{kit_display}", kit.getDisplayName());
    ph.put("{cooldown}", pl.tymek24.fortcodekits.util.TimeUtil.formatDuration(kit.getCooldownSeconds()));
    ph.put("{status}", "");
    ph.put("{slot}", kit.getGuiSlot() == null ? "-" : String.valueOf(kit.getGuiSlot()));
    ph.put("{items}", String.valueOf(kit.getItems().size()));
    ph.put(
        "{one_time}",
        kit.isOneTime()
            ? this.text("text.one-time-yes", "&aTAK")
            : this.text("text.one-time-no", "&cNIE"));
    ph.put("{price}", this.plugin.formatPrice(kit.getPrice()));
    ph.put(
        "{rarity}",
        kit.getRarity() == null
            ? this.text("text.rarity-none", "&7BRAK")
            : kit.getRarity().display());
    ph.put(
        "{rarity_key}",
        kit.getRarity() == null ? "none" : kit.getRarity().key());
    ph.put(
        "{category}",
        kit.getCategory() == null
            ? this.text("text.category-default", "&7DOMYSLNA")
            : kit.getCategory().toUpperCase(java.util.Locale.ROOT));
    ph.put(
        "{enabled}",
        kit.isEnabled() ? this.text("text.yes", "&aTAK") : this.text("text.no", "&cNIE"));
    ph.put(
        "{hidden}",
        kit.isHidden() ? this.text("text.yes", "&aTAK") : this.text("text.no", "&cNIE"));
    ph.put(
        "{welcome}",
        kit.isWelcome() ? this.text("text.yes", "&aTAK") : this.text("text.no", "&cNIE"));
    ph.put(
        "{claim_limit}",
        kit.getClaimCountLimit() == null
            ? this.text("text.cooldown-ready", "&7BRAK")
            : String.valueOf(kit.getClaimCountLimit()));
    ph.put("{claims}", String.valueOf(this.plugin.getStatsManager().getKitClaims(kit.getId())));
    String rank = kit.getRequiredGroup();
    String rankNone = this.text("text.rank-none", "&7BRAK");
    if (rank == null || rank.isBlank()) {
      ph.put("{rank}", rankNone);
      ph.put("{rank_prefix}", this.text("text.rank-prefix-none", "&7-"));
      ph.put("{rank_display}", rankNone);
    } else {
      String prefix =
          pl.tymek24.fortcodekits.integration.LuckPermsHook.getGroupPrefix(rank);
      String display = prefix.isEmpty() ? this.text("text.rank-prefix-none", "&7-") : prefix;
      ph.put("{rank}", rank);
      ph.put("{rank_prefix}", display);
      ph.put("{rank_display}", display);
    }
    String type = kit.getFeedbackType();
    ph.put("{feedback_type}", type == null || type.isBlank() ? "default" : type);
    ph.put(
        "{feedback_type_display}",
        type == null || type.isBlank()
            ? this.text("text.feedback-default", "&7DOMYSLNY")
            : type.toUpperCase(java.util.Locale.ROOT));
    return ph;
  }
}
