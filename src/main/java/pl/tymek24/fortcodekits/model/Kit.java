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
    this.id = id.toLowerCase();
    this.displayName = id;
    this.cooldownSeconds = 0L;
    this.oneTime = false;
    this.guiSlot = null;
    this.requiredGroup = null;
    this.feedbackType = null;
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
    List rawItems = section.getList("items", new ArrayList());
    for (Object o : rawItems) {
      if (!(o instanceof ItemStack)) continue;
      ItemStack stack = (ItemStack) o;
      loaded.add(stack);
    }
    kit.items = loaded;
    return kit;
  }

  public void saveTo(ConfigurationSection section) {
    section.set("display-name", (Object) this.displayName);
    section.set("cooldown", (Object) this.cooldownSeconds);
    section.set("one-time", (Object) this.oneTime);
    section.set("gui-slot", (Object) this.guiSlot);
    section.set("rank", (Object) this.requiredGroup);
    section.set("feedback-type", (Object) this.feedbackType);
    section.set("claim-animation.enabled", (Object) this.claimAnimationEnabled);
    section.set("claim-animation.mode", (Object) this.claimAnimationMode);
    section.set("claim-animation.interval-ticks", (Object) this.claimAnimationIntervalTicks);
    section.set("claim-animation.batch-size", (Object) this.claimAnimationBatchSize);
    section.set(
        "claim-animation.interval-jitter-ticks", (Object) this.claimAnimationIntervalJitterTicks);
    section.set("claim-animation.start-delay-ticks", (Object) this.claimAnimationStartDelayTicks);
    section.set("claim-animation.split-stacks", (Object) this.claimAnimationSplitStacks);
    section.set("claim-animation.max-items", (Object) this.claimAnimationMaxItems);
    section.set(
        "claim-animation.temporary-glow.enabled",
        (Object) this.claimAnimationTemporaryGlowEnabled);
    section.set(
        "claim-animation.temporary-glow.remove-after-ticks",
        (Object) this.claimAnimationTemporaryGlowRemoveAfterTicks);
    section.set(
        "claim-animation.temporary-glow.enchantment",
        (Object) this.claimAnimationTemporaryGlowEnchantment);
    section.set("icon", (Object) this.icon);
    section.set("items", this.items);
  }

  public String buildAdminSummary() {
    ItemMeta meta = this.icon.getItemMeta();
    String iconName =
        meta != null && meta.hasDisplayName() ? meta.getDisplayName() : this.icon.getType().name();
    return String.format(
        "%s | cooldown=%ss | oneTime=%s | items=%d | icon=%s | slot=%s | rank=%s",
        this.displayName,
        this.cooldownSeconds,
        this.oneTime,
        this.items.size(),
        iconName,
        this.guiSlot == null ? "-" : this.guiSlot,
        this.requiredGroup == null ? "-" : this.requiredGroup);
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

  private static String normalizeClaimAnimationMode(String raw) {
    if (raw == null) {
      return null;
    }
    String value =
        raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
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
