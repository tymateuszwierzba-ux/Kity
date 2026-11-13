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
import pl.tymek24.fortcodekits.integration.EconomyHook;
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
import pl.tymek24.fortcodekits.service.FavoriteManager;
import pl.tymek24.fortcodekits.service.KitManager;
import pl.tymek24.fortcodekits.service.PlayerSettingsManager;
import pl.tymek24.fortcodekits.service.StatsManager;
import pl.tymek24.fortcodekits.util.ColorUtil;
import pl.tymek24.fortcodekits.util.TimeUtil;

public class FortCodeKits extends JavaPlugin {
  private DatabaseManager databaseManager;
  private KitManager kitManager;
  private CooldownManager cooldownManager;
  private StatsManager statsManager;
  private FavoriteManager favoriteManager;
  private PlayerSettingsManager playerSettingsManager;
  private GUIManager guiManager;
  private ChatInputListener chatInputListener;
  private TextAnimationManager textAnimationManager;
  private KitClaimAnimationManager kitClaimAnimationManager;
  private LicenseGuard licenseGuard;
  private FileConfiguration messages;
  private final Map<UUID, BukkitTask> activeClaimActionbarTasks = new ConcurrentHashMap<UUID, BukkitTask>();
  private final Map<UUID, BukkitTask> activeClaimBossbarTasks = new ConcurrentHashMap<UUID, BukkitTask>();
  private final Map<UUID, BossBar> activeClaimBossbars = new ConcurrentHashMap<UUID, BossBar>();
  private BukkitTask autosaveTask;
  private BukkitTask welcomeTask;
  private final Map<UUID, java.util.Set<String>> welcomed =
      new ConcurrentHashMap<UUID, java.util.Set<String>>();

  public void onEnable() {
    this.saveDefaultConfig();
    this.saveResourceIfMissing("kits.yml");
    this.saveResourceIfMissing("messages.yml");
    this.saveResourceIfMissing("gui.yml");
    this.saveResourceIfMissing("animations.yml");
    this.saveResourceIfMissing("animations2.yml");
    this.saveResourceIfMissing("license.yml");
    this.saveResourceIfMissing("favorites.yml");
    this.saveResourceIfMissing("stats.yml");
    this.saveResourceIfMissing("playersettings.yml");
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
    this.statsManager = new StatsManager(this);
    this.favoriteManager = new FavoriteManager(this);
    this.playerSettingsManager = new PlayerSettingsManager(this);
    this.kitManager = new KitManager(this);
    this.kitManager.load();
    this.cooldownManager = new CooldownManager(this.databaseManager, this.statsManager);
    this.guiManager = new GUIManager(this);
    this.guiManager.startAutoRefresh();
    this.chatInputListener = new ChatInputListener(this);
    Bukkit.getPluginManager().registerEvents(new InventoryClickListener(this), this);
    Bukkit.getPluginManager().registerEvents(this.chatInputListener, this);
    Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this), this);
    new CommandManager(this);
    EconomyHook.init();
    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new KitsPlaceholderExpansion(this).register();
      this.getLogger().info("Hook PlaceholderAPI aktywny.");
    }
    this.getLogger().info("Vault integration: " + (VaultHook.isVaultAvailable() ? "ON" : "OFF"));
    this.getLogger().info("Economy integration: " + (EconomyHook.isReady() ? "ON" : "OFF"));
    this.getLogger().info("LuckPerms integration: " + (LuckPermsHook.isLuckPermsAvailable() ? "ON" : "OFF"));
    this.startAutosave();
    this.scheduleWelcomeKits();
    this.getLogger().info("FORT-CODE KITS v" + this.getDescription().getVersion() + " zaladowany.");
  }

  public void onDisable() {
    this.clearAllClaimFeedbackVisuals();
    if (this.welcomeTask != null) {
      this.welcomeTask.cancel();
      this.welcomeTask = null;
    }
    if (this.autosaveTask != null) {
      this.autosaveTask.cancel();
      this.autosaveTask = null;
    }
    if (this.statsManager != null) {
      this.statsManager.save();
    }
    if (this.favoriteManager != null) {
      this.favoriteManager.save();
    }
    if (this.playerSettingsManager != null) {
      this.playerSettingsManager.save();
    }
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

  private void startAutosave() {
    long interval = Math.max(20L, this.getConfig().getInt("autosave.interval-seconds", 120) * 20L);
    this.autosaveTask =
        Bukkit.getScheduler()
            .runTaskTimer(
                this,
                () -> {
                  if (this.statsManager != null) {
                    this.statsManager.autosave();
                  }
                },
                interval,
                interval);
  }

  private void scheduleWelcomeKits() {
    if (!this.getConfig().getBoolean("welcome.enabled", true)) {
      return;
    }
    long delay = Math.max(1L, this.getConfig().getInt("welcome.delay-ticks", 60));
    this.welcomeTask =
        Bukkit.getScheduler()
            .runTaskTimer(
                this,
                () -> {
                  for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.hasPermission("fortcode.kits.use")) continue;
                    java.util.Set<String> given =
                        this.welcomed.computeIfAbsent(
                            player.getUniqueId(), key -> new java.util.HashSet<String>());
                    for (Kit kit : this.kitManager.getKits()) {
                      if (!kit.isWelcome() || !kit.isEnabled()) continue;
                      if (given.contains(kit.getId())) continue;
                      if (!this.hasKitPermission(player, kit)) continue;
                      if (!this.cooldownManager.canClaim(player, kit)) continue;
                      if (!this.hasInventorySpace(player, kit.getItems())) continue;
                      this.giveKit(player, kit, false);
                      given.add(kit.getId());
                    }
                  }
                },
                delay,
                Math.max(delay, this.getConfig().getInt("welcome.interval-ticks", 12000)));
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
    if (this.favoriteManager != null) {
      this.favoriteManager.reload();
    }
    EconomyHook.init();
    this.guiManager.reload();
    this.guiManager.restartAutoRefresh();
    this.kitManager.load();
  }

  public void giveKit(Player player, Kit kit, boolean bypassCooldown) {
    if (this.licenseGuard != null && !this.licenseGuard.isValid()) {
      this.clearClaimFeedbackVisual(player);
      player.sendMessage(this.formatText("&cLicencja pluginu jest niepoprawna."));
      return;
    }
    if (!bypassCooldown && !player.hasPermission("fortcode.kits.use")) {
      this.clearClaimFeedbackVisual(player);
      this.sendMessage(player, "no-permission");
      this.playConfiguredSound(player, "sounds.error");
      return;
    }
    if (!bypassCooldown && !this.hasKitPermission(player, kit)) {
      this.clearClaimFeedbackVisual(player);
      this.sendMessage(player, "no-permission");
      this.playConfiguredSound(player, "sounds.error");
      return;
    }
    if (!bypassCooldown && !kit.isEnabled()) {
      this.clearClaimFeedbackVisual(player);
      this.sendMessage(player, "kit-disabled", Map.of("{kit}", kit.getDisplayName()));
      this.playConfiguredSound(player, "sounds.error");
      return;
    }
    if (!bypassCooldown) {
      String deny = this.cooldownManager.denyReason(player, kit);
      if (deny.equals("cooldown")) {
        this.clearClaimFeedbackVisual(player);
        long remaining = this.cooldownManager.getRemaining(player.getUniqueId(), kit.getId());
        Map<String, String> ph = new java.util.HashMap<String, String>();
        ph.put("{kit}", kit.getDisplayName());
        ph.put("{time}", TimeUtil.formatDuration(remaining));
        this.sendMessage(player, "cooldown", ph);
        this.playConfiguredSound(player, "sounds.error");
        return;
      }
      if (deny.equals("one-time")) {
        this.clearClaimFeedbackVisual(player);
        this.sendMessage(player, "already-claimed", Map.of("{kit}", kit.getDisplayName()));
        this.playConfiguredSound(player, "sounds.error");
        return;
      }
      if (deny.equals("claim-limit")) {
        this.clearClaimFeedbackVisual(player);
        int used = this.statsManager.getPlayerKitClaims(player.getUniqueId(), kit.getId());
        Map<String, String> ph = new java.util.HashMap<String, String>();
        ph.put("{kit}", kit.getDisplayName());
        ph.put("{limit}", String.valueOf(kit.getClaimCountLimit()));
        ph.put("{used}", String.valueOf(used));
        this.sendMessage(player, "claim-limit", ph);
        this.playConfiguredSound(player, "sounds.error");
        return;
      }
    }
    if (!this.hasInventorySpace(player, kit.getItems())) {
      this.clearClaimFeedbackVisual(player);
      this.sendMessage(player, "inventory-full");
      this.playConfiguredSound(player, "sounds.error");
      return;
    }
    double price = kit.getPrice();
    if (!bypassCooldown && price > 0.0D && EconomyHook.isReady()) {
      if (!EconomyHook.has(player, price)) {
        this.clearClaimFeedbackVisual(player);
        Map<String, String> ph = new java.util.HashMap<String, String>();
        ph.put("{price}", this.formatPrice(price));
        ph.put("{balance}", this.formatEconomyBalance(player));
        this.sendMessage(player, "not-enough-money", ph);
        this.playConfiguredSound(player, "sounds.error");
        return;
      }
      if (!EconomyHook.withdraw(player, price)) {
        this.clearClaimFeedbackVisual(player);
        this.sendMessage(player, "payment-failed");
        this.playConfiguredSound(player, "sounds.error");
        return;
      }
    }

    boolean animate =
        this.playerSettingsManager.get(player.getUniqueId(), PlayerSettingsManager.KEY_CLAIM_ANIMATION);
    if (this.kitClaimAnimationManager != null && animate) {
      this.kitClaimAnimationManager.giveItems(player, kit);
    } else {
      for (ItemStack item : kit.getItems()) {
        player.getInventory().addItem(item.clone());
      }
    }

    if (!bypassCooldown) {
      this.cooldownManager.recordClaim(player.getUniqueId(), kit);
    }
    this.statsManager.recordClaim(player.getUniqueId(), player.getName(), kit.getId());
    this.statsManager.save();

    Map<String, String> claimPh = new java.util.HashMap<String, String>();
    claimPh.put("{kit}", kit.getDisplayName());
    claimPh.put("{price}", this.formatPrice(price));
    if (!bypassCooldown && price > 0.0D && EconomyHook.isReady()) {
      this.sendMessage(player, "economy-charged", claimPh);
    }
    this.sendClaimFeedback(player, kit);
    this.playConfiguredSound(player, "sounds.claim");
    this.playClaimAnimation(player);
    if (this.guiManager != null) {
      this.guiManager.refreshPlayerIfOpen(player);
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
    this.playConfiguredSound(player, "sounds.click");
  }

  public boolean hasInventorySpace(Player player, List<ItemStack> items) {
    ItemStack[] simulated = player.getInventory().getStorageContents().clone();
    for (int i = 0; i < simulated.length; ++i) {
      if (simulated[i] != null) {
        simulated[i] = simulated[i].clone();
      }
    }
    int inventoryMaxStack = Math.max(1, player.getInventory().getMaxStackSize());
    for (ItemStack rawItem : items) {
      if (rawItem == null || rawItem.getType() == Material.AIR) continue;
      ItemStack item = rawItem.clone();
      int maxStack = Math.max(1, Math.min(inventoryMaxStack, item.getMaxStackSize()));
      int remaining = Math.max(1, item.getAmount());
      for (int slot = 0; slot < simulated.length && remaining > 0; ++slot) {
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
      while (remaining > 0) {
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
    return true;
  }

  private int findFirstEmptyStorageSlot(ItemStack[] contents) {
    for (int i = 0; i < contents.length; ++i) {
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
      YamlConfiguration defaults =
          YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
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
    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
      raw = raw.replace(entry.getKey(), entry.getValue());
    }
    return this.formatText(raw);
  }

  public String msgRaw(String path) {
    return this.messages.getString("messages." + path, "");
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
    List<ItemStack> items = new ArrayList<ItemStack>();
    for (ItemStack stack : player.getInventory().getStorageContents()) {
      if (stack != null && stack.getType() != Material.AIR) {
        items.add(stack.clone());
      }
    }
    return items;
  }

  public void playConfiguredSound(Player player, String path) {
    if (!this.playerSettingsManager.get(player.getUniqueId(), PlayerSettingsManager.KEY_SOUNDS)) {
      return;
    }
    String name = this.getConfig().getString(path, "").trim();
    if (!name.isEmpty()) {
      try {
        Sound sound = Sound.valueOf(name.toUpperCase(Locale.ROOT));
        player.playSound(player.getLocation(), sound, 1.0F, 1.0F);
      } catch (IllegalArgumentException ignored) {
      }
    }
  }

  public boolean hasKitPermission(Player player, Kit kit) {
    if (!this.hasRequiredGroup(player, kit)) {
      return false;
    }
    String kitPerm = "fortcode.kits.kit." + kit.getId().toLowerCase(Locale.ROOT);
    return player.hasPermission("fortcode.kits.admin")
        || player.hasPermission("fortcode.kits.*")
        || player.hasPermission("fortcode.kits.kit.*")
        || player.hasPermission(kitPerm);
  }

  private boolean hasRequiredGroup(Player player, Kit kit) {
    String requiredGroup = kit.getRequiredGroup();
    if (requiredGroup != null && !requiredGroup.isBlank()) {
      if (LuckPermsHook.isLuckPermsAvailable()) {
        return LuckPermsHook.isUserInGroup(player, requiredGroup);
      }
      return player.hasPermission("group." + requiredGroup)
          || player.hasPermission("luckperms.group." + requiredGroup);
    }
    return true;
  }

  public void clearWelcomeCache(UUID uuid) {
    this.welcomed.remove(uuid);
  }

  public String formatPrice(double price) {
    if (price <= 0.0D) {
      return this.msgRaw("price-free").isEmpty() ? "ZA DARMO" : this.formatText(this.msgRaw("price-free"));
    }
    if (EconomyHook.isReady()) {
      return EconomyHook.format(price);
    }
    return String.format("%.2f", price);
  }

  public String formatEconomyBalance(Player player) {
    if (!EconomyHook.isReady()) {
      return "-";
    }
    return EconomyHook.format(EconomyHook.getBalance(player));
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

  public StatsManager getStatsManager() {
    return this.statsManager;
  }

  public FavoriteManager getFavoriteManager() {
    return this.favoriteManager;
  }

  public PlayerSettingsManager getPlayerSettingsManager() {
    return this.playerSettingsManager;
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
    }
    String animated = this.textAnimationManager == null ? raw : this.textAnimationManager.apply(raw);
    return ColorUtil.colorize(animated);
  }

  public void clearClaimFeedbackVisual(Player player) {
    if (player == null) {
      return;
    }
    UUID uuid = player.getUniqueId();
    BukkitTask actionbarTask = this.activeClaimActionbarTasks.remove(uuid);
    if (actionbarTask != null) {
      actionbarTask.cancel();
    }
    BukkitTask bossbarTask = this.activeClaimBossbarTasks.remove(uuid);
    if (bossbarTask != null) {
      bossbarTask.cancel();
    }
    BossBar bar = this.activeClaimBossbars.remove(uuid);
    if (bar != null) {
      bar.removePlayer(player);
      bar.removeAll();
    }
  }

  private void clearAllClaimFeedbackVisuals() {
    for (BukkitTask task : this.activeClaimActionbarTasks.values()) {
      if (task != null) {
        task.cancel();
      }
    }
    this.activeClaimActionbarTasks.clear();
    for (BukkitTask task : this.activeClaimBossbarTasks.values()) {
      if (task != null) {
        task.cancel();
      }
    }
    this.activeClaimBossbarTasks.clear();
    for (BossBar bar : this.activeClaimBossbars.values()) {
      if (bar != null) {
        bar.removeAll();
      }
    }
    this.activeClaimBossbars.clear();
  }

  public void sendClaimFeedback(Player player, Kit kit) {
    if (!this.playerSettingsManager.get(player.getUniqueId(), PlayerSettingsManager.KEY_TITLE_FEEDBACK)
        && !this.playerSettingsManager.get(
            player.getUniqueId(), PlayerSettingsManager.KEY_BOSSBAR_FEEDBACK)
        && !this.playerSettingsManager.get(
            player.getUniqueId(), PlayerSettingsManager.KEY_ACTIONBAR_FEEDBACK)) {
      boolean chatOnly =
          this.messages.getStringList("messages.kit-received-channels").contains("chat")
              || this.messages.getString("messages.kit-received-type", "default").equalsIgnoreCase("chat");
      if (!chatOnly) {
        return;
      }
    }
    List<String> channels = this.resolveClaimChannels(kit, player);
    if (!channels.isEmpty()) {
      this.clearClaimFeedbackVisual(player);
      Map<String, String> placeholders = Map.of("{kit}", kit.getDisplayName());
      this.sendConfiguredMessage(player, "kit-received", channels, placeholders);
    }
  }

  private List<String> resolveClaimChannels(Kit kit, Player player) {
    List<String> raw = new ArrayList<String>();
    boolean override = kit != null && kit.getFeedbackType() != null && !kit.getFeedbackType().isBlank();
    if (override) {
      for (String part : kit.getFeedbackType().split(",")) {
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
    boolean[] noneSelected = new boolean[] {false};
    List<String> normalized = this.normalizeChannels(raw, noneSelected);
    if (noneSelected[0]) {
      return List.of();
    }
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
    UUID uuid = player.getUniqueId();
    List<String> filtered = new ArrayList<String>();
    for (String channel : normalized) {
      if (channel.equals("title")
          && !this.playerSettingsManager.get(uuid, PlayerSettingsManager.KEY_TITLE_FEEDBACK)) {
        continue;
      }
      if (channel.equals("bossbar")
          && !this.playerSettingsManager.get(uuid, PlayerSettingsManager.KEY_BOSSBAR_FEEDBACK)) {
        continue;
      }
      if (channel.equals("actionbar")
          && !this.playerSettingsManager.get(uuid, PlayerSettingsManager.KEY_ACTIONBAR_FEEDBACK)) {
        continue;
      }
      filtered.add(channel);
    }
    return filtered.isEmpty() ? List.of("chat") : filtered;
  }

  private List<String> getMessageChannelsRaw(String key) {
    if (this.messages == null) {
      return List.of();
    }
    List<String> list = new ArrayList<String>();
    for (String entry : this.messages.getStringList("messages." + key + "-channels")) {
      if (entry != null) {
        String value = entry.trim();
        if (!value.isEmpty()) {
          list.add(value);
        }
      }
    }
    if (!list.isEmpty()) {
      return list;
    }
    String single = this.messages.getString("messages." + key + "-type", "").trim();
    if (!single.isEmpty() && !single.equalsIgnoreCase("default")) {
      String[] parts = single.split(",");
      for (String part : parts) {
        String value = part.trim();
        if (!value.isEmpty()) {
          list.add(value);
        }
      }
      return list;
    }
    return List.of();
  }

  private List<String> getMessageChannels(String key) {
    List<String> raw = new ArrayList<String>(this.getMessageChannelsRaw(key));
    if (raw.isEmpty()) {
      raw = List.of("chat");
    }
    boolean[] noneSelected = new boolean[] {false};
    List<String> normalized = this.normalizeChannels(raw, noneSelected);
    if (noneSelected[0]) {
      return List.of();
    }
    return normalized.isEmpty() ? List.of("chat") : normalized;
  }

  private void sendConfiguredMessage(
      final Player player, String key, List<String> channels, Map<String, String> placeholders) {
    UUID uuid = player.getUniqueId();
    if (channels.contains("chat")) {
      player.sendMessage(this.msg(key, placeholders));
    }
    if (channels.contains("title")
        && this.playerSettingsManager.get(uuid, PlayerSettingsManager.KEY_TITLE_FEEDBACK)) {
      String title = this.getMessageOptional(key + "-title", key, placeholders);
      String subtitle = this.getMessageOptional(key + "-subtitle", "", placeholders);
      if (!title.isBlank() || !subtitle.isBlank()) {
        int fadeIn = Math.max(0, this.getConfig().getInt("feedback.claim.title.fade-in", 10));
        int stay = Math.max(0, this.getConfig().getInt("feedback.claim.title.stay", 40));
        int fadeOut = Math.max(0, this.getConfig().getInt("feedback.claim.title.fade-out", 10));
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
      }
    }
    if (channels.contains("actionbar")
        && this.playerSettingsManager.get(uuid, PlayerSettingsManager.KEY_ACTIONBAR_FEEDBACK)) {
      final String actionRaw = this.getMessageRawOptional(key + "-actionbar", key, placeholders);
      if (!actionRaw.isBlank()) {
        boolean dynamic =
            this.shouldRunDynamicClaimText(key, "actionbar") && this.hasDynamicAnimationTag(actionRaw);
        BukkitTask previous = this.activeClaimActionbarTasks.remove(uuid);
        if (previous != null) {
          previous.cancel();
        }
        if (dynamic) {
          final int duration = this.getClaimActionbarAnimationDurationTicks();
          final int interval = this.getClaimMessageAnimationIntervalTicks();
          BukkitTask task =
              new BukkitRunnable() {
                int remaining = duration;

                @Override
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
              }.runTaskTimer(this, 0L, (long) interval);
          this.activeClaimActionbarTasks.put(uuid, task);
        } else {
          player.sendActionBar(this.formatText(actionRaw));
        }
      }
    }
    if (channels.contains("bossbar")
        && this.playerSettingsManager.get(uuid, PlayerSettingsManager.KEY_BOSSBAR_FEEDBACK)) {
      final String textRaw = this.getMessageRawOptional(key + "-bossbar", key, placeholders);
      if (textRaw.isBlank()) {
        return;
      }
      BukkitTask previousTask = this.activeClaimBossbarTasks.remove(uuid);
      if (previousTask != null) {
        previousTask.cancel();
      }
      BossBar previousBar = this.activeClaimBossbars.remove(uuid);
      if (previousBar != null) {
        previousBar.removePlayer(player);
        previousBar.removeAll();
      }
      String colorRaw = this.getConfig().getString("feedback.claim.bossbar.color", "GREEN");
      String styleRaw = this.getConfig().getString("feedback.claim.bossbar.style", "SOLID");
      final int duration = Math.max(10, this.getConfig().getInt("feedback.claim.bossbar.duration-ticks", 60));
      final int interval = this.getClaimMessageAnimationIntervalTicks();
      final boolean dynamic =
          this.shouldRunDynamicClaimText(key, "bossbar") && this.hasDynamicAnimationTag(textRaw);
      BarColor color = this.parseBarColor(colorRaw);
      BarStyle style = this.parseBarStyle(styleRaw);
      final BossBar bar = Bukkit.createBossBar(this.formatText(textRaw), color, style, new BarFlag[0]);
      bar.addPlayer(player);
      bar.setProgress(1.0F);
      this.activeClaimBossbars.put(uuid, bar);
      BukkitTask task =
          new BukkitRunnable() {
            int remaining = duration;
            int tick = 0;

            @Override
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
                double progress = Math.max(0.0F, (double) this.remaining / (double) duration);
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
          }.runTaskTimer(this, 0L, 1L);
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
      for (Map.Entry<String, String> entry : placeholders.entrySet()) {
        raw = raw.replace(entry.getKey(), entry.getValue());
      }
      return raw;
    }
    return "";
  }

  private String getMessageOptional(String key, String fallbackKey, Map<String, String> placeholders) {
    return this.formatText(this.getMessageRawOptional(key, fallbackKey, placeholders));
  }

  private boolean shouldRunDynamicClaimText(String key, String channel) {
    if (!"kit-received".equalsIgnoreCase(key)) {
      return false;
    }
    if (!this.getConfig().getBoolean("feedback.claim.message-animation.enabled", true)) {
      return false;
    }
    List<String> channels = this.getConfig().getStringList("feedback.claim.message-animation.channels");
    if (channels.isEmpty()) {
      channels = List.of("actionbar", "bossbar");
    }
    String requested = channel.toLowerCase(Locale.ROOT);
    for (String value : channels) {
      if (value != null && value.trim().toLowerCase(Locale.ROOT).equals(requested)) {
        return true;
      }
    }
    return false;
  }

  private int getClaimMessageAnimationIntervalTicks() {
    return Math.max(1, this.getConfig().getInt("feedback.claim.message-animation.interval-ticks", 1));
  }

  private int getClaimActionbarAnimationDurationTicks() {
    return Math.max(
        1, this.getConfig().getInt("feedback.claim.message-animation.actionbar-duration-ticks", 40));
  }

  private boolean hasDynamicAnimationTag(String value) {
    return value != null && value.toLowerCase(Locale.ROOT).contains("<anim:");
  }

  private List<String> normalizeChannels(List<String> raw, boolean[] noneSelected) {
    List<String> allowed = List.of("chat", "title", "actionbar", "bossbar");
    List<String> result = new ArrayList<String>();
    boolean all = false;
    for (String entry : raw) {
      if (entry == null) continue;
      String value = entry.trim().toLowerCase(Locale.ROOT);
      if (value.isEmpty()) continue;
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
    if (all) {
      return new ArrayList<String>(allowed);
    }
    return result;
  }

  public void playClaimAnimation(final Player player) {
    if (!this.playerSettingsManager.get(player.getUniqueId(), PlayerSettingsManager.KEY_PARTICLES)) {
      return;
    }
    if (!this.getConfig().getBoolean("animation.claim.enabled", true)) {
      return;
    }
    String particleName = this.getConfig().getString("animation.claim.particle", "FIREWORKS_SPARK");
    final Particle particle;
    try {
      particle = Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
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
    final double burstOffset = this.getConfig().getDouble("animation.claim.burst.offset-y", 1.0F);
    final double burstSpread = this.getConfig().getDouble("animation.claim.burst.spread", 0.4);
    final double burstSpeed = this.getConfig().getDouble("animation.claim.burst.speed", 0.02);
    final World world = player.getWorld();
    new BukkitRunnable() {
      int tick = 0;

      @Override
      public void run() {
        if (!player.isOnline()) {
          this.cancel();
          return;
        }
        Location base = player.getLocation();
        double y = yOffset + height * ((double) this.tick / (double) duration);
        for (int i = 0; i < points; ++i) {
          double angle = (Math.PI * 2.0D) * (double) i / (double) points + rotation * (double) this.tick;
          double x = Math.cos(angle) * radius;
          double z = Math.sin(angle) * radius;
          Location loc = base.clone().add(x, y, z);
          world.spawnParticle(particle, loc, 0, 0.0F, 0.0F, 0.0F, 0.0F);
        }
        if (this.tick++ >= duration) {
          if (burstEnabled && burstCount > 0) {
            Location center = base.clone().add(0.0F, burstOffset, 0.0F);
            world.spawnParticle(particle, center, burstCount, burstSpread, burstSpread, burstSpread, burstSpeed);
          }
          this.cancel();
        }
      }
    }.runTaskTimer(this, 0L, 1L);
  }

  private BarColor parseBarColor(String value) {
    if (value == null) {
      return BarColor.GREEN;
    }
    try {
      return BarColor.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return BarColor.GREEN;
    }
  }

  private BarStyle parseBarStyle(String value) {
    if (value == null) {
      return BarStyle.SOLID;
    }
    try {
      return BarStyle.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return BarStyle.SOLID;
    }
  }
}
