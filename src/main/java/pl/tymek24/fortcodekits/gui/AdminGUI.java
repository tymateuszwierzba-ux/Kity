package pl.tymek24.fortcodekits.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.model.Kit;
import pl.tymek24.fortcodekits.model.KitRarity;
import pl.tymek24.fortcodekits.model.SortMode;
import pl.tymek24.fortcodekits.util.TimeUtil;

public class AdminGUI extends AbstractGui {
  public AdminGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player) {
    this.open(player, 1);
  }

  public void open(Player player, int page) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("admin.size", 54);
    UUID uuid = player.getUniqueId();
    String category = GuiSession.category(uuid);
    String search = GuiSession.search(uuid);
    SortMode sort = GuiSession.sort(uuid);
    Map<String, String> headerPh = this.headerPlaceholders(category, search, sort);
    String title = this.title("admin.title", "&8ADMIN KITY", headerPh);

    HashMap<Integer, String> slotToKit = new HashMap<Integer, String>();
    GuiHolder holder = new GuiHolder(GuiType.ADMIN, slotToKit, null, null, page, null);
    holder.setCategoryFilter(category);
    holder.setSearchQuery(search);
    holder.setSortMode(sort);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "admin.filler");

    List<Kit> kits = this.visibleAdminKits(category, search, sort);
    HashSet<String> fixed = new HashSet<String>();
    List<Integer> kitSlots = gui.getSlots("admin.kit-slots");
    ConfigurationSection kitItemSection = gui.getConfig().getConfigurationSection("admin.kit-item");
    for (Kit kit : kits) {
      Integer slot = kit.getGuiSlot();
      if (slot == null || slot < 0 || slot >= size || slotToKit.containsKey(slot)) continue;
      inv.setItem(slot, this.buildKitItem(kit, kitItemSection, gui));
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
      inv.setItem(slot, this.buildKitItem(kit, kitItemSection, gui));
      slotToKit.put(slot, kit.getId());
    }

    headerPh.put("{page}", String.valueOf(safePage));
    headerPh.put("{pages}", String.valueOf(pages));
    headerPh.put("{visible}", String.valueOf(slotToKit.size()));
    headerPh.put("{total}", String.valueOf(kits.size()));
    this.placeSection(inv, "admin.header", headerPh);
    this.placeSectionButtons(inv, "admin.footer", headerPh, safePage, pages, search);
    this.button(inv, "admin.actions.create", headerPh);
    player.openInventory(inv);
    this.openSound(player);
  }

  private void placeSection(Inventory inv, String path, Map<String, String> placeholders) {
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

  private void placeSectionButtons(
      Inventory inv,
      String path,
      Map<String, String> placeholders,
      int page,
      int pages,
      String search) {
    ConfigurationSection section = this.cfg().getConfig().getConfigurationSection(path);
    if (section == null) {
      return;
    }
    for (String key : section.getKeys(false)) {
      ConfigurationSection button = section.getConfigurationSection(key);
      if (button == null) continue;
      if (key.equals("clear-search") && (search == null || search.isEmpty())) continue;
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

  public void refresh(Player player, GuiHolder holder) {
    if (holder == null || holder.getType() != GuiType.ADMIN) {
      return;
    }
    Inventory inv = holder.getInventory();
    if (inv == null) {
      return;
    }
    GuiConfig gui = this.cfg();
    ConfigurationSection kitItemSection = gui.getConfig().getConfigurationSection("admin.kit-item");
    for (Map.Entry<Integer, String> entry : holder.getKitSlots().entrySet()) {
      Kit kit = this.plugin.getKitManager().getKit(entry.getValue()).orElse(null);
      if (kit == null) {
        inv.setItem(entry.getKey(), null);
        continue;
      }
      inv.setItem(entry.getKey(), this.buildKitItem(kit, kitItemSection, gui));
    }
  }

  private List<Kit> visibleAdminKits(String category, String search, SortMode sort) {
    List<Kit> kits = new ArrayList<Kit>();
    String categoryKey = category == null ? "" : category.trim().toLowerCase(java.util.Locale.ROOT);
    String query = search == null ? "" : search.trim().toLowerCase(java.util.Locale.ROOT);
    for (Kit kit : this.plugin.getKitManager().getKits()) {
      if (!categoryKey.isEmpty() && !categoryKey.equals("all")) {
        String kitCategory = kit.getCategory() == null ? "default" : kit.getCategory();
        if (!kitCategory.equalsIgnoreCase(categoryKey)) continue;
      }
      if (!query.isEmpty()) {
        String haystack =
            (kit.getId()
                    + " "
                    + kit.getDisplayName()
                    + " "
                    + (kit.getCategory() == null ? "" : kit.getCategory()))
                .toLowerCase(java.util.Locale.ROOT);
        if (!haystack.contains(query)) continue;
      }
      kits.add(kit);
    }
    if (sort == null || sort == SortMode.DEFAULT) {
      kits.sort(java.util.Comparator.comparing(Kit::getId));
    } else {
      switch (sort) {
        case NAME:
          kits.sort(
              java.util.Comparator.comparing(
                  kit ->
                      kit.getDisplayName()
                          .replaceAll("(?i)<[^>]+>", "")
                          .toLowerCase(java.util.Locale.ROOT)));
          break;
        case COOLDOWN:
          kits.sort(java.util.Comparator.comparingLong(Kit::getCooldownSeconds));
          break;
        case STATUS:
          kits.sort(
              java.util.Comparator.comparing(
                  kit -> !kit.isEnabled() ? 0 : (kit.isHidden() ? 1 : 2)));
          break;
        case RARITY:
          kits.sort(
              java.util.Comparator.comparingInt(
                  kit -> kit.getRarity() == null ? -1 : kit.getRarity().ordinal()));
          break;
        default:
          kits.sort(java.util.Comparator.comparing(Kit::getId));
      }
    }
    return kits;
  }

  private Map<String, String> headerPlaceholders(String category, String search, SortMode sort) {
    Map<String, String> ph = this.ph();
    String categoryDisplay =
        category == null || category.isEmpty()
            ? this.text("text.filter-all", "&7WSZYSTKIE")
            : category.toUpperCase(java.util.Locale.ROOT);
    ph.put("{filter}", categoryDisplay);
    ph.put(
        "{search}",
        search == null || search.isEmpty() ? this.text("text.search-none", "&7BRAK") : search);
    ph.put("{sort}", sort == null ? SortMode.DEFAULT.label() : sort.label());
    ph.put("{kits}", String.valueOf(this.plugin.getKitManager().getKits().size()));
    ph.put("{total_claims}", String.valueOf(this.plugin.getStatsManager().getTotalClaims()));
    ph.put(
        "{history}",
        String.valueOf(this.plugin.getStatsManager().historySize()));
    return ph;
  }

  public ItemStack buildKitItem(Kit kit, ConfigurationSection kitItemSection, GuiConfig gui) {
    Map<String, String> ph = this.kitPlaceholders(kit, gui);
    if (kitItemSection == null) {
      return kit.getIcon();
    }
    ItemStack item = gui.applyTemplate(kit.getIcon(), kitItemSection, ph);
    if (kit.getRarity() == KitRarity.LEGENDARY || kit.getRarity() == KitRarity.EPIC) {
      gui.applyGlow(item);
    }
    return item;
  }

  private Map<String, String> kitPlaceholders(Kit kit, GuiConfig gui) {
    Map<String, String> ph = this.ph();
    ph.put("{kit_id}", kit.getId());
    ph.put("{kit_display}", kit.getDisplayName());
    ph.put("{cooldown}", TimeUtil.formatDuration(kit.getCooldownSeconds()));
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
      String prefix = pl.tymek24.fortcodekits.integration.LuckPermsHook.getGroupPrefix(rank);
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
