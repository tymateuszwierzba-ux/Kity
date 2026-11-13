//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package pl.tymek24.fortcodekits;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.tymek24.fortcodekits.animation.KitClaimAnimationManager;
import pl.tymek24.fortcodekits.animation.TextAnimationManager;
import pl.tymek24.fortcodekits.command.CommandManager;
import pl.tymek24.fortcodekits.database.DatabaseManager;
import pl.tymek24.fortcodekits.gui.GUIManager;
import pl.tymek24.fortcodekits.integration.KitsPlaceholderExpansion;
import pl.tymek24.fortcodekits.integration.LuckPermsHook;
import pl.tymek24.fortcodekits.integration.VaultHook;
import pl.tymek24.fortcodekits.listener.ChatInputListener;
import pl.tymek24.fortcodekits.listener.InventoryClickListener;
import pl.tymek24.fortcodekits.listener.PlayerJoinListener;
import pl.tymek24.fortcodekits.listener.PlayerQuitListener;
import pl.tymek24.fortcodekits.model.Kit;
import pl.tymek24.fortcodekits.security.LicenseGuard;
import pl.tymek24.fortcodekits.service.CooldownManager;
import pl.tymek24.fortcodekits.service.KitManager;
import pl.tymek24.fortcodekits.util.ColorUtil;

public class FortCodeKits extends JavaPlugin {
  private DatabaseManager databaseManager;
  private KitManager kitManager;
  private CooldownManager cooldownManager;
  private GUIManager guiManager;
  private ChatInputListener chatInputListener;
  private TextAnimationManager textAnimationManager;
  private KitClaimAnimationManager kitClaimAnimationManager;
  private LicenseGuard licenseGuard;
  private FileConfiguration messages;
  private final Map<UUID, BukkitTask> activeClaimActionbarTasks = new ConcurrentHashMap();
  private final Map<UUID, BukkitTask> activeClaimBossbarTasks = new ConcurrentHashMap();
  private final Map<UUID, BossBar> activeClaimBossbars = new ConcurrentHashMap();

  public void onEnable() {
    this.saveDefaultConfig();
    this.saveResourceIfMissing("kits.yml");
    this.saveResourceIfMissing("messages.yml");
    this.saveResourceIfMissing("gui.yml");
    this.saveResourceIfMissing("animations.yml");
    this.saveResourceIfMissing("animations2.yml");
    this.saveResourceIfMissing("license.yml");
    this.licenseGuard = new LicenseGuard(this);
    LicenseGuard.LicenseState licenseState = this.licenseGuard.reloadAndValidate();
    if (!licenseState.valid()) {
      this.getLogger().severe("License check failed: " + licenseState.reason());
      this.getLogger().severe("Runtime server-id: " + licenseState.runtimeServerId());
      this.getLogger().severe("Owner: " + licenseState.owner() + ", expires: " + licenseState.expiresAt());
      this.getLogger().severe("Plugin zostal wylaczony przez zabezpieczenie licencyjne.");
      Bukkit.getPluginManager().disablePlugin(this);
      return;
    }
    if (!licenseState.enabled()) {
      this.getLogger().warning("License mode disabled (development). Wlacz license.enabled w license.yml.");
    } else {
      this.getLogger().info("License OK for server-id: " + licenseState.runtimeServerId());
    }

    this.loadMessages();
    this.textAnimationManager = new TextAnimationManager(this);
    this.kitClaimAnimationManager = new KitClaimAnimationManager(this);
    this.databaseManager = new DatabaseManager(this);
    this.databaseManager.init().join();
    this.kitManager = new KitManager(this);
    this.kitManager.load();
    this.cooldownManager = new CooldownManager(this.databaseManager);
    this.guiManager = new GUIManager(this);
    this.guiManager.startAutoRefresh();
    this.chatInputListener = new ChatInputListener(this);
    Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);
    Bukkit.getPluginManager().registerEvents(this.chatInputListener, this);
    Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
    new CommandManager(this);
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      (new KitsPlaceholderExpansion(this)).register();
      this.getLogger().info("Hook PlaceholderAPI aktywny.");
    }

    Logger var10000 = this.getLogger();
    String var10001 = VaultHook.isVaultAvailable() ? "ON" : "OFF";
    var10000.info("Vault integration: " + var10001);
    this.getLogger().info("LuckPerms integration: " + (LuckPermsHook.isLuckPermsAvailable() ? "ON" : "OFF"));
  }

  public void onDisable() {
    this.clearAllClaimFeedbackVisuals();
    if (this.kitManager != null) {
      this.kitManager.save();
    }

    if (this.guiManager != null) {
      this.guiManager.stopAutoRefresh();
    }

    if (this.databaseManager != null) {
      this.databaseManager.close().join();
    }
    if (this.kitClaimAnimationManager != null) {
      this.kitClaimAnimationManager.shutdown();
    }

  }

  public void reloadPlugin() {
    this.reloadConfig();
    if (this.licenseGuard != null) {
      LicenseGuard.LicenseState state = this.licenseGuard.reloadAndValidate();
      if (!state.valid()) {
        this.getLogger().severe("License check failed on reload: " + state.reason());
        Bukkit.getPluginManager().disablePlugin(this);
        return;
      }
    }

    this.loadMessages();
    if (this.textAnimationManager != null) {
      this.textAnimationManager.reload();
    }
    if (this.kitClaimAnimationManager != null) {
      this.kitClaimAnimationManager.reload();
    }

    this.guiManager.reload();
    this.guiManager.restartAutoRefresh();
    this.kitManager.load();
  }

  public void giveKit(Player player, Kit kit, boolean bypassCooldown) {
    if (this.licenseGuard != null && !this.licenseGuard.isValid()) {
      this.clearClaimFeedbackVisual(player);
      player.sendMessage(this.formatText("&cLicencja pluginu jest niepoprawna."));
    } else if (!player.hasPermission("fortcode.kits.use") && !bypassCooldown) {
      this.clearClaimFeedbackVisual(player);
      this.sendMessage(player, "no-permission");
    } else if (!bypassCooldown && !this.hasKitPermission(player, kit)) {
      this.clearClaimFeedbackVisual(player);
      this.sendMessage(player, "no-permission");
    } else if (!bypassCooldown && !this.cooldownManager.canClaim(player, kit)) {
      this.clearClaimFeedbackVisual(player);
      this.sendMessage(player, "cooldown");
    } else if (!this.hasInventorySpace(player, kit.getItems())) {
      this.clearClaimFeedbackVisual(player);
      this.sendMessage(player, "inventory-full");
    } else {
      if (this.kitClaimAnimationManager != null) {
        this.kitClaimAnimationManager.giveItems(player, kit);
      } else {
        for (ItemStack item : kit.getItems()) {
          player.getInventory().addItem(item.clone());
        }
      }

      if (!bypassCooldown) {
        this.cooldownManager.recordClaim(player.getUniqueId(), kit);
      }

      this.sendClaimFeedback(player, kit);
      this.playConfiguredSound(player, "sounds.claim");
      this.playClaimAnimation(player);
      if (this.guiManager != null) {
        this.guiManager.refreshPlayerIfOpen(player);
      }

    }
  }

  public void createKitFromInventory(Player player, String id) {
    ItemStack hand = player.getInventory().getItemInMainHand();
    Kit kit = new Kit(id);
    kit.setDisplayName(id);
    kit.setIcon(hand.getType().isAir() ? new ItemStack(Material.CHEST) : hand);
    kit.setItems(this.collectInventoryItems(player));
    this.kitManager.upsert(kit);
    this.kitManager.save();
    this.sendMessage(player, "kit-created", Map.of("{kit}", kit.getDisplayName()));
  }

  private boolean hasInventorySpace(Player player, List<ItemStack> items) {
    ItemStack[] simulated = (ItemStack[])player.getInventory().getStorageContents().clone();

    for(int i = 0; i < simulated.length; ++i) {
      if (simulated[i] != null) {
        simulated[i] = simulated[i].clone();
      }
    }

    int inventoryMaxStack = Math.max(1, player.getInventory().getMaxStackSize());

    for(ItemStack rawItem : items) {
      if (rawItem != null && rawItem.getType() != Material.AIR) {
        ItemStack item = rawItem.clone();
        int maxStack = Math.max(1, Math.min(inventoryMaxStack, item.getMaxStackSize()));
        int remaining = Math.max(1, item.getAmount());

        for(int slot = 0; slot < simulated.length && remaining > 0; ++slot) {
          ItemStack current = simulated[slot];
          if (current != null && current.getType() != Material.AIR && current.isSimilar(item)) {
            int currentMax = Math.max(1, Math.min(inventoryMaxStack, current.getMaxStackSize()));
            int free = Math.max(0, currentMax - current.getAmount());
            if (free != 0) {
              int move = Math.min(free, remaining);
              current.setAmount(current.getAmount() + move);
              remaining -= move;
            }
          }
        }

        while(remaining > 0) {
          int emptySlot = this.findFirstEmptyStorageSlot(simulated);
          if (emptySlot < 0) {
            return false;
          }

          int toPlace = Math.min(remaining, maxStack);
          ItemStack placed = item.clone();
          placed.setAmount(toPlace);
          simulated[emptySlot] = placed;
          remaining -= toPlace;
        }
      }
    }

    return true;
  }

  private int findFirstEmptyStorageSlot(ItemStack[] contents) {
    for(int i = 0; i < contents.length; ++i) {
      ItemStack stack = contents[i];
      if (stack == null || stack.getType() == Material.AIR) {
        return i;
      }
    }

    return -1;
  }

  private void saveResourceIfMissing(String name) {
    File file = new File(this.getDataFolder(), name);
    if (!file.exists()) {
      this.saveResource(name, false);
    }

  }

  private void loadMessages() {
    File file = new File(this.getDataFolder(), "messages.yml");
    this.messages = YamlConfiguration.loadConfiguration(file);
    InputStream resource = this.getResource("messages.yml");
    if (resource != null) {
      YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
      this.messages.setDefaults(defaults);
      this.messages.options().copyDefaults(true);

      try {
        this.messages.save(file);
      } catch (IOException e) {
        this.getLogger().warning("Nie udalo sie zapisac messages.yml: " + e.getMessage());
      }

    }
  }

  public String msg(String path) {
    String raw = this.messages.getString("messages." + path, "&cMissing message: " + path);
    return this.formatText(raw);
  }

  public String msg(String path, Map<String, String> placeholders) {
    String raw = this.messages.getString("messages." + path, "&cMissing message: " + path);

    for(Map.Entry<String, String> entry : placeholders.entrySet()) {
      raw = raw.replace((CharSequence)entry.getKey(), (CharSequence)entry.getValue());
    }

    return this.formatText(raw);
  }

  public void sendMessage(Player player, String key) {
    this.sendMessage(player, key, Map.of());
  }

  public void sendMessage(Player player, String key, Map<String, String> placeholders) {
    List<String> channels = this.getMessageChannels(key);
    if (!channels.isEmpty()) {
      this.sendConfiguredMessage(player, key, channels, placeholders);
    }
  }

  public List<ItemStack> collectInventoryItems(Player player) {
    List<ItemStack> items = new ArrayList();

    for(ItemStack stack : player.getInventory().getStorageContents()) {
      if (stack != null && stack.getType() != Material.AIR) {
        items.add(stack.clone());
      }
    }

    return items;
  }

  public void playConfiguredSound(Player player, String path) {
    String name = this.getConfig().getString(path, "").trim();
    if (!name.isEmpty()) {
      try {
        Sound sound = Sound.valueOf(name.toUpperCase(Locale.ROOT));
        player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
      } catch (IllegalArgumentException var5) {
      }

    }
  }

  public boolean hasKitPermission(Player player, Kit kit) {
    if (!this.hasRequiredGroup(player, kit)) {
      return false;
    } else {
      String var10000 = kit.getId();
      String kitPerm = "fortcode.kits.kit." + var10000.toLowerCase(Locale.ROOT);
      return player.hasPermission("fortcode.kits.admin") || player.hasPermission("fortcode.kits.*") || player.hasPermission("fortcode.kits.kit.*") || player.hasPermission(kitPerm);
    }
  }

  private boolean hasRequiredGroup(Player player, Kit kit) {
    String requiredGroup = kit.getRequiredGroup();
    if (requiredGroup != null && !requiredGroup.isBlank()) {
      if (LuckPermsHook.isLuckPermsAvailable()) {
        return LuckPermsHook.isUserInGroup(player, requiredGroup);
      } else {
        return player.hasPermission("group." + requiredGroup) || player.hasPermission("luckperms.group." + requiredGroup);
      }
    } else {
      return true;
    }
  }

  public DatabaseManager getDatabaseManager() {
    return this.databaseManager;
  }

  public KitManager getKitManager() {
    return this.kitManager;
  }

  public CooldownManager getCooldownManager() {
    return this.cooldownManager;
  }

  public GUIManager getGuiManager() {
    return this.guiManager;
  }

  public ChatInputListener getChatInputListener() {
    return this.chatInputListener;
  }

  public TextAnimationManager getTextAnimationManager() {
    return this.textAnimationManager;
  }

  public KitClaimAnimationManager getKitClaimAnimationManager() {
    return this.kitClaimAnimationManager;
  }

  public String formatText(String raw) {
    if (raw == null) {
      return "";
    } else {
      String animated = this.textAnimationManager == null ? raw : this.textAnimationManager.apply(raw);
      return ColorUtil.colorize(animated);
    }
  }

  public void clearClaimFeedbackVisual(Player player) {
    if (player != null) {
      UUID uuid = player.getUniqueId();
      BukkitTask actionbarTask = (BukkitTask)this.activeClaimActionbarTasks.remove(uuid);
      if (actionbarTask != null) {
        actionbarTask.cancel();
      }

      BukkitTask bossbarTask = (BukkitTask)this.activeClaimBossbarTasks.remove(uuid);
      if (bossbarTask != null) {
        bossbarTask.cancel();
      }

      BossBar bar = (BossBar)this.activeClaimBossbars.remove(uuid);
      if (bar != null) {
        bar.removePlayer(player);
        bar.removeAll();
      }

    }
  }

  private void clearAllClaimFeedbackVisuals() {
    for(BukkitTask task : this.activeClaimActionbarTasks.values()) {
      if (task != null) {
        task.cancel();
      }
    }

    this.activeClaimActionbarTasks.clear();

    for(BukkitTask task : this.activeClaimBossbarTasks.values()) {
      if (task != null) {
        task.cancel();
      }
    }

    this.activeClaimBossbarTasks.clear();

    for(BossBar bar : this.activeClaimBossbars.values()) {
      if (bar != null) {
        bar.removeAll();
      }
    }

    this.activeClaimBossbars.clear();
  }

  public void sendClaimFeedback(Player player, Kit kit) {
    List<String> channels = this.resolveClaimChannels(kit);
    if (!channels.isEmpty()) {
      this.clearClaimFeedbackVisual(player);
      Map<String, String> placeholders = Map.of("{kit}", kit.getDisplayName());
      this.sendConfiguredMessage(player, "kit-received", channels, placeholders);
    }
  }

  private List<String> resolveClaimChannels(Kit kit) {
    List<String> raw = new ArrayList();
    boolean override = kit != null && kit.getFeedbackType() != null && !kit.getFeedbackType().isBlank();
    if (override) {
      for(String part : kit.getFeedbackType().split(",")) {
        if (part != null) {
          String value = part.trim().toLowerCase(Locale.ROOT);
          if (!value.isEmpty()) {
            raw.add(value);
          }
        }
      }
    } else {
      raw.addAll(this.getMessageChannelsRaw("kit-received"));
      if (raw.isEmpty()) {
        raw.addAll(this.getConfig().getStringList("feedback.claim.channels"));
      }
    }

    if (raw.isEmpty()) {
      raw = List.of("chat");
    }

    boolean[] noneSelected = new boolean[]{false};
    List<String> normalized = this.normalizeChannels(raw, noneSelected);
    if (noneSelected[0]) {
      return List.of();
    } else {
      if (override && normalized.isEmpty()) {
        raw = this.getConfig().getStringList("feedback.claim.channels");
        if (raw.isEmpty()) {
          raw = List.of("chat");
        }

        normalized = this.normalizeChannels(raw, noneSelected);
        if (noneSelected[0]) {
          return List.of();
        }
      }

      return normalized;
    }
  }

  private List<String> getMessageChannelsRaw(String key) {
    if (this.messages == null) {
      return List.of();
    } else {
      List<String> list = new ArrayList();

      for(String entry : this.messages.getStringList("messages." + key + "-channels")) {
        if (entry != null) {
          String value = entry.trim();
          if (!value.isEmpty()) {
            list.add(value);
          }
        }
      }

      if (!list.isEmpty()) {
        return list;
      } else {
        String single = this.messages.getString("messages." + key + "-type", "").trim();
        if (!single.isEmpty() && !single.equalsIgnoreCase("default")) {
          String[] parts = single.split(",");

          for(String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
              list.add(value);
            }
          }

          return list;
        } else {
          return List.of();
        }
      }
    }
  }

  private List<String> getMessageChannels(String key) {
    List<String> raw = new ArrayList(this.getMessageChannelsRaw(key));
    if (raw.isEmpty()) {
      raw = List.of("chat");
    }

    boolean[] noneSelected = new boolean[]{false};
    List<String> normalized = this.normalizeChannels(raw, noneSelected);
    if (noneSelected[0]) {
      return List.of();
    } else {
      return normalized.isEmpty() ? List.of("chat") : normalized;
    }
  }

  private void sendConfiguredMessage(final Player player, String key, List<String> channels, Map<String, String> placeholders) {
    if (channels.contains("chat")) {
      player.sendMessage(this.msg(key, placeholders));
    }

    if (channels.contains("title")) {
      String title = this.getMessageOptional(key + "-title", key, placeholders);
      String subtitle = this.getMessageOptional(key + "-subtitle", "", placeholders);
      if (!title.isBlank() || !subtitle.isBlank()) {
        int fadeIn = Math.max(0, this.getConfig().getInt("feedback.claim.title.fade-in", 10));
        int stay = Math.max(0, this.getConfig().getInt("feedback.claim.title.stay", 40));
        int fadeOut = Math.max(0, this.getConfig().getInt("feedback.claim.title.fade-out", 10));
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
      }
    }

    if (channels.contains("actionbar")) {
      final String actionRaw = this.getMessageRawOptional(key + "-actionbar", key, placeholders);
      if (!actionRaw.isBlank()) {
        boolean dynamic = this.shouldRunDynamicClaimText(key, "actionbar") && this.hasDynamicAnimationTag(actionRaw);
        final UUID uuid = player.getUniqueId();
        BukkitTask previous = (BukkitTask)this.activeClaimActionbarTasks.remove(uuid);
        if (previous != null) {
          previous.cancel();
        }

        if (dynamic) {
          final int duration = this.getClaimActionbarAnimationDurationTicks();
          final int interval = this.getClaimMessageAnimationIntervalTicks();
          BukkitTask task = (new BukkitRunnable() {
            int remaining = duration;

            public void run() {
              if (!player.isOnline()) {
                FortCodeKits.this.activeClaimActionbarTasks.remove(uuid);
                this.cancel();
              } else {
                player.sendActionBar(FortCodeKits.this.formatText(actionRaw));
                this.remaining -= interval;
                if (this.remaining <= 0) {
                  FortCodeKits.this.activeClaimActionbarTasks.remove(uuid);
                  this.cancel();
                }

              }
            }
          }).runTaskTimer(this, 0L, (long)interval);
          this.activeClaimActionbarTasks.put(uuid, task);
        } else {
          player.sendActionBar(this.formatText(actionRaw));
        }
      }
    }

    if (channels.contains("bossbar")) {
      final String textRaw = this.getMessageRawOptional(key + "-bossbar", key, placeholders);
      if (textRaw.isBlank()) {
        return;
      }

      final UUID uuid = player.getUniqueId();
      BukkitTask previousTask = (BukkitTask)this.activeClaimBossbarTasks.remove(uuid);
      if (previousTask != null) {
        previousTask.cancel();
      }

      BossBar previousBar = (BossBar)this.activeClaimBossbars.remove(uuid);
      if (previousBar != null) {
        previousBar.removePlayer(player);
        previousBar.removeAll();
      }

      String colorRaw = this.getConfig().getString("feedback.claim.bossbar.color", "GREEN");
      String styleRaw = this.getConfig().getString("feedback.claim.bossbar.style", "SOLID");
      final int duration = Math.max(10, this.getConfig().getInt("feedback.claim.bossbar.duration-ticks", 60));
      final int interval = this.getClaimMessageAnimationIntervalTicks();
      final boolean dynamic = this.shouldRunDynamicClaimText(key, "bossbar") && this.hasDynamicAnimationTag(textRaw);
      BarColor color = this.parseBarColor(colorRaw);
      BarStyle style = this.parseBarStyle(styleRaw);
      final BossBar bar = Bukkit.createBossBar(this.formatText(textRaw), color, style, new BarFlag[0]);
      bar.addPlayer(player);
      bar.setProgress((double)1.0F);
      this.activeClaimBossbars.put(uuid, bar);
      BukkitTask task = (new BukkitRunnable() {
        int remaining = duration;
        int tick = 0;

        public void run() {
          if (!player.isOnline()) {
            bar.removeAll();
            FortCodeKits.this.activeClaimBossbarTasks.remove(uuid);
            FortCodeKits.this.activeClaimBossbars.remove(uuid);
            this.cancel();
          } else {
            if (dynamic && this.tick++ % interval == 0) {
              bar.setTitle(FortCodeKits.this.formatText(textRaw));
            }

            double progress = Math.max((double)0.0F, (double)this.remaining / (double)duration);
            bar.setProgress(progress);
            if (this.remaining-- <= 0) {
              bar.removePlayer(player);
              bar.removeAll();
              FortCodeKits.this.activeClaimBossbarTasks.remove(uuid);
              FortCodeKits.this.activeClaimBossbars.remove(uuid);
              this.cancel();
            }

          }
        }
      }).runTaskTimer(this, 0L, 1L);
      this.activeClaimBossbarTasks.put(uuid, task);
    }

  }

  private String getMessageRawOptional(String key, String fallbackKey, Map<String, String> placeholders) {
    String raw = this.messages == null ? null : this.messages.getString("messages." + key);
    if (raw == null || raw.isBlank()) {
      if (fallbackKey == null || fallbackKey.isBlank()) {
        return "";
      }

      raw = this.messages == null ? null : this.messages.getString("messages." + fallbackKey);
    }

    if (raw != null && !raw.isBlank()) {
      for(Map.Entry<String, String> entry : placeholders.entrySet()) {
        raw = raw.replace((CharSequence)entry.getKey(), (CharSequence)entry.getValue());
      }

      return raw;
    } else {
      return "";
    }
  }

  private String getMessageOptional(String key, String fallbackKey, Map<String, String> placeholders) {
    return this.formatText(this.getMessageRawOptional(key, fallbackKey, placeholders));
  }

  private boolean shouldRunDynamicClaimText(String key, String channel) {
    if (!"kit-received".equalsIgnoreCase(key)) {
      return false;
    } else if (!this.getConfig().getBoolean("feedback.claim.message-animation.enabled", true)) {
      return false;
    } else {
      List<String> channels = this.getConfig().getStringList("feedback.claim.message-animation.channels");
      if (channels.isEmpty()) {
        channels = List.of("actionbar", "bossbar");
      }

      String requested = channel.toLowerCase(Locale.ROOT);

      for(String value : channels) {
        if (value != null && value.trim().toLowerCase(Locale.ROOT).equals(requested)) {
          return true;
        }
      }

      return false;
    }
  }

  private int getClaimMessageAnimationIntervalTicks() {
    return Math.max(1, this.getConfig().getInt("feedback.claim.message-animation.interval-ticks", 1));
  }

  private int getClaimActionbarAnimationDurationTicks() {
    return Math.max(1, this.getConfig().getInt("feedback.claim.message-animation.actionbar-duration-ticks", 40));
  }

  private boolean hasDynamicAnimationTag(String value) {
    return value != null && value.toLowerCase(Locale.ROOT).contains("<anim:");
  }

  private List<String> normalizeChannels(List<String> raw, boolean[] noneSelected) {
    List<String> allowed = List.of("chat", "title", "actionbar", "bossbar");
    List<String> result = new ArrayList();
    boolean all = false;

    for(String entry : raw) {
      if (entry != null) {
        String value = entry.trim().toLowerCase(Locale.ROOT);
        if (!value.isEmpty()) {
          if (value.equals("boosbar")) {
            value = "bossbar";
          }

          if (value.equals("action")) {
            value = "actionbar";
          }

          if (value.equals("none")) {
            noneSelected[0] = true;
            return List.of();
          }

          if (value.equals("all")) {
            all = true;
          } else if (allowed.contains(value) && !result.contains(value)) {
            result.add(value);
          }
        }
      }
    }

    if (all) {
      return new ArrayList(allowed);
    } else {
      return result;
    }
  }

  public void playClaimAnimation(final Player player) {
    if (this.getConfig().getBoolean("animation.claim.enabled", true)) {
      String particleName = this.getConfig().getString("animation.claim.particle", "FIREWORKS_SPARK");

      final Particle particle;
      try {
        particle = Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var23) {
        return;
      }

      final int duration = Math.max(1, this.getConfig().getInt("animation.claim.duration-ticks", 20));
      final int points = Math.max(4, this.getConfig().getInt("animation.claim.points", 24));
      final double radius = Math.max(0.1, this.getConfig().getDouble("animation.claim.radius", 1.2));
      final double height = Math.max(0.1, this.getConfig().getDouble("animation.claim.height", 1.6));
      final double rotation = this.getConfig().getDouble("animation.claim.rotation", 0.4);
      final double yOffset = this.getConfig().getDouble("animation.claim.y-offset", 0.2);
      final boolean burstEnabled = this.getConfig().getBoolean("animation.claim.burst.enabled", true);
      final int burstCount = Math.max(0, this.getConfig().getInt("animation.claim.burst.count", 30));
      final double burstOffset = this.getConfig().getDouble("animation.claim.burst.offset-y", (double)1.0F);
      final double burstSpread = this.getConfig().getDouble("animation.claim.burst.spread", 0.4);
      final double burstSpeed = this.getConfig().getDouble("animation.claim.burst.speed", 0.02);
      final World world = player.getWorld();
      (new BukkitRunnable() {
        int tick = 0;

        public void run() {
          if (!player.isOnline()) {
            this.cancel();
          } else {
            Location base = player.getLocation();
            double y = yOffset + height * ((double)this.tick / (double)duration);

            for(int i = 0; i < points; ++i) {
              double angle = (Math.PI * 2D) * (double)i / (double)points + rotation * (double)this.tick;
              double x = Math.cos(angle) * radius;
              double z = Math.sin(angle) * radius;
              Location loc = base.clone().add(x, y, z);
              world.spawnParticle(particle, loc, 0, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            }

            if (this.tick++ >= duration) {
              if (burstEnabled && burstCount > 0) {
                Location center = base.clone().add((double)0.0F, burstOffset, (double)0.0F);
                world.spawnParticle(particle, center, burstCount, burstSpread, burstSpread, burstSpread, burstSpeed);
              }

              this.cancel();
            }

          }
        }
      }).runTaskTimer(this, 0L, 1L);
    }
  }

  private BarColor parseBarColor(String value) {
    if (value == null) {
      return BarColor.GREEN;
    } else {
      try {
        return BarColor.valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var3) {
        return BarColor.GREEN;
      }
    }
  }

  private BarStyle parseBarStyle(String value) {
    if (value == null) {
      return BarStyle.SOLID;
    } else {
      try {
        return BarStyle.valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException var3) {
        return BarStyle.SOLID;
      }
    }
  }
}
