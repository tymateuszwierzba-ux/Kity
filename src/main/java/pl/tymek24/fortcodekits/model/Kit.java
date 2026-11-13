package pl.tymek24.fortcodekits.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Kit {
  private final String id;
  private String displayName;
  private long cooldownSeconds;
  private boolean oneTime;
  private Integer guiSlot;
  private String requiredGroup;
  private String feedbackType;
  private String category;
  private double price;
  private KitRarity rarity;
  private boolean enabled;
  private boolean hidden;
  private boolean welcome;
  private Integer claimCountLimit;
  private Boolean claimAnimationEnabled;
  private String claimAnimationMode;
  private Integer claimAnimationIntervalTicks;
  private Integer claimAnimationBatchSize;
  private Integer claimAnimationIntervalJitterTicks;
  private Integer claimAnimationStartDelayTicks;
  private Boolean claimAnimationSplitStacks;
  private Integer claimAnimationMaxItems;
  private Boolean claimAnimationTemporaryGlowEnabled;
  private Integer claimAnimationTemporaryGlowRemoveAfterTicks;
  private String claimAnimationTemporaryGlowEnchantment;
  private ItemStack icon;
  private List<ItemStack> items;

  public Kit(String id) {
    this.id = id.toLowerCase(Locale.ROOT);
    this.displayName = id;
    this.cooldownSeconds = 0L;
    this.oneTime = false;
    this.guiSlot = null;
    this.requiredGroup = null;
    this.feedbackType = null;
    this.category = null;
    this.price = 0.0D;
    this.rarity = null;
    this.enabled = true;
    this.hidden = false;
    this.welcome = false;
    this.claimCountLimit = null;
    this.claimAnimationEnabled = null;
    this.claimAnimationMode = null;
    this.claimAnimationIntervalTicks = null;
    this.claimAnimationBatchSize = null;
    this.claimAnimationIntervalJitterTicks = null;
    this.claimAnimationStartDelayTicks = null;
    this.claimAnimationSplitStacks = null;
    this.claimAnimationMaxItems = null;
    this.claimAnimationTemporaryGlowEnabled = null;
    this.claimAnimationTemporaryGlowRemoveAfterTicks = null;
    this.claimAnimationTemporaryGlowEnchantment = null;
    this.icon = new ItemStack(Material.CHEST);
    this.items = new ArrayList<ItemStack>();
  }

  public static Kit fromConfig(String id, ConfigurationSection section) {
    Kit kit = new Kit(id);
    kit.displayName = section.getString("display-name", id);
    kit.cooldownSeconds = section.getLong("cooldown", 0L);
    kit.oneTime = section.getBoolean("one-time", false);
    int slot = section.getInt("gui-slot", -1);
    kit.guiSlot = slot < 0 ? null : Integer.valueOf(slot);
    String rawGroup = section.getString("rank", section.getString("required-group", ""));
    kit.requiredGroup = Kit.normalizeGroup(rawGroup);
    String rawFeedback = section.getString("feedback-type", section.getString("message-type", ""));
    kit.feedbackType = Kit.normalizeFeedbackType(rawFeedback);
    kit.category = Kit.normalizeCategory(section.getString("category", ""));
    kit.price = Math.max(0.0D, section.getDouble("price", 0.0D));
    kit.rarity = KitRarity.from(section.getString("rarity", ""));
    kit.enabled = section.getBoolean("enabled", true);
    kit.hidden = section.getBoolean("hidden", false);
    kit.welcome = section.getBoolean("welcome", false);
    int limit = section.getInt("claim-limit", -1);
    kit.claimCountLimit = limit < 0 ? null : Integer.valueOf(limit);
    ConfigurationSection claimAnimation = section.getConfigurationSection("claim-animation");
    if (claimAnimation != null) {
      if (claimAnimation.contains("enabled")) {
        kit.claimAnimationEnabled = claimAnimation.getBoolean("enabled");
      }
      if (claimAnimation.contains("mode")) {
        kit.claimAnimationMode = Kit.normalizeClaimAnimationMode(claimAnimation.getString("mode"));
      }
      if (claimAnimation.contains("interval-ticks")) {
        kit.claimAnimationIntervalTicks = Math.max(1, claimAnimation.getInt("interval-ticks"));
      }
      if (claimAnimation.contains("batch-size")) {
        kit.claimAnimationBatchSize = Math.max(1, claimAnimation.getInt("batch-size"));
      }
      if (claimAnimation.contains("interval-jitter-ticks")) {
        kit.claimAnimationIntervalJitterTicks =
            Math.max(0, claimAnimation.getInt("interval-jitter-ticks"));
      }
      if (claimAnimation.contains("start-delay-ticks")) {
        kit.claimAnimationStartDelayTicks = Math.max(0, claimAnimation.getInt("start-delay-ticks"));
      }
      if (claimAnimation.contains("split-stacks")) {
        kit.claimAnimationSplitStacks = claimAnimation.getBoolean("split-stacks");
      }
      if (claimAnimation.contains("max-items")) {
        int maxItems = claimAnimation.getInt("max-items");
        kit.claimAnimationMaxItems = maxItems <= 0 ? 0 : maxItems;
      }
      ConfigurationSection tempGlow = claimAnimation.getConfigurationSection("temporary-glow");
      if (tempGlow != null) {
        if (tempGlow.contains("enabled")) {
          kit.claimAnimationTemporaryGlowEnabled = tempGlow.getBoolean("enabled");
        }
        if (tempGlow.contains("remove-after-ticks")) {
          kit.claimAnimationTemporaryGlowRemoveAfterTicks =
              Math.max(1, tempGlow.getInt("remove-after-ticks"));
        }
        if (tempGlow.contains("enchantment")) {
          kit.claimAnimationTemporaryGlowEnchantment =
              Kit.normalizeClaimAnimationEnchantment(tempGlow.getString("enchantment"));
        }
      }
    }
    ItemStack configIcon = section.getItemStack("icon");
    if (configIcon != null) {
      kit.icon = configIcon;
    }
    ArrayList<ItemStack> loaded = new ArrayList<ItemStack>();
    List<?> rawItems = section.getList("items", new ArrayList<>());
    for (Object o : rawItems) {
      if (!(o instanceof ItemStack)) continue;
      loaded.add((ItemStack) o);
    }
    kit.items = loaded;
    return kit;
  }

  public void saveTo(ConfigurationSection section) {
    section.set("display-name", this.displayName);
    section.set("cooldown", this.cooldownSeconds);
    section.set("one-time", this.oneTime);
    section.set("gui-slot", this.guiSlot);
    section.set("rank", this.requiredGroup);
    section.set("feedback-type", this.feedbackType);
    section.set("category", this.category);
    section.set("price", this.price);
    section.set("rarity", this.rarity == null ? null : this.rarity.key());
    section.set("enabled", this.enabled);
    section.set("hidden", this.hidden);
    section.set("welcome", this.welcome);
    section.set("claim-limit", this.claimCountLimit);
    section.set("claim-animation.enabled", this.claimAnimationEnabled);
    section.set("claim-animation.mode", this.claimAnimationMode);
    section.set("claim-animation.interval-ticks", this.claimAnimationIntervalTicks);
    section.set("claim-animation.batch-size", this.claimAnimationBatchSize);
    section.set("claim-animation.interval-jitter-ticks", this.claimAnimationIntervalJitterTicks);
    section.set("claim-animation.start-delay-ticks", this.claimAnimationStartDelayTicks);
    section.set("claim-animation.split-stacks", this.claimAnimationSplitStacks);
    section.set("claim-animation.max-items", this.claimAnimationMaxItems);
    section.set("claim-animation.temporary-glow.enabled", this.claimAnimationTemporaryGlowEnabled);
    section.set(
        "claim-animation.temporary-glow.remove-after-ticks",
        this.claimAnimationTemporaryGlowRemoveAfterTicks);
    section.set(
        "claim-animation.temporary-glow.enchantment", this.claimAnimationTemporaryGlowEnchantment);
    section.set("icon", this.icon);
    section.set("items", this.items);
  }

  public String buildAdminSummary() {
    ItemMeta meta = this.icon.getItemMeta();
    String iconName =
        meta != null && meta.hasDisplayName() ? meta.getDisplayName() : this.icon.getType().name();
    return String.format(
        "%s | cd=%ss | oneTime=%s | price=%.2f | rarity=%s | cat=%s | items=%d | icon=%s | slot=%s | rank=%s",
        this.displayName,
        this.cooldownSeconds,
        this.oneTime,
        this.price,
        this.rarity == null ? "-" : this.rarity.key(),
        this.category == null ? "-" : this.category,
        this.items.size(),
        iconName,
        this.guiSlot == null ? "-" : this.guiSlot,
        this.requiredGroup == null ? "-" : this.requiredGroup);
  }

  public Kit duplicate(String newId) {
    Kit copy = new Kit(newId);
    copy.displayName = this.displayName;
    copy.cooldownSeconds = this.cooldownSeconds;
    copy.oneTime = this.oneTime;
    copy.guiSlot = null;
    copy.requiredGroup = this.requiredGroup;
    copy.feedbackType = this.feedbackType;
    copy.category = this.category;
    copy.price = this.price;
    copy.rarity = this.rarity;
    copy.enabled = this.enabled;
    copy.hidden = this.hidden;
    copy.welcome = false;
    copy.claimCountLimit = this.claimCountLimit;
    copy.claimAnimationEnabled = this.claimAnimationEnabled;
    copy.claimAnimationMode = this.claimAnimationMode;
    copy.claimAnimationIntervalTicks = this.claimAnimationIntervalTicks;
    copy.claimAnimationBatchSize = this.claimAnimationBatchSize;
    copy.claimAnimationIntervalJitterTicks = this.claimAnimationIntervalJitterTicks;
    copy.claimAnimationStartDelayTicks = this.claimAnimationStartDelayTicks;
    copy.claimAnimationSplitStacks = this.claimAnimationSplitStacks;
    copy.claimAnimationMaxItems = this.claimAnimationMaxItems;
    copy.claimAnimationTemporaryGlowEnabled = this.claimAnimationTemporaryGlowEnabled;
    copy.claimAnimationTemporaryGlowRemoveAfterTicks =
        this.claimAnimationTemporaryGlowRemoveAfterTicks;
    copy.claimAnimationTemporaryGlowEnchantment = this.claimAnimationTemporaryGlowEnchantment;
    copy.icon = this.icon.clone();
    copy.items = new ArrayList<ItemStack>();
    for (ItemStack item : this.items) {
      copy.items.add(item.clone());
    }
    return copy;
  }

  public String getId() {
    return this.id;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public long getCooldownSeconds() {
    return this.cooldownSeconds;
  }

  public void setCooldownSeconds(long cooldownSeconds) {
    this.cooldownSeconds = cooldownSeconds;
  }

  public boolean isOneTime() {
    return this.oneTime;
  }

  public void setOneTime(boolean oneTime) {
    this.oneTime = oneTime;
  }

  public Integer getGuiSlot() {
    return this.guiSlot;
  }

  public void setGuiSlot(Integer guiSlot) {
    this.guiSlot = guiSlot;
  }

  public String getRequiredGroup() {
    return this.requiredGroup;
  }

  public void setRequiredGroup(String requiredGroup) {
    this.requiredGroup = Kit.normalizeGroup(requiredGroup);
  }

  public String getFeedbackType() {
    return this.feedbackType;
  }

  public void setFeedbackType(String feedbackType) {
    this.feedbackType = Kit.normalizeFeedbackType(feedbackType);
  }

  public String getCategory() {
    return this.category;
  }

  public void setCategory(String category) {
    this.category = Kit.normalizeCategory(category);
  }

  public double getPrice() {
    return this.price;
  }

  public void setPrice(double price) {
    this.price = Math.max(0.0D, price);
  }

  public KitRarity getRarity() {
    return this.rarity;
  }

  public void setRarity(KitRarity rarity) {
    this.rarity = rarity;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isHidden() {
    return this.hidden;
  }

  public void setHidden(boolean hidden) {
    this.hidden = hidden;
  }

  public boolean isWelcome() {
    return this.welcome;
  }

  public void setWelcome(boolean welcome) {
    this.welcome = welcome;
  }

  public Integer getClaimCountLimit() {
    return this.claimCountLimit;
  }

  public void setClaimCountLimit(Integer claimCountLimit) {
    this.claimCountLimit = claimCountLimit;
  }

  public Boolean getClaimAnimationEnabled() {
    return this.claimAnimationEnabled;
  }

  public void setClaimAnimationEnabled(Boolean claimAnimationEnabled) {
    this.claimAnimationEnabled = claimAnimationEnabled;
  }

  public String getClaimAnimationMode() {
    return this.claimAnimationMode;
  }

  public void setClaimAnimationMode(String claimAnimationMode) {
    this.claimAnimationMode = Kit.normalizeClaimAnimationMode(claimAnimationMode);
  }

  public Integer getClaimAnimationIntervalTicks() {
    return this.claimAnimationIntervalTicks;
  }

  public void setClaimAnimationIntervalTicks(Integer claimAnimationIntervalTicks) {
    if (claimAnimationIntervalTicks == null) {
      this.claimAnimationIntervalTicks = null;
      return;
    }
    this.claimAnimationIntervalTicks = Math.max(1, claimAnimationIntervalTicks);
  }

  public Integer getClaimAnimationBatchSize() {
    return this.claimAnimationBatchSize;
  }

  public void setClaimAnimationBatchSize(Integer claimAnimationBatchSize) {
    if (claimAnimationBatchSize == null) {
      this.claimAnimationBatchSize = null;
      return;
    }
    this.claimAnimationBatchSize = Math.max(1, claimAnimationBatchSize);
  }

  public Integer getClaimAnimationIntervalJitterTicks() {
    return this.claimAnimationIntervalJitterTicks;
  }

  public void setClaimAnimationIntervalJitterTicks(Integer claimAnimationIntervalJitterTicks) {
    if (claimAnimationIntervalJitterTicks == null) {
      this.claimAnimationIntervalJitterTicks = null;
      return;
    }
    this.claimAnimationIntervalJitterTicks = Math.max(0, claimAnimationIntervalJitterTicks);
  }

  public Integer getClaimAnimationStartDelayTicks() {
    return this.claimAnimationStartDelayTicks;
  }

  public void setClaimAnimationStartDelayTicks(Integer claimAnimationStartDelayTicks) {
    if (claimAnimationStartDelayTicks == null) {
      this.claimAnimationStartDelayTicks = null;
      return;
    }
    this.claimAnimationStartDelayTicks = Math.max(0, claimAnimationStartDelayTicks);
  }

  public Boolean getClaimAnimationSplitStacks() {
    return this.claimAnimationSplitStacks;
  }

  public void setClaimAnimationSplitStacks(Boolean claimAnimationSplitStacks) {
    this.claimAnimationSplitStacks = claimAnimationSplitStacks;
  }

  public Integer getClaimAnimationMaxItems() {
    return this.claimAnimationMaxItems;
  }

  public void setClaimAnimationMaxItems(Integer claimAnimationMaxItems) {
    if (claimAnimationMaxItems == null) {
      this.claimAnimationMaxItems = null;
      return;
    }
    this.claimAnimationMaxItems = claimAnimationMaxItems <= 0 ? 0 : claimAnimationMaxItems;
  }

  public Boolean getClaimAnimationTemporaryGlowEnabled() {
    return this.claimAnimationTemporaryGlowEnabled;
  }

  public void setClaimAnimationTemporaryGlowEnabled(Boolean claimAnimationTemporaryGlowEnabled) {
    this.claimAnimationTemporaryGlowEnabled = claimAnimationTemporaryGlowEnabled;
  }

  public Integer getClaimAnimationTemporaryGlowRemoveAfterTicks() {
    return this.claimAnimationTemporaryGlowRemoveAfterTicks;
  }

  public void setClaimAnimationTemporaryGlowRemoveAfterTicks(
      Integer claimAnimationTemporaryGlowRemoveAfterTicks) {
    if (claimAnimationTemporaryGlowRemoveAfterTicks == null) {
      this.claimAnimationTemporaryGlowRemoveAfterTicks = null;
      return;
    }
    this.claimAnimationTemporaryGlowRemoveAfterTicks =
        Math.max(1, claimAnimationTemporaryGlowRemoveAfterTicks);
  }

  public String getClaimAnimationTemporaryGlowEnchantment() {
    return this.claimAnimationTemporaryGlowEnchantment;
  }

  public void setClaimAnimationTemporaryGlowEnchantment(
      String claimAnimationTemporaryGlowEnchantment) {
    this.claimAnimationTemporaryGlowEnchantment =
        Kit.normalizeClaimAnimationEnchantment(claimAnimationTemporaryGlowEnchantment);
  }

  public ItemStack getIcon() {
    return this.icon.clone();
  }

  public void setIcon(ItemStack icon) {
    this.icon = icon == null ? new ItemStack(Material.CHEST) : icon.clone();
  }

  public List<ItemStack> getItems() {
    return new ArrayList<ItemStack>(this.items);
  }

  public void setItems(List<ItemStack> items) {
    this.items = new ArrayList<ItemStack>(items);
  }

  private static String normalizeGroup(String raw) {
    if (raw == null) {
      return null;
    }
    String value = raw.trim();
    if (value.isEmpty()) {
      return null;
    }
    String lower = value.toLowerCase(Locale.ROOT);
    if (lower.startsWith("luckperms.group.")) {
      lower = lower.substring("luckperms.group.".length());
    }
    if (lower.startsWith("group.")) {
      lower = lower.substring("group.".length());
    }
    return lower.isEmpty() ? null : lower;
  }

  private static String normalizeFeedbackType(String raw) {
    if (raw == null) {
      return null;
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (value.isEmpty() || value.equals("default")) {
      return null;
    }
    return value;
  }

  private static String normalizeCategory(String raw) {
    if (raw == null) {
      return null;
    }
    String value = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    return value.isEmpty() ? null : value;
  }

  private static String normalizeClaimAnimationMode(String raw) {
    if (raw == null) {
      return null;
    }
    String value = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    if (value.isEmpty() || value.equals("default")) {
      return null;
    }
    return value;
  }

  private static String normalizeClaimAnimationEnchantment(String raw) {
    if (raw == null) {
      return null;
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (value.isEmpty() || value.equals("default")) {
      return null;
    }
    return value;
  }
}
