package pl.tymek24.fortcodekits.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.integration.LuckPermsHook;
import pl.tymek24.fortcodekits.model.Kit;

public class RankGUI {
  private final FortCodeKits plugin;

  public RankGUI(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  public void open(Player player, Kit kit) {
    this.open(player, kit, 1);
  }

  public void open(Player player, Kit kit, int page) {
    int pages;
    if (!LuckPermsHook.isLuckPermsAvailable()) {
      this.plugin.sendMessage(player, "luckperms-missing");
      this.plugin.getGuiManager().openEditor(player, kit);
      return;
    }
    GuiConfig gui = this.plugin.getGuiManager().getGuiConfig();
    int size = gui.getSize("rank.size", 54);
    String rawTitle = gui.getConfig().getString("rank.title", "&bRangi: {kit_display}");
    String title = this.plugin.formatText(rawTitle.replace("{kit_display}", kit.getDisplayName()));
    HashMap<Integer, String> slotToGroup = new HashMap<Integer, String>();
    GuiHolder holder =
        new GuiHolder(GuiType.RANK, slotToGroup, null, kit.getId(), page, GuiType.EDITOR);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, (int) size, (String) title);
    holder.setInventory(inv);
    this.applyFiller(inv, gui, "rank.filler");
    ArrayList<Group> groups = new ArrayList<Group>(LuckPermsHook.getGroupsSorted());
    List<Integer> groupSlots = gui.getSlots("rank.group-slots");
    ArrayList<Integer> availableSlots = new ArrayList<Integer>();
    for (int slot : groupSlots) {
      if (slot < 0 || slot >= size) continue;
      availableSlots.add(slot);
    }
    int perPage = availableSlots.size();
    int n = pages = perPage == 0 ? 1 : (int) Math.ceil((double) groups.size() / (double) perPage);
    if (pages <= 0) {
      pages = 1;
    }
    int safePage = Math.max(1, Math.min(page, pages));
    int start = (safePage - 1) * perPage;
    int end = Math.min(groups.size(), start + perPage);
    int slotIndex = 0;
    ConfigurationSection groupItemSection =
        gui.getConfig().getConfigurationSection("rank.group-item");
    for (int i = start; i < end && slotIndex < availableSlots.size(); ++i) {
      int slot = (Integer) availableSlots.get(slotIndex++);
      Group group = (Group) groups.get(i);
      ItemStack item = this.buildGroupItem(group, kit, groupItemSection, gui);
      inv.setItem(slot, item);
      slotToGroup.put(slot, group.getName());
    }
    this.placeNavigation(inv, gui, "rank.navigation", safePage, pages);
    this.placeNone(inv, gui, "rank.none");
    this.placeBack(inv, gui, "rank.back");
    player.openInventory(inv);
    this.plugin.playConfiguredSound(player, "sounds.open");
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

  private ItemStack buildGroupItem(
      Group group, Kit kit, ConfigurationSection section, GuiConfig gui) {
    String prefixDisplay;
    HashMap<String, String> placeholders = new HashMap<String, String>();
    String groupName = group.getName();
    String prefixRaw = LuckPermsHook.getGroupPrefix(groupName);
    String display =
        prefixDisplay =
            prefixRaw.isEmpty()
                ? gui.getConfig().getString("text.rank-prefix-none", "&7-")
                : prefixRaw;
    String selected =
        kit.getRequiredGroup() != null && kit.getRequiredGroup().equalsIgnoreCase(groupName)
            ? gui.getConfig().getString("text.rank-selected", "&a(aktualna)")
            : "";
    placeholders.put("{rank_name}", groupName);
    placeholders.put("{rank_prefix}", prefixDisplay);
    placeholders.put("{rank_display}", display);
    placeholders.put("{selected}", selected);
    if (section == null) {
      return new ItemStack(Material.NAME_TAG);
    }
    return gui.buildItem(section, placeholders);
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

  private void placeNone(Inventory inv, GuiConfig gui, String path) {
    int slot = gui.getConfig().getInt(path + ".slot", -1);
    if (slot < 0 || slot >= inv.getSize()) {
      return;
    }
    ItemStack item = gui.buildItem(path + ".item", Map.of());
    if (item != null) {
      inv.setItem(slot, item);
    }
  }

  private void placeBack(Inventory inv, GuiConfig gui, String path) {
    int slot = gui.getConfig().getInt(path + ".slot", -1);
    if (slot < 0 || slot >= inv.getSize()) {
      return;
    }
    ItemStack item = gui.buildItem(path + ".item", Map.of());
    if (item != null) {
      inv.setItem(slot, item);
    }
  }
}
