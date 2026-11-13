package pl.tymek24.fortcodekits.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.service.PlayerSettingsManager;

public class SettingsGUI extends AbstractGui {
  private static final String[][] TOGGLES = {
    {PlayerSettingsManager.KEY_SOUNDS, "sounds"},
    {PlayerSettingsManager.KEY_PARTICLES, "particles"},
    {PlayerSettingsManager.KEY_CLAIM_ANIMATION, "claim-animation"},
    {PlayerSettingsManager.KEY_TEXT_ANIMATION, "text-animation"},
    {PlayerSettingsManager.KEY_TITLE_FEEDBACK, "title-feedback"},
    {PlayerSettingsManager.KEY_BOSSBAR_FEEDBACK, "bossbar-feedback"},
    {PlayerSettingsManager.KEY_ACTIONBAR_FEEDBACK, "actionbar-feedback"}
  };

  public SettingsGUI(FortCodeKits plugin) {
    super(plugin);
  }

  public void open(Player player) {
    GuiConfig gui = this.cfg();
    int size = gui.getSize("settings.size", 54);
    Map<String, String> ph = this.buildPlaceholders(player);
    String title = this.title("settings.title", "&8USTAWIENIA", ph);
    HashMap<Integer, String> slotToKey = new HashMap<Integer, String>();
    GuiHolder holder = new GuiHolder(GuiType.SETTINGS, slotToKey, null, null, 0, GuiType.PLAYER);
    Inventory inv = Bukkit.createInventory((InventoryHolder) holder, size, title);
    holder.setInventory(inv);
    this.fill(inv, "settings.filler");
    ConfigurationSection toggles = gui.getConfig().getConfigurationSection("settings.toggles");
    if (toggles != null) {
      for (String[] pair : TOGGLES) {
        ConfigurationSection section = toggles.getConfigurationSection(pair[1]);
        if (section == null) continue;
        int slot = section.getInt("slot", -1);
        if (slot < 0 || slot >= size) continue;
        boolean enabled = this.plugin.getPlayerSettingsManager().get(player.getUniqueId(), pair[0]);
        Map<String, String> itemPh = new HashMap<String, String>(ph);
        itemPh.put("{setting}", pair[1].toUpperCase());
        itemPh.put(
            "{value}",
            enabled
                ? this.text("settings.on", "&aWLACZONE")
                : this.text("settings.off", "&cWYLACZONE"));
        itemPh.put("{state_key}", enabled ? "on" : "off");
        ConfigurationSection itemSection = section.getConfigurationSection(enabled ? "on" : "off");
        if (itemSection == null) itemSection = section;
        ItemStack item = this.sectionItem(itemSection, itemPh);
        if (item != null) {
          inv.setItem(slot, item);
          slotToKey.put(slot, pair[0]);
        }
      }
    }
    this.button(inv, "settings.actions.reset", ph);
    this.button(inv, "settings.actions.back", ph);
    player.openInventory(inv);
    this.openSound(player);
  }

  private Map<String, String> buildPlaceholders(Player player) {
    Map<String, String> ph = this.ph();
    UUID uuid = player.getUniqueId();
    PlayerSettingsManager manager = this.plugin.getPlayerSettingsManager();
    ph.put("{player}", player.getName());
    ph.put("{sounds}", this.state(manager.get(uuid, PlayerSettingsManager.KEY_SOUNDS)));
    ph.put("{particles}", this.state(manager.get(uuid, PlayerSettingsManager.KEY_PARTICLES)));
    ph.put(
        "{claim_animation}",
        this.state(manager.get(uuid, PlayerSettingsManager.KEY_CLAIM_ANIMATION)));
    ph.put(
        "{text_animation}",
        this.state(manager.get(uuid, PlayerSettingsManager.KEY_TEXT_ANIMATION)));
    ph.put(
        "{title_feedback}",
        this.state(manager.get(uuid, PlayerSettingsManager.KEY_TITLE_FEEDBACK)));
    ph.put(
        "{bossbar_feedback}",
        this.state(manager.get(uuid, PlayerSettingsManager.KEY_BOSSBAR_FEEDBACK)));
    ph.put(
        "{actionbar_feedback}",
        this.state(manager.get(uuid, PlayerSettingsManager.KEY_ACTIONBAR_FEEDBACK)));
    return ph;
  }

  private String state(boolean value) {
    return value
        ? this.text("settings.on", "&aWLACZONE")
        : this.text("settings.off", "&cWYLACZONE");
  }
}
