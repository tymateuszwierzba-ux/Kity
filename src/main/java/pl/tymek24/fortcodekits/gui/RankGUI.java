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

public class RankGUI extends AbstractGui {
  public RankGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player, Kit kit) {
    this.open(player, kit, 1);
  }

  public void open(Player player, Kit kit, int page) {
    if (!LuckPermsHook.isLuckPermsAvailable()) {
      this.plugin.sendMessage(player, "luckperms-missing");
      this.plugin.getGuiManager().openEditor(player, kit);
      return;
    }
    GuiConfig gui = this.cfg();
    int size = gui.getSize("rank.size", 54);
    String title = this.title("rank.title", "&bRangi: {kit_display}", Map.of("{kit_display}", kit.getDisplayName()));
    Map<Integer, String> slotToGroup = new HashMap<Integer, String>();
    GuiHolder holder = new GuiHolder(GuiType.RANK, slotToGroup, null, kit.getId(), page, GuiType.EDITOR);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "rank.filler");
    List<Integer> groupSlots = new ArrayList<Integer>();
    for (int slot : gui.getSlots("rank.group-slots")) {
      if (slot >= 0 && slot < size) {
        groupSlots.add(slot);
      }
    }
    ArrayList<Group> groups = new ArrayList<Group>(LuckPermsHook.getGroupsSorted());
    int perPage = groupSlots.size();
    int pages = perPage == 0 ? 1 : (int) Math.ceil((double) groups.size() / (double) perPage);
    if (pages <= 0) {
      pages = 1;
    }
    int safePage = Math.max(1, Math.min(page, pages));
    int start = (safePage - 1) * perPage;
    int end = Math.min(groups.size(), start + perPage);
    ConfigurationSection groupItem = gui.getConfig().getConfigurationSection("rank.group-item");
    for (int i = start, slotIndex = 0; i < end && slotIndex < groupSlots.size(); i++, slotIndex++) {
      Group group = groups.get(i);
      int slot = groupSlots.get(slotIndex);
      ItemStack item = this.buildGroupItem(group, kit, groupItem);
      if (item == null) {
        continue;
      }
      inv.setItem(slot, item);
      slotToGroup.put(slot, group.getName());
    }
    this.navigation(inv, "rank.navigation", safePage, pages);
    this.button(inv, "rank.none", Map.of());
    this.button(inv, "rank.back", Map.of());
    player.openInventory(inv);
    this.openSound(player);
  }

  private ItemStack buildGroupItem(Group group, Kit kit, ConfigurationSection section) {
    String groupName = group.getName();
    String prefix = LuckPermsHook.getGroupPrefix(groupName);
    String display = prefix.isEmpty() ? this.text("text.rank-prefix-none", "&7-") : prefix;
    boolean selected = kit.getRequiredGroup() != null && kit.getRequiredGroup().equalsIgnoreCase(groupName);
    Map<String, String> ph = this.ph();
    ph.put("{rank_name}", groupName);
    ph.put("{rank_prefix}", prefix);
    ph.put("{rank_display}", display);
    ph.put("{selected}", selected ? this.text("text.rank-selected", "&a(aktualna)") : "");
    ItemStack item = this.sectionItem(section, ph);
    return item != null ? item : new ItemStack(Material.NAME_TAG);
  }
}
