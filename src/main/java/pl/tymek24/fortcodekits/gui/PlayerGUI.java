package pl.tymek24.fortcodekits.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.integration.LuckPermsHook;
import pl.tymek24.fortcodekits.model.Kit;
import pl.tymek24.fortcodekits.model.KitRarity;
import pl.tymek24.fortcodekits.model.SortMode;
import pl.tymek24.fortcodekits.util.TimeUtil;

public class PlayerGUI extends AbstractGui {
  public PlayerGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player) {
    this.open(player, 1);
  }

  public void open(Player player, int page) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("player.size", 54);
    UUID uuid = player.getUniqueId();
    String category = GuiSession.category(uuid);
    String search = GuiSession.search(uuid);
    SortMode sort = GuiSession.sort(uuid);
    Map<String, String> headerPh = this.headerPlaceholders(player, category, search, sort);
    String title = this.title("player.title", "&8KITY", headerPh);

    HashMap<Integer, String> slotToKit = new HashMap<Integer, String>();
    GuiHolder holder = new GuiHolder(GuiType.PLAYER, slotToKit, null, null, page, null);
    holder.setCategoryFilter(category);
    holder.setSearchQuery(search);
    holder.setSortMode(sort);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "player.filler");

    List<Kit> kits = this.visibleKits(player, category, search, sort, false);
    HashSet<String> fixed = new HashSet<String>();
    List<Integer> kitSlots = gui.getSlots("player.kit-slots");
    ConfigurationSection kitItemSection = gui.getConfig().getConfigurationSection("player.kit-item");
    for (Kit kit : kits) {
      Integer slot = kit.getGuiSlot();
      if (slot == null || slot < 0 || slot >= size || slotToKit.containsKey(slot)) continue;
      inv.setItem(slot, this.buildKitItem(player, kit, kitItemSection, gui));
      slotToKit.put(slot, kit.getId());
      fixed.add(kit.getId());
    }
    List<Kit> dynamic = new ArrayList<Kit>();
    for (Kit kit : kits) {
      if (fixed.contains(kit.getId())) continue;
      dynamic.add(kit);
    }
    ArrayList<Integer> freeSlots = new ArrayList<Integer>();
    for (int slot : kitSlots) {
      if (slot < 0 || slot >= size || slotToKit.containsKey(slot)) continue;
      freeSlots.add(slot);
    }
    int perPage = freeSlots.size();
    int pages = perPage == 0 ? 1 : (int) Math.ceil((double) dynamic.size() / (double) perPage);
    if (pages <= 0) {
      pages = 1;
    }
    int safePage = Math.max(1, Math.min(page, pages));
    int start = (safePage - 1) * perPage;
    int end = Math.min(dynamic.size(), start + perPage);
    int slotIndex = 0;
    for (int i = start; i < end && slotIndex < freeSlots.size(); i++) {
      int slot = freeSlots.get(slotIndex++);
      Kit kit = dynamic.get(i);
      inv.setItem(slot, this.buildKitItem(player, kit, kitItemSection, gui));
      slotToKit.put(slot, kit.getId());
    }

    headerPh.put("{page}", String.valueOf(safePage));
    headerPh.put("{pages}", String.valueOf(pages));
    headerPh.put("{visible}", String.valueOf(slotToKit.size()));
    headerPh.put("{total}", String.valueOf(kits.size()));
    this.placeHeader(inv, "player.header", headerPh);
    this.placeFooterButtons(inv, "player.footer", player, headerPh, safePage, pages);
    player.openInventory(inv);
    this.openSound(player);
  }

  public void refresh(Player player, GuiHolder holder) {
    if (holder == null || holder.getType() != GuiType.PLAYER) {
      return;
    }
    Inventory inv = holder.getInventory();
    if (inv == null) {
      return;
    }
    GuiConfig gui = this.cfg();
    ConfigurationSection kitItemSection = gui.getConfig().getConfigurationSection("player.kit-item");
    for (Map.Entry<Integer, String> entry : holder.getKitSlots().entrySet()) {
      int slot = entry.getKey();
      Kit kit = this.plugin.getKitManager().getKit(entry.getValue()).orElse(null);
      if (kit == null) {
        inv.setItem(slot, null);
        continue;
      }
      inv.setItem(slot, this.buildKitItem(player, kit, kitItemSection, gui));
    }
  }

  private void placeHeader(Inventory inv, String path, Map<String, String> placeholders) {
    ConfigurationSection section = this.cfg().getConfig().getConfigurationSection(path);
    if (section == null) {
      return;
    }
    for (String key : section.getKeys(false)) {
      ConfigurationSection button = section.getConfigurationSection(key);
      if (button == null) continue;
      int slot = button.getInt("slot", -1);
      if (slot < 0 || slot >= inv.getSize()) continue;
      ItemStack item = this.sectionItem(button, placeholders);
      if (item != null) {
        inv.setItem(slot, item);
      }
    }
  }

  private void placeFooterButtons(
      Inventory inv,
      String path,
      Player player,
      Map<String, String> placeholders,
      int page,
      int pages) {
    ConfigurationSection section = this.cfg().getConfig().getConfigurationSection(path);
    if (section == null) {
      return;
    }
    for (String key : section.getKeys(false)) {
      ConfigurationSection button = section.getConfigurationSection(key);
      if (button == null) continue;
      String enabledPath = path + "." + key + ".show";
      if (!this.cfg().getConfig().getBoolean(enabledPath, true)) continue;
      if (key.equals("clear-search") && GuiSession.search(player.getUniqueId()).isEmpty()) continue;
      if (key.equals("prev") && page <= 1) continue;
      if (key.equals("next") && page >= pages) continue;
      int slot = button.getInt("slot", -1);
      if (slot < 0 || slot >= inv.getSize()) continue;
      ItemStack item = this.sectionItem(button, placeholders);
      if (item != null) {
        inv.setItem(slot, item);
      }
    }
  }

  public List<Kit> visibleKits(
      Player player, String category, String search, SortMode sort, boolean adminView) {
    List<Kit> kits = new ArrayList<Kit>();
    String categoryKey = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
    String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
    boolean seeHidden =
        adminView
            || player.hasPermission("fortcode.kits.admin")
            || player.hasPermission("fortcode.kits.hidden");
    for (Kit kit : this.plugin.getKitManager().getKits()) {
      if (!adminView && !kit.isEnabled()) continue;
      if (!seeHidden && kit.isHidden()) continue;
      if (!categoryKey.isEmpty() && !categoryKey.equals("all")) {
        String kitCategory = kit.getCategory() == null ? "default" : kit.getCategory();
        if (!kitCategory.equalsIgnoreCase(categoryKey)) continue;
      }
      if (!query.isEmpty()) {
        String haystack =
            (kit.getId() + " " + kit.getDisplayName() + " " + (kit.getCategory() == null ? "" : kit.getCategory()))
                .toLowerCase(Locale.ROOT);
        if (!haystack.contains(query)) continue;
      }
      kits.add(kit);
    }
    this.sortKits(kits, sort, player);
    return kits;
  }

  private void sortKits(List<Kit> kits, SortMode sort, Player player) {
    if (sort == null || sort == SortMode.DEFAULT) {
      kits.sort(Comparator.comparing(Kit::getId));
      return;
    }
    switch (sort) {
      case NAME:
        kits.sort(
            Comparator.comparing(
                (Kit kit) -> stripTags(kit.getDisplayName()).toLowerCase(Locale.ROOT)));
        break;
      case COOLDOWN:
        kits.sort(Comparator.comparingLong(Kit::getCooldownSeconds));
        break;
      case STATUS:
        kits.sort(
            Comparator.comparingInt(
                (Kit kit) -> {
                  long remaining =
                      this.plugin.getCooldownManager().getRemaining(player.getUniqueId(), kit.getId());
                  boolean hasPerm = this.plugin.hasKitPermission(player, kit);
                  if (!hasPerm) return 3;
                  if (kit.isOneTime()
                      && this.plugin.getCooldownManager().hasClaimed(player.getUniqueId(), kit.getId())) {
                    return 2;
                  }
                  return remaining > 0L ? 1 : 0;
                }));
        break;
      case RARITY:
        kits.sort(
            Comparator.comparingInt(
                (Kit kit) -> kit.getRarity() == null ? -1 : kit.getRarity().ordinal()));
        break;
      default:
        kits.sort(Comparator.comparing(Kit::getId));
    }
  }

  private static String stripTags(String value) {
    if (value == null) return "";
    return value.replaceAll("(?i)<[^>]+>", "").replaceAll("&[0-9A-FK-ORa-fk-or]", "");
  }

  private Map<String, String> headerPlaceholders(
      Player player, String category, String search, SortMode sort) {
    Map<String, String> ph = this.ph();
    String categoryDisplay =
        category == null || category.isEmpty() ? this.text("text.filter-all", "&7WSZYSTKIE") : category.toUpperCase(Locale.ROOT);
    ph.put("{player}", player.getName());
    ph.put("{filter}", categoryDisplay);
    ph.put("{search}", search == null || search.isEmpty() ? this.text("text.search-none", "&7BRAK") : search);
    ph.put("{sort}", sort == null ? SortMode.DEFAULT.label() : sort.label());
    ph.put(
        "{favorites}",
        String.valueOf(this.plugin.getFavoriteManager().count(player.getUniqueId())));
    ph.put(
        "{claims}",
        String.valueOf(this.plugin.getStatsManager().getPlayerTotalClaims(player.getUniqueId())));
    ph.put("{balance}", this.plugin.formatEconomyBalance(player));
    ph.put("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()));
    return ph;
  }

  public ItemStack buildKitItem(
      Player player, Kit kit, ConfigurationSection kitItemSection, GuiConfig gui) {
    Map<String, String> ph = this.kitPlaceholders(player, kit, gui);
    if (kitItemSection == null) {
      return kit.getIcon();
    }
    ItemStack item = gui.applyTemplate(kit.getIcon(), kitItemSection, ph);
    if (kit.getRarity() == KitRarity.LEGENDARY
        || kit.getRarity() == KitRarity.EPIC) {
      gui.applyGlow(item);
    }
    return item;
  }

  public Map<String, String> kitPlaceholders(Player player, Kit kit, GuiConfig gui) {
    Map<String, String> ph = this.ph();
    UUID uuid = player.getUniqueId();
    long remaining = this.plugin.getCooldownManager().getRemaining(uuid, kit.getId());
    boolean claimed = this.plugin.getCooldownManager().hasClaimed(uuid, kit.getId());
    boolean hasPermission = this.plugin.hasKitPermission(player, kit);
    int claimLimit = kit.getClaimCountLimit() == null ? -1 : kit.getClaimCountLimit();
    int playerClaims = this.plugin.getStatsManager().getPlayerKitClaims(uuid, kit.getId());

    String status;
    if (!kit.isEnabled()) {
      status = this.text("text.status-disabled", "&cWYLACZONY");
    } else if (!hasPermission) {
      status = this.text("text.status-no-permission", "&cBRAK UPRAWNIEN");
    } else if (claimLimit >= 0 && playerClaims >= claimLimit) {
      status = this.text("text.status-limit", "&cLIMIT OSIAGNIETY");
    } else if (kit.isOneTime() && claimed) {
      status = this.text("text.status-one-time", "&cJEDNORAZOWY");
    } else if (remaining > 0L) {
      status = this.text("text.status-cooldown", "&cCOOLDOWN");
    } else {
      status = this.text("text.status-available", "&aDOSTEPNY");
    }

    String cooldownText;
    if (kit.isOneTime() && claimed) {
      cooldownText = this.text("text.cooldown-one-time", "&7-");
    } else if (remaining > 0L) {
      cooldownText = TimeUtil.formatDuration(remaining);
    } else if (kit.getCooldownSeconds() > 0L) {
      cooldownText = TimeUtil.formatDuration(kit.getCooldownSeconds());
    } else {
      cooldownText = this.text("text.cooldown-ready", "&7BRAK");
    }

    ph.put("{kit_id}", kit.getId());
    ph.put("{kit_display}", kit.getDisplayName());
    ph.put("{cooldown}", cooldownText);
    ph.put("{status}", status);
    ph.put("{slot}", kit.getGuiSlot() == null ? "-" : String.valueOf(kit.getGuiSlot()));
    ph.put("{items}", String.valueOf(kit.getItems().size()));
    ph.put(
        "{one_time}",
        kit.isOneTime()
            ? this.text("text.one-time-yes", "&aTAK")
            : this.text("text.one-time-no", "&cNIE"));
    ph.put("{price}", this.plugin.formatPrice(kit.getPrice()));
    ph.put("{balance}", this.plugin.formatEconomyBalance(player));
    ph.put(
        "{rarity}",
        kit.getRarity() == null
            ? this.text("text.rarity-none", "&7BRAK")
            : kit.getRarity().display());
    ph.put("{rarity_key}", kit.getRarity() == null ? "none" : kit.getRarity().key());
    ph.put(
        "{category}",
        kit.getCategory() == null
            ? this.text("text.category-default", "&7DOMYSLNA")
            : kit.getCategory().toUpperCase(Locale.ROOT));
    ph.put(
        "{favorite}",
        this.plugin.getFavoriteManager().isFavorite(uuid, kit.getId())
            ? this.text("text.yes", "&aTAK")
            : this.text("text.no", "&cNIE"));
    ph.put("{claims}", String.valueOf(playerClaims));
    ph.put("{kit_claims}", String.valueOf(this.plugin.getStatsManager().getKitClaims(kit.getId())));
    ph.put(
        "{enabled}",
        kit.isEnabled()
            ? this.text("text.yes", "&aTAK")
            : this.text("text.no", "&cNIE"));
    ph.put(
        "{hidden}",
        kit.isHidden()
            ? this.text("text.yes", "&aTAK")
            : this.text("text.no", "&cNIE"));
    ph.put(
        "{welcome}",
        kit.isWelcome()
            ? this.text("text.yes", "&aTAK")
            : this.text("text.no", "&cNIE"));
    ph.put(
        "{claim_limit}",
        claimLimit < 0 ? this.text("text.cooldown-ready", "&7BRAK") : String.valueOf(claimLimit));
    this.applyRankPlaceholders(kit, ph);
    this.applyFeedbackPlaceholders(kit, ph);
    return ph;
  }

  private void applyRankPlaceholders(Kit kit, Map<String, String> ph) {
    String rankNone = this.text("text.rank-none", "&7BRAK");
    String prefixNone = this.text("text.rank-prefix-none", "&7-");
    String rank = kit.getRequiredGroup();
    if (rank == null || rank.isBlank()) {
      ph.put("{rank}", rankNone);
      ph.put("{rank_prefix}", prefixNone);
      ph.put("{rank_display}", rankNone);
      return;
    }
    String prefixRaw = LuckPermsHook.getGroupPrefix(rank);
    String display = prefixRaw.isEmpty() ? prefixNone : prefixRaw;
    ph.put("{rank}", rank);
    ph.put("{rank_prefix}", display);
    ph.put("{rank_display}", display);
  }

  private void applyFeedbackPlaceholders(Kit kit, Map<String, String> ph) {
    String type = kit.getFeedbackType();
    String defaultText = this.text("text.feedback-default", "&7DOMYSLNY");
    String noneText = this.text("text.feedback-none", "&7BRAK");
    String display =
        type == null || type.isBlank()
            ? defaultText
            : (type.equalsIgnoreCase("none") ? noneText : type.toUpperCase(Locale.ROOT));
    ph.put("{feedback_type}", type == null || type.isBlank() ? "default" : type);
    ph.put("{feedback_type_display}", display);
  }
}
