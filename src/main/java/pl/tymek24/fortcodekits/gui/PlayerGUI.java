package pl.tymek24.fortcodekits.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

public class PlayerGUI {
  private final FortCodeKits plugin;

  public PlayerGUI(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  public void open(Player player) {
    this.open(player, 1);
  }

  public void open(Player player, int page) {
    int pages;
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    int size = gui.getSize("player.size", 54);
    String rawTitle = gui.getConfig().getString("player.title", "&8Kity");
    String title = this.plugin.formatText(rawTitle);
    HashMap<Integer, String> slotToKit = new HashMap<Integer, String>();
    GuiHolder holder = new GuiHolder(GuiType.PLAYER, slotToKit, null, null, page, null);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, (int) size, (String) title);
    holder.setInventory(inv);
    this.applyFiller(inv, gui, "player.filler");
    ArrayList<Kit> kits = new ArrayList<Kit>(this.plugin.getKitManager().getKits());
    HashSet<String> fixed = new HashSet<String>();
    List<Integer> kitSlots = gui.getSlots("player.kit-slots");
    ConfigurationSection kitItemSection =
        gui.getConfig().getConfigurationSection("player.kit-item");
    for (Kit kit : kits) {
      Integer n = kit.getGuiSlot();
      if (n == null || n < 0 || n >= size) continue;
      ItemStack item = this.buildKitItem(player, kit, kitItemSection, gui);
      inv.setItem(n.intValue(), item);
      slotToKit.put(n, kit.getId());
      fixed.add(kit.getId());
    }
    ArrayList<Kit> dynamic = new ArrayList<Kit>();
    for (Kit kit : kits) {
      if (fixed.contains(kit.getId())) continue;
      dynamic.add(kit);
    }
    ArrayList<Integer> arrayList = new ArrayList<Integer>();
    for (int slot : kitSlots) {
      if (slot < 0 || slot >= size || slotToKit.containsKey(slot)) continue;
      arrayList.add(slot);
    }
    int n = arrayList.size();
    int n2 = pages = n == 0 ? 1 : (int) Math.ceil((double) dynamic.size() / (double) n);
    if (pages <= 0) {
      pages = 1;
    }
    int safePage = Math.max(1, Math.min(page, pages));
    int start = (safePage - 1) * n;
    int end = Math.min(dynamic.size(), start + n);
    int slotIndex = 0;
    for (int i = start; i < end && slotIndex < arrayList.size(); ++i) {
      int slot = (Integer) arrayList.get(slotIndex++);
      Kit kit4 = (Kit) dynamic.get(i);
      ItemStack item = this.buildKitItem(player, kit4, kitItemSection, gui);
      inv.setItem(slot, item);
      slotToKit.put(slot, kit4.getId());
    }
    this.placeNavigation(inv, gui, "player.navigation", safePage, pages);
    player.openInventory(inv);
    this.plugin.playConfiguredSound(player, "sounds.open");
  }

  public void refresh(Player player, GuiHolder holder) {
    if (holder == null || holder.getType() != GuiType.PLAYER) {
      return;
    }
    Inventory inv = holder.getInventory();
    if (inv == null) {
      return;
    }
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    ConfigurationSection kitItemSection =
        gui.getConfig().getConfigurationSection("player.kit-item");
    for (Map.Entry<Integer, String> entry : holder.getKitSlots().entrySet()) {
      int slot = entry.getKey();
      String kitId = entry.getValue();
      Kit kit = this.plugin.getKitManager().getKit(kitId).orElse(null);
      if (kit == null) {
        inv.setItem(slot, null);
        continue;
      }
      ItemStack item = this.buildKitItem(player, kit, kitItemSection, gui);
      inv.setItem(slot, item);
    }
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

  private ItemStack buildKitItem(
      Player player, Kit kit, ConfigurationSection kitItemSection, GuiConfig gui) {
    HashMap<String, String> placeholders = new HashMap<String, String>();
    long remaining =
        this.plugin.getCooldownManager().getRemaining(player.getUniqueId(), kit.getId());
    boolean claimed =
        this.plugin.getCooldownManager().hasClaimed(player.getUniqueId(), kit.getId());
    boolean hasPermission = this.plugin.hasKitPermission(player, kit);
    String status =
        !hasPermission
            ? gui.getConfig().getString("text.status-no-permission", "&cBrak permisji")
            : (kit.isOneTime() && claimed
                ? gui.getConfig().getString("text.status-one-time", "&cJednorazowy")
                : (remaining > 0L
                    ? gui.getConfig().getString("text.status-cooldown", "&cCooldown")
                    : gui.getConfig().getString("text.status-available", "&aDostepny")));
    String cooldownText =
        kit.isOneTime() && claimed
            ? gui.getConfig().getString("text.cooldown-one-time", "&7-")
            : (remaining > 0L
                ? TimeUtil.formatDuration(remaining)
                : (kit.getCooldownSeconds() > 0L
                    ? TimeUtil.formatDuration(kit.getCooldownSeconds())
                    : gui.getConfig().getString("text.cooldown-ready", "&7Brak")));
    placeholders.put("{kit_id}", kit.getId());
    placeholders.put("{kit_display}", kit.getDisplayName());
    placeholders.put("{cooldown}", cooldownText);
    placeholders.put("{status}", status);
    placeholders.put("{slot}", kit.getGuiSlot() == null ? "-" : String.valueOf(kit.getGuiSlot()));
    placeholders.put("{items}", String.valueOf(kit.getItems().size()));
    placeholders.put(
        "{one_time}",
        kit.isOneTime()
            ? gui.getConfig().getString("text.one-time-yes", "&aTak")
            : gui.getConfig().getString("text.one-time-no", "&cNie"));
    this.applyRankPlaceholders(gui, kit, placeholders);
    this.applyFeedbackPlaceholders(gui, kit, placeholders);
    if (kitItemSection == null) {
      return kit.getIcon();
    }
    return gui.applyTemplate(kit.getIcon(), kitItemSection, placeholders);
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

  private void placeNavigation(Inventory inv, GuiConfig gui, String path, int page, int pages) {
    ItemStack next;
    ItemStack prev;
    HashMap<String, String> placeholders = new HashMap<String, String>();
    placeholders.put("{page}", String.valueOf(page));
    placeholders.put("{pages}", String.valueOf(pages));
    int prevSlot = gui.getConfig().getInt(path + ".prev.slot", -1);
    int nextSlot = gui.getConfig().getInt(path + ".next.slot", -1);
    if (page > 1
        && prevSlot >= 0
        && prevSlot < inv.getSize()
        && (prev = gui.buildItem(path + ".prev.item", placeholders)) != null) {
      inv.setItem(prevSlot, prev);
    }
    if (page < pages
        && nextSlot >= 0
        && nextSlot < inv.getSize()
        && (next = gui.buildItem(path + ".next.item", placeholders)) != null) {
      inv.setItem(nextSlot, next);
    }
  }
}
