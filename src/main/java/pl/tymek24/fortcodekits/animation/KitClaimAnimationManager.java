package pl.tymek24.fortcodekits.animation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.model.Kit;

public class KitClaimAnimationManager {
  private final FortCodeKits plugin;
  private final File file;
  private final NamespacedKey markerKey;
  private final NamespacedKey hadHideFlagKey;
  private final NamespacedKey hadGlowEnchantKey;
  private final NamespacedKey glowEnchantKey;
  private final Map<UUID, BukkitTask> activeGiveTasks = new ConcurrentHashMap<>();
  private final Map<UUID, BukkitTask> activeCleanupTasks = new ConcurrentHashMap<>();

  private boolean enabled;
  private ClaimAnimationMode mode;
  private int intervalTicks;
  private int intervalJitterTicks;
  private int batchSize;
  private int startDelayTicks;
  private boolean splitStacks;
  private int maxItems;
  private boolean temporaryGlowEnabled;
  private int temporaryGlowRemoveAfterTicks;
  private String temporaryGlowEnchantmentRaw;
  private Enchantment temporaryGlowEnchantment;

  public KitClaimAnimationManager(FortCodeKits plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "animations2.yml");
    this.markerKey = new NamespacedKey(plugin, "anim2_temp_glow");
    this.hadHideFlagKey = new NamespacedKey(plugin, "anim2_had_hide_flag");
    this.hadGlowEnchantKey = new NamespacedKey(plugin, "anim2_had_glow_enchant");
    this.glowEnchantKey = new NamespacedKey(plugin, "anim2_glow_enchant");
    this.reload();
  }

  public static List<String> getSupportedModeNames() {
    return ClaimAnimationMode.names();
  }

  public static boolean isSupportedMode(String rawMode) {
    if (rawMode == null || rawMode.isBlank()) {
      return false;
    }
    return ClaimAnimationMode.names().contains(ClaimAnimationMode.from(rawMode).key());
  }

  public void reload() {
    if (!this.file.exists()) {
      this.plugin.saveResource("animations2.yml", false);
    }

    YamlConfiguration loaded = YamlConfiguration.loadConfiguration(this.file);
    InputStream resource = this.plugin.getResource("animations2.yml");
    if (resource != null) {
      YamlConfiguration defaults =
          YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
      loaded.setDefaults(defaults);
      loaded.options().copyDefaults(true);
      try {
        loaded.save(this.file);
      } catch (IOException ex) {
        this.plugin.getLogger().warning("Nie udalo sie zapisac animations2.yml: " + ex.getMessage());
      }
    }

    this.enabled = loaded.getBoolean("enabled", true);
    this.mode = ClaimAnimationMode.from(loaded.getString("claim.mode", "forward"));
    this.intervalTicks = Math.max(1, loaded.getInt("claim.interval-ticks", 2));
    this.intervalJitterTicks = Math.max(0, loaded.getInt("claim.interval-jitter-ticks", 0));
    this.batchSize = Math.max(1, loaded.getInt("claim.batch-size", 1));
    this.startDelayTicks = Math.max(0, loaded.getInt("claim.start-delay-ticks", 0));
    this.splitStacks = loaded.getBoolean("claim.split-stacks", false);
    int loadedMaxItems = loaded.getInt("claim.max-items", 0);
    this.maxItems = loadedMaxItems <= 0 ? 0 : loadedMaxItems;
    this.temporaryGlowEnabled = loaded.getBoolean("claim.temporary-glow.enabled", false);
    this.temporaryGlowRemoveAfterTicks =
        Math.max(1, loaded.getInt("claim.temporary-glow.remove-after-ticks", 20));
    this.temporaryGlowEnchantmentRaw =
        normalizeEnchantRaw(loaded.getString("claim.temporary-glow.enchantment"), true);
    this.temporaryGlowEnchantment = this.resolveEnchantment(this.temporaryGlowEnchantmentRaw, true);
  }

  public void shutdown() {
    for (BukkitTask task : this.activeGiveTasks.values()) {
      if (task != null) {
        task.cancel();
      }
    }
    this.activeGiveTasks.clear();

    for (BukkitTask task : this.activeCleanupTasks.values()) {
      if (task != null) {
        task.cancel();
      }
    }
    this.activeCleanupTasks.clear();
  }

  public void clearPlayer(Player player) {
    if (player == null) {
      return;
    }
    UUID uuid = player.getUniqueId();
    this.cancelTask(this.activeGiveTasks, uuid);
    this.cancelTask(this.activeCleanupTasks, uuid);
    this.cleanupTemporaryGlow(player);
  }

  public void giveItems(Player player, Kit kit) {
    if (player == null || kit == null) {
      return;
    }

    ClaimAnimationSettings settings = this.resolveSettings(kit);
    List<ItemStack> prepared = this.prepareItems(kit.getItems(), settings);
    if (prepared.isEmpty()) {
      return;
    }

    UUID uuid = player.getUniqueId();
    this.cancelTask(this.activeGiveTasks, uuid);

    Runnable runGive = () -> this.giveItemsNow(player, uuid, prepared, settings);
    if (settings.startDelayTicks() > 0) {
      BukkitTask delayed =
          this.plugin
              .getServer()
              .getScheduler()
              .runTaskLater(
                  this.plugin,
                  () -> {
                    this.activeGiveTasks.remove(uuid);
                    runGive.run();
                  },
                  settings.startDelayTicks());
      this.activeGiveTasks.put(uuid, delayed);
      return;
    }

    runGive.run();
  }

  private void giveItemsNow(Player player, UUID uuid, List<ItemStack> prepared, ClaimAnimationSettings settings) {
    if (!player.isOnline()) {
      return;
    }

    if (!settings.enabled() || settings.mode() == ClaimAnimationMode.ALL_AT_ONCE) {
      for (ItemStack item : prepared) {
        this.giveSingleItem(player, item, settings);
      }
      return;
    }

    List<ItemStack> ordered = this.order(prepared, settings.mode());
    this.scheduleNextBatch(player, uuid, ordered, settings, 0, true);
  }

  private void scheduleNextBatch(
      Player player,
      UUID uuid,
      List<ItemStack> ordered,
      ClaimAnimationSettings settings,
      int index,
      boolean firstRun) {
    if (index >= ordered.size()) {
      this.activeGiveTasks.remove(uuid);
      return;
    }

    long delay = firstRun ? 0L : settings.nextIntervalTicks();
    BukkitTask task =
        this.plugin
            .getServer()
            .getScheduler()
            .runTaskLater(
                this.plugin,
                () -> {
                  if (!player.isOnline()) {
                    this.activeGiveTasks.remove(uuid);
                    return;
                  }

                  int end = Math.min(index + settings.batchSize(), ordered.size());
                  for (int i = index; i < end; i++) {
                    this.giveSingleItem(player, ordered.get(i), settings);
                  }

                  this.scheduleNextBatch(player, uuid, ordered, settings, end, false);
                },
                delay);
    this.activeGiveTasks.put(uuid, task);
  }

  private List<ItemStack> prepareItems(List<ItemStack> items, ClaimAnimationSettings settings) {
    if (items == null || items.isEmpty()) {
      return List.of();
    }

    List<ItemStack> prepared = new ArrayList<>();
    for (ItemStack source : items) {
      if (source == null || source.getType() == Material.AIR) {
        continue;
      }

      ItemStack copy = source.clone();
      if (settings.splitStacks() && copy.getAmount() > 1) {
        int amount = copy.getAmount();
        for (int i = 0; i < amount; i++) {
          ItemStack single = copy.clone();
          single.setAmount(1);
          prepared.add(single);
        }
      } else {
        prepared.add(copy);
      }

      if (settings.maxItems() > 0 && prepared.size() >= settings.maxItems()) {
        break;
      }
    }

    if (settings.maxItems() > 0 && prepared.size() > settings.maxItems()) {
      return new ArrayList<>(prepared.subList(0, settings.maxItems()));
    }

    return prepared;
  }

  private List<ItemStack> order(List<ItemStack> items, ClaimAnimationMode order) {
    List<Integer> indexes = this.buildOrderIndexes(items.size(), order);
    List<ItemStack> ordered = new ArrayList<>(indexes.size());
    for (int index : indexes) {
      ordered.add(items.get(index));
    }
    return ordered;
  }

  private List<Integer> buildOrderIndexes(int size, ClaimAnimationMode order) {
    if (size <= 1 || order == ClaimAnimationMode.FORWARD) {
      return this.forwardIndexes(size);
    }

    switch (order) {
      case REVERSE:
        return this.reverseIndexes(size);
      case RANDOM:
        return this.randomIndexes(size);
      case EVEN_THEN_ODD:
        return this.byModuloIndexes(size, 2, new int[] {0, 1});
      case ODD_THEN_EVEN:
        return this.byModuloIndexes(size, 2, new int[] {1, 0});
      case CENTER_OUT:
        return this.centerOutIndexes(size);
      case OUTSIDE_IN:
        return this.outsideInIndexes(size);
      case EDGE_ALTERNATE:
        return this.edgeAlternateIndexes(size, false);
      case EDGE_ALTERNATE_REVERSE:
        return this.edgeAlternateIndexes(size, true);
      case SECOND_HALF_THEN_FIRST:
        return this.secondHalfThenFirstIndexes(size);
      case WAVES_2:
        return this.byModuloIndexes(size, 2, new int[] {0, 1});
      case WAVES_3:
        return this.byModuloIndexes(size, 3, new int[] {0, 1, 2});
      case CHUNKED_RANDOM_2:
        return this.chunkedRandomIndexes(size, 2);
      case CHUNKED_RANDOM_3:
        return this.chunkedRandomIndexes(size, 3);
      case FORWARD_SKIP_ONE:
        return this.forwardSkipOneIndexes(size);
      case REVERSE_SKIP_ONE:
        return this.reverseSkipOneIndexes(size);
      case MIRROR_CENTER:
        return this.mirrorCenterIndexes(size);
      case MIRROR_EDGES:
        return this.mirrorEdgesIndexes(size);
      case RANDOM_WEIGHTED:
        return this.randomWeightedIndexes(size);
      case ALL_AT_ONCE:
      default:
        return this.forwardIndexes(size);
    }
  }

  private List<Integer> forwardIndexes(int size) {
    List<Integer> indexes = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      indexes.add(i);
    }
    return indexes;
  }

  private List<Integer> reverseIndexes(int size) {
    List<Integer> indexes = new ArrayList<>(size);
    for (int i = size - 1; i >= 0; i--) {
      indexes.add(i);
    }
    return indexes;
  }

  private List<Integer> randomIndexes(int size) {
    List<Integer> indexes = this.forwardIndexes(size);
    Collections.shuffle(indexes);
    return indexes;
  }

  private List<Integer> byModuloIndexes(int size, int modulo, int[] offsets) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    for (int offset : offsets) {
      for (int i = offset; i < size; i += modulo) {
        indexes.add(i);
      }
    }
    return this.toListWithFallback(size, indexes);
  }

  private List<Integer> centerOutIndexes(int size) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    int left = (size - 1) / 2;
    int right = size / 2;

    while (left >= 0 || right < size) {
      if (left == right) {
        indexes.add(left);
      } else {
        if (left >= 0) {
          indexes.add(left);
        }
        if (right < size) {
          indexes.add(right);
        }
      }
      left--;
      right++;
    }

    return this.toListWithFallback(size, indexes);
  }

  private List<Integer> outsideInIndexes(int size) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    int left = 0;
    int right = size - 1;
    while (left <= right) {
      indexes.add(left++);
      if (left <= right) {
        indexes.add(right--);
      }
    }
    return this.toListWithFallback(size, indexes);
  }

  private List<Integer> edgeAlternateIndexes(int size, boolean reverseDirection) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    int left = 0;
    int right = size - 1;

    while (left <= right) {
      if (reverseDirection) {
        this.addIfValid(indexes, right--, size);
        this.addIfValid(indexes, right--, size);
        this.addIfValid(indexes, left++, size);
      } else {
        this.addIfValid(indexes, left++, size);
        this.addIfValid(indexes, left++, size);
        this.addIfValid(indexes, right--, size);
      }
    }

    return this.toListWithFallback(size, indexes);
  }

  private List<Integer> secondHalfThenFirstIndexes(int size) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    int half = size / 2;
    for (int i = half; i < size; i++) {
      indexes.add(i);
    }
    for (int i = 0; i < half; i++) {
      indexes.add(i);
    }
    return this.toListWithFallback(size, indexes);
  }

  private List<Integer> chunkedRandomIndexes(int size, int chunkSize) {
    List<List<Integer>> chunks = new ArrayList<>();
    for (int i = 0; i < size; i += chunkSize) {
      List<Integer> chunk = new ArrayList<>();
      int end = Math.min(size, i + chunkSize);
      for (int j = i; j < end; j++) {
        chunk.add(j);
      }
      chunks.add(chunk);
    }

    Collections.shuffle(chunks);
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    for (List<Integer> chunk : chunks) {
      indexes.addAll(chunk);
    }
    return this.toListWithFallback(size, indexes);
  }

  private List<Integer> forwardSkipOneIndexes(int size) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    for (int i = 0; i < size; i += 2) {
      indexes.add(i);
    }
    for (int i = size - 1 - (size % 2); i >= 0; i -= 2) {
      this.addIfValid(indexes, i, size);
    }
    return this.toListWithFallback(size, indexes);
  }

  private List<Integer> reverseSkipOneIndexes(int size) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    for (int i = size - 1; i >= 0; i -= 2) {
      indexes.add(i);
    }
    for (int i = size % 2 == 0 ? 0 : 1; i < size; i += 2) {
      this.addIfValid(indexes, i, size);
    }
    return this.toListWithFallback(size, indexes);
  }

  private List<Integer> mirrorCenterIndexes(int size) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    int left = (size - 1) / 2;
    int right = size / 2;

    while (left >= 0 || right < size) {
      if (left >= 0) {
        indexes.add(left);
      }
      if (right < size && right != left) {
        indexes.add(right);
      }
      left--;
      right++;
    }

    return this.toListWithFallback(size, indexes);
  }

  private List<Integer> mirrorEdgesIndexes(int size) {
    LinkedHashSet<Integer> indexes = new LinkedHashSet<>();
    int left = 0;
    int right = size - 1;

    while (left <= right) {
      indexes.add(left++);
      if (left <= right) {
        indexes.add(right--);
      }
      if (left <= right) {
        indexes.add(right--);
      }
      if (left <= right) {
        indexes.add(left++);
      }
    }

    return this.toListWithFallback(size, indexes);
  }

  private List<Integer> randomWeightedIndexes(int size) {
    List<Integer> indexes = this.forwardIndexes(size);
    double center = (size - 1) / 2.0D;
    indexes.sort(
        (a, b) -> {
          double aw = Math.abs(center - a) * 0.075D + ThreadLocalRandom.current().nextDouble();
          double bw = Math.abs(center - b) * 0.075D + ThreadLocalRandom.current().nextDouble();
          return Double.compare(aw, bw);
        });
    return indexes;
  }

  private void addIfValid(LinkedHashSet<Integer> indexes, int index, int size) {
    if (index >= 0 && index < size) {
      indexes.add(index);
    }
  }

  private List<Integer> toListWithFallback(int size, LinkedHashSet<Integer> source) {
    List<Integer> ordered = new ArrayList<>(source);
    if (ordered.size() == size) {
      return ordered;
    }

    for (int i = 0; i < size; i++) {
      if (!source.contains(i)) {
        ordered.add(i);
      }
    }
    return ordered;
  }

  private void giveSingleItem(Player player, ItemStack source, ClaimAnimationSettings settings) {
    ItemStack toGive = source.clone();
    if (settings.temporaryGlowEnabled()) {
      this.applyTemporaryGlow(toGive, settings.temporaryGlowEnchantment());
    }

    Map<Integer, ItemStack> leftovers = player.getInventory().addItem(toGive);
    if (!leftovers.isEmpty()) {
      for (ItemStack leftover : leftovers.values()) {
        ItemStack clean = leftover.clone();
        this.clearTemporaryGlow(clean);
        player.getWorld().dropItemNaturally(player.getLocation(), clean);
      }
    }

    if (settings.temporaryGlowEnabled()) {
      this.scheduleTemporaryGlowCleanup(player, settings.temporaryGlowRemoveAfterTicks());
    }
  }

  private void scheduleTemporaryGlowCleanup(Player player, int delayTicks) {
    UUID uuid = player.getUniqueId();
    this.cancelTask(this.activeCleanupTasks, uuid);
    BukkitTask task =
        this.plugin
            .getServer()
            .getScheduler()
            .runTaskLater(
                this.plugin,
                () -> {
                  this.cleanupTemporaryGlow(player);
                  this.activeCleanupTasks.remove(uuid);
                },
                delayTicks);
    this.activeCleanupTasks.put(uuid, task);
  }

  private void cleanupTemporaryGlow(Player player) {
    if (player == null) {
      return;
    }

    boolean changed = false;
    ItemStack[] contents = player.getInventory().getContents();
    for (ItemStack stack : contents) {
      if (stack == null || stack.getType() == Material.AIR) {
        continue;
      }
      if (!this.hasTemporaryGlowMarker(stack)) {
        continue;
      }
      this.clearTemporaryGlow(stack);
      changed = true;
    }

    if (changed) {
      player.updateInventory();
    }
  }

  private boolean hasTemporaryGlowMarker(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }
    return meta.getPersistentDataContainer().has(this.markerKey, PersistentDataType.BYTE);
  }

  private void applyTemporaryGlow(ItemStack item, Enchantment enchantment) {
    if (enchantment == null) {
      return;
    }

    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return;
    }

    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    if (pdc.has(this.markerKey, PersistentDataType.BYTE)) {
      return;
    }

    boolean hadHideFlag = meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS);
    boolean hadGlowEnchant = meta.hasEnchant(enchantment);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    if (!hadGlowEnchant) {
      meta.addEnchant(enchantment, 1, true);
    }

    pdc.set(this.markerKey, PersistentDataType.BYTE, (byte) 1);
    pdc.set(this.hadHideFlagKey, PersistentDataType.BYTE, hadHideFlag ? (byte) 1 : (byte) 0);
    pdc.set(this.hadGlowEnchantKey, PersistentDataType.BYTE, hadGlowEnchant ? (byte) 1 : (byte) 0);
    pdc.set(this.glowEnchantKey, PersistentDataType.STRING, enchantment.getKey().toString());
    item.setItemMeta(meta);
  }

  private void clearTemporaryGlow(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return;
    }

    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    if (!pdc.has(this.markerKey, PersistentDataType.BYTE)) {
      return;
    }

    boolean hadHideFlag = this.readBooleanFlag(pdc, this.hadHideFlagKey, false);
    boolean hadGlowEnchant = this.readBooleanFlag(pdc, this.hadGlowEnchantKey, false);
    String enchantKeyRaw = pdc.get(this.glowEnchantKey, PersistentDataType.STRING);

    if (!hadHideFlag) {
      meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    if (!hadGlowEnchant && enchantKeyRaw != null) {
      Enchantment enchantment = this.resolveEnchantment(enchantKeyRaw, false);
      if (enchantment != null) {
        meta.removeEnchant(enchantment);
      }
    }

    pdc.remove(this.markerKey);
    pdc.remove(this.hadHideFlagKey);
    pdc.remove(this.hadGlowEnchantKey);
    pdc.remove(this.glowEnchantKey);
    item.setItemMeta(meta);
  }

  private boolean readBooleanFlag(PersistentDataContainer pdc, NamespacedKey key, boolean defaultValue) {
    Byte value = pdc.get(key, PersistentDataType.BYTE);
    if (value == null) {
      return defaultValue;
    }
    return value == (byte) 1;
  }

  private void cancelTask(Map<UUID, BukkitTask> tasks, UUID uuid) {
    BukkitTask task = tasks.remove(uuid);
    if (task != null) {
      task.cancel();
    }
  }

  private ClaimAnimationSettings resolveSettings(Kit kit) {
    boolean resolvedEnabled =
        kit.getClaimAnimationEnabled() != null ? kit.getClaimAnimationEnabled() : this.enabled;

    ClaimAnimationMode resolvedMode = this.mode;
    if (kit.getClaimAnimationMode() != null) {
      resolvedMode = ClaimAnimationMode.from(kit.getClaimAnimationMode());
    }

    int resolvedInterval =
        kit.getClaimAnimationIntervalTicks() != null
            ? Math.max(1, kit.getClaimAnimationIntervalTicks())
            : this.intervalTicks;

    int resolvedIntervalJitter =
        kit.getClaimAnimationIntervalJitterTicks() != null
            ? Math.max(0, kit.getClaimAnimationIntervalJitterTicks())
            : this.intervalJitterTicks;

    int resolvedBatchSize =
        kit.getClaimAnimationBatchSize() != null
            ? Math.max(1, kit.getClaimAnimationBatchSize())
            : this.batchSize;

    int resolvedStartDelay =
        kit.getClaimAnimationStartDelayTicks() != null
            ? Math.max(0, kit.getClaimAnimationStartDelayTicks())
            : this.startDelayTicks;

    boolean resolvedSplitStacks =
        kit.getClaimAnimationSplitStacks() != null
            ? kit.getClaimAnimationSplitStacks()
            : this.splitStacks;

    int resolvedMaxItems =
        kit.getClaimAnimationMaxItems() != null
            ? Math.max(0, kit.getClaimAnimationMaxItems())
            : this.maxItems;

    boolean resolvedGlowEnabled =
        kit.getClaimAnimationTemporaryGlowEnabled() != null
            ? kit.getClaimAnimationTemporaryGlowEnabled()
            : this.temporaryGlowEnabled;

    int resolvedGlowRemoveAfter =
        kit.getClaimAnimationTemporaryGlowRemoveAfterTicks() != null
            ? Math.max(1, kit.getClaimAnimationTemporaryGlowRemoveAfterTicks())
            : this.temporaryGlowRemoveAfterTicks;

    String resolvedGlowEnchantRaw =
        normalizeEnchantRaw(
            kit.getClaimAnimationTemporaryGlowEnchantment() != null
                ? kit.getClaimAnimationTemporaryGlowEnchantment()
                : this.temporaryGlowEnchantmentRaw,
            true);

    Enchantment resolvedGlowEnchant = this.resolveEnchantment(resolvedGlowEnchantRaw, true);

    if (!resolvedEnabled) {
      resolvedMode = ClaimAnimationMode.ALL_AT_ONCE;
      resolvedStartDelay = 0;
      resolvedInterval = 1;
      resolvedIntervalJitter = 0;
      resolvedBatchSize = Integer.MAX_VALUE;
      resolvedSplitStacks = false;
      resolvedGlowEnabled = false;
    }

    return new ClaimAnimationSettings(
        resolvedEnabled,
        resolvedMode,
        resolvedInterval,
        resolvedIntervalJitter,
        resolvedBatchSize,
        resolvedStartDelay,
        resolvedSplitStacks,
        resolvedMaxItems,
        resolvedGlowEnabled,
        resolvedGlowRemoveAfter,
        resolvedGlowEnchant);
  }

  private Enchantment resolveEnchantment(String raw, boolean fallbackDefault) {
    NamespacedKey key = this.parseEnchantmentKey(raw);
    if (key == null) {
      return fallbackDefault ? Enchantment.DURABILITY : null;
    }
    Enchantment enchantment = Enchantment.getByKey(key);
    if (enchantment != null) {
      return enchantment;
    }
    return fallbackDefault ? Enchantment.DURABILITY : null;
  }

  private NamespacedKey parseEnchantmentKey(String raw) {
    if (raw == null) {
      return null;
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (value.isEmpty()) {
      return null;
    }

    try {
      if (!value.contains(":")) {
        return NamespacedKey.minecraft(value);
      }
      String[] parts = value.split(":", 2);
      if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
        return null;
      }
      return new NamespacedKey(parts[0], parts[1]);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static String normalizeEnchantRaw(String raw, boolean withFallback) {
    if (raw == null || raw.isBlank()) {
      return withFallback ? "minecraft:unbreaking" : null;
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (value.equals("default")) {
      return withFallback ? "minecraft:unbreaking" : null;
    }
    return value;
  }

  private record ClaimAnimationSettings(
      boolean enabled,
      ClaimAnimationMode mode,
      int intervalTicks,
      int intervalJitterTicks,
      int batchSize,
      int startDelayTicks,
      boolean splitStacks,
      int maxItems,
      boolean temporaryGlowEnabled,
      int temporaryGlowRemoveAfterTicks,
      Enchantment temporaryGlowEnchantment) {

    int nextIntervalTicks() {
      if (this.intervalJitterTicks <= 0) {
        return this.intervalTicks;
      }

      int jitter =
          ThreadLocalRandom.current().nextInt(-this.intervalJitterTicks, this.intervalJitterTicks + 1);
      return Math.max(1, this.intervalTicks + jitter);
    }
  }

  private enum ClaimAnimationMode {
    ALL_AT_ONCE,
    FORWARD,
    REVERSE,
    RANDOM,
    EVEN_THEN_ODD,
    ODD_THEN_EVEN,
    CENTER_OUT,
    OUTSIDE_IN,
    EDGE_ALTERNATE,
    EDGE_ALTERNATE_REVERSE,
    SECOND_HALF_THEN_FIRST,
    WAVES_2,
    WAVES_3,
    CHUNKED_RANDOM_2,
    CHUNKED_RANDOM_3,
    FORWARD_SKIP_ONE,
    REVERSE_SKIP_ONE,
    MIRROR_CENTER,
    MIRROR_EDGES,
    RANDOM_WEIGHTED;

    String key() {
      return this.name().toLowerCase(Locale.ROOT);
    }

    static List<String> names() {
      return List.of(
          "all_at_once",
          "forward",
          "reverse",
          "random",
          "even_then_odd",
          "odd_then_even",
          "center_out",
          "outside_in",
          "edge_alternate",
          "edge_alternate_reverse",
          "second_half_then_first",
          "waves_2",
          "waves_3",
          "chunked_random_2",
          "chunked_random_3",
          "forward_skip_one",
          "reverse_skip_one",
          "mirror_center",
          "mirror_edges",
          "random_weighted");
    }

    static ClaimAnimationMode from(String raw) {
      if (raw == null || raw.isBlank()) {
        return ALL_AT_ONCE;
      }

      String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');

      if (value.equals("all") || value.equals("all_at_once") || value.equals("naraz")) {
        return ALL_AT_ONCE;
      }
      if (value.equals("forward")
          || value.equals("front")
          || value.equals("from_front")
          || value.equals("od_przodu")
          || value.equals("sequential")) {
        return FORWARD;
      }
      if (value.equals("reverse")
          || value.equals("back")
          || value.equals("from_back")
          || value.equals("od_tylu")) {
        return REVERSE;
      }
      if (value.equals("random") || value.equals("shuffle") || value.equals("losowo")) {
        return RANDOM;
      }
      if (value.equals("even_then_odd") || value.equals("parzyste_potem_nieparzyste")) {
        return EVEN_THEN_ODD;
      }
      if (value.equals("odd_then_even") || value.equals("nieparzyste_potem_parzyste")) {
        return ODD_THEN_EVEN;
      }
      if (value.equals("center_out") || value.equals("od_srodka")) {
        return CENTER_OUT;
      }
      if (value.equals("outside_in") || value.equals("od_brzegow")) {
        return OUTSIDE_IN;
      }
      if (value.equals("edge_alternate") || value.equals("edge_alt")) {
        return EDGE_ALTERNATE;
      }
      if (value.equals("edge_alternate_reverse") || value.equals("edge_alt_rev")) {
        return EDGE_ALTERNATE_REVERSE;
      }
      if (value.equals("second_half_then_first") || value.equals("polowa2_potem_polowa1")) {
        return SECOND_HALF_THEN_FIRST;
      }
      if (value.equals("waves_2") || value.equals("fala2")) {
        return WAVES_2;
      }
      if (value.equals("waves_3") || value.equals("fala3")) {
        return WAVES_3;
      }
      if (value.equals("chunked_random_2") || value.equals("chunk2")) {
        return CHUNKED_RANDOM_2;
      }
      if (value.equals("chunked_random_3") || value.equals("chunk3")) {
        return CHUNKED_RANDOM_3;
      }
      if (value.equals("forward_skip_one") || value.equals("przod_co_drugi")) {
        return FORWARD_SKIP_ONE;
      }
      if (value.equals("reverse_skip_one") || value.equals("tyl_co_drugi")) {
        return REVERSE_SKIP_ONE;
      }
      if (value.equals("mirror_center") || value.equals("lustro_srodek")) {
        return MIRROR_CENTER;
      }
      if (value.equals("mirror_edges") || value.equals("lustro_brzegi")) {
        return MIRROR_EDGES;
      }
      if (value.equals("random_weighted") || value.equals("random_wazony")) {
        return RANDOM_WEIGHTED;
      }

      return ALL_AT_ONCE;
    }
  }
}
