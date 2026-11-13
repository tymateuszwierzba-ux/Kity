package pl.tymek24.fortcodekits.animation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.tymek24.fortcodekits.FortCodeKits;

public class TextAnimationManager {
  private static final Pattern ANIM_TAG_PATTERN =
      Pattern.compile("(?is)<anim:([a-z0-9_\\-]+)>(.*?)</anim>");
  private static final Pattern GRADIENT_TAG_PATTERN =
      Pattern.compile("(?is)<gradient:([^>]+)>(.*?)</gradient>");
  private static final Pattern SHORT_GRADIENT_TAG_PATTERN =
      Pattern.compile("(?is)<grad:([^>]+)>(.*?)</grad>");
  private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
  private final FortCodeKits plugin;
  private final File file;
  private final Map<String, TextAnimator> animations = new HashMap<String, TextAnimator>();
  private boolean enabled = true;
  private int refreshTicks = 1;

  public TextAnimationManager(FortCodeKits plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "animations.yml");
    this.reload();
  }

  public void reload() {
    YamlConfiguration config = YamlConfiguration.loadConfiguration((File) this.file);
    InputStream resource = this.plugin.getResource("animations.yml");
    if (resource != null) {
      YamlConfiguration defaults =
          YamlConfiguration.loadConfiguration(
              (Reader) new InputStreamReader(resource, StandardCharsets.UTF_8));
      config.setDefaults((Configuration) defaults);
      config.options().copyDefaults(true);
      try {
        config.save(this.file);
      } catch (IOException ex) {
        this.plugin.getLogger().warning("Nie udalo sie zapisac animations.yml: " + ex.getMessage());
      }
    }
    this.enabled = config.getBoolean("enabled", true);
    this.refreshTicks = Math.max(1, config.getInt("refresh-ticks", 1));
    this.animations.clear();
    ConfigurationSection root = config.getConfigurationSection("animations");
    if (root == null) {
      return;
    }
    for (String key : root.getKeys(false)) {
      TextAnimator animator;
      ConfigurationSection section = root.getConfigurationSection(key);
      if (section == null) continue;
      String type = section.getString("type", "").trim().toLowerCase(Locale.ROOT);
      if (type.isEmpty()) {
        type = this.detectLegacyType(section);
      }
      if ((animator =
              (switch (type) {
                case "gradient" -> this.buildGradientAnimator(section);
                case "gradient-legacy" -> this.buildLegacyGradientAnimator(section);
                case "frames" -> this.buildFramesAnimator(section);
                default -> null;
              }))
          == null) continue;
      this.animations.put(key.toLowerCase(Locale.ROOT), animator);
    }
  }

  public String apply(String text) {
    if (!this.enabled || text == null || text.isEmpty()) {
      return text;
    }
    long nowNanos = System.nanoTime();
    String result = this.replaceAnimationTags(text, nowNanos);
    result = this.replaceGradientTags(result);
    return result;
  }

  public int getRefreshTicks() {
    return this.refreshTicks;
  }

  public boolean hasAnimation(String id) {
    if (id == null || id.isBlank()) {
      return false;
    }
    return this.animations.containsKey(id.trim().toLowerCase(Locale.ROOT));
  }

  public List<String> getAnimationNames() {
    return this.animations.keySet().stream().sorted(Comparator.naturalOrder()).toList();
  }

  private String replaceAnimationTags(String input, long nowNanos) {
    Matcher matcher = ANIM_TAG_PATTERN.matcher(input);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String id = matcher.group(1).toLowerCase(Locale.ROOT);
      String content = matcher.group(2);
      TextAnimator animator = this.animations.get(id);
      String replacement = animator == null ? content : animator.render(content, nowNanos);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private String replaceGradientTags(String input) {
    String afterLong = this.replaceGradientTagPattern(input, GRADIENT_TAG_PATTERN);
    return this.replaceGradientTagPattern(afterLong, SHORT_GRADIENT_TAG_PATTERN);
  }

  private String replaceGradientTagPattern(String input, Pattern pattern) {
    Matcher matcher = pattern.matcher(input);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      String spec = matcher.group(1);
      String content = matcher.group(2);
      List<Integer> colors = this.parseHexColors(spec);
      String replacement = colors.size() < 2 ? content : this.applyStaticGradient(content, colors);
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private TextAnimator buildFramesAnimator(ConfigurationSection section) {
    int intervalTicks = Math.max(1, section.getInt("interval-ticks", 2));
    List frames = section.getStringList("frames");
    if (frames.isEmpty()) {
      return null;
    }
    return new FramesAnimator(intervalTicks, frames);
  }

  private TextAnimator buildGradientAnimator(ConfigurationSection section) {
    List<Integer> colors = this.parseHexColors(section.getStringList("colors"));
    if (colors.size() < 2) {
      return null;
    }
    double speed = section.getDouble("speed", 0.6);
    double spread = section.getDouble("spread", 0.17);
    return new GradientAnimator(colors, speed, spread);
  }

  private TextAnimator buildLegacyGradientAnimator(ConfigurationSection section) {
    List<Integer> colors = this.extractLegacyFrameColors(section.getStringList("frames"));
    if (colors.size() < 2) {
      return null;
    }
    double speed = section.getDouble("speed", 0.6);
    double spread = section.getDouble("spread", 0.17);
    return new GradientAnimator(colors, speed, spread);
  }

  private String detectLegacyType(ConfigurationSection section) {
    List<Integer> explicitColors = this.parseHexColors(section.getStringList("colors"));
    if (explicitColors.size() >= 2) {
      return "gradient";
    }
    List<Integer> frameColors = this.extractLegacyFrameColors(section.getStringList("frames"));
    if (frameColors.size() >= 2) {
      return "gradient-legacy";
    }
    return "frames";
  }

  private List<Integer> parseHexColors(String spec) {
    if (spec == null || spec.isBlank()) {
      return List.of();
    }
    String normalized = spec.replace(';', ':').replace(',', ':').trim();
    String[] parts = normalized.split(":");
    ArrayList<Integer> result = new ArrayList<Integer>();
    for (String raw : parts) {
      String value = raw == null ? "" : raw.trim();
      int color = this.parseHexColor(value);
      if (color < 0) continue;
      result.add(color);
    }
    return result;
  }

  private List<Integer> parseHexColors(List<String> values) {
    if (values == null || values.isEmpty()) {
      return List.of();
    }
    ArrayList<Integer> result = new ArrayList<Integer>();
    for (String raw : values) {
      int color = this.parseHexColor(raw);
      if (color < 0) continue;
      result.add(color);
    }
    return result;
  }

  private List<Integer> extractLegacyFrameColors(List<String> frames) {
    if (frames == null || frames.isEmpty()) {
      return List.of();
    }
    ArrayList<Integer> result = new ArrayList<Integer>();
    for (String frame : frames) {
      int color;
      Matcher matcher;
      if (frame == null
          || frame.isBlank()
          || !(matcher = HEX_PATTERN.matcher(frame)).find()
          || (color = this.parseHexColor(matcher.group(1))) < 0
          || !result.isEmpty() && (Integer) result.get(result.size() - 1) == color) continue;
      result.add(color);
    }
    return result;
  }

  private int parseHexColor(String raw) {
    if (raw == null) {
      return -1;
    }
    String value = raw.trim();
    if (value.isEmpty()) {
      return -1;
    }
    if (value.startsWith("#")) {
      value = value.substring(1);
    }
    if (value.length() != 6) {
      return -1;
    }
    try {
      return Integer.parseInt(value, 16);
    } catch (NumberFormatException ex) {
      return -1;
    }
  }

  private String applyStaticGradient(String text, List<Integer> colors) {
    if (text.isEmpty()) {
      return text;
    }
    int size = text.length();
    StringBuilder out = new StringBuilder(size * 14);
    for (int i = 0; i < size; ++i) {
      double t = size <= 1 ? 0.0 : (double) i / (double) (size - 1);
      int rgb = this.interpolateLinear(colors, t);
      out.append(this.toHexCode(rgb)).append(text.charAt(i));
    }
    return out.toString();
  }

  private String applyAnimatedGradient(
      String text, List<Integer> colors, long nowNanos, double speed, double spread) {
    if (text.isEmpty()) {
      return text;
    }
    double seconds = (double) nowNanos / 1.0E9;
    double phase = seconds * speed;
    StringBuilder out = new StringBuilder(text.length() * 14);
    for (int i = 0; i < text.length(); ++i) {
      double t = this.fract(phase + (double) i * spread);
      int rgb = this.interpolateLoop(colors, t);
      out.append(this.toHexCode(rgb)).append(text.charAt(i));
    }
    return out.toString();
  }

  private int interpolateLoop(List<Integer> colors, double t) {
    int size = colors.size();
    if (size == 1) {
      return colors.get(0);
    }
    double scaled = this.fract(t) * (double) size;
    int idx = (int) Math.floor(scaled);
    double local = scaled - (double) idx;
    int c1 = colors.get(idx % size);
    int c2 = colors.get((idx + 1) % size);
    return this.lerpColor(c1, c2, local);
  }

  private int interpolateLinear(List<Integer> colors, double t) {
    int size = colors.size();
    if (size == 1) {
      return colors.get(0);
    }
    double clamped = Math.max(0.0, Math.min(1.0, t));
    double scaled = clamped * (double) (size - 1);
    int idx = (int) Math.floor(scaled);
    if (idx >= size - 1) {
      return colors.get(size - 1);
    }
    double local = scaled - (double) idx;
    return this.lerpColor(colors.get(idx), colors.get(idx + 1), local);
  }

  private int lerpColor(int c1, int c2, double t) {
    int r1 = c1 >> 16 & 0xFF;
    int g1 = c1 >> 8 & 0xFF;
    int b1 = c1 & 0xFF;
    int r2 = c2 >> 16 & 0xFF;
    int g2 = c2 >> 8 & 0xFF;
    int b2 = c2 & 0xFF;
    int r = (int) Math.round((double) r1 + (double) (r2 - r1) * t);
    int g = (int) Math.round((double) g1 + (double) (g2 - g1) * t);
    int b = (int) Math.round((double) b1 + (double) (b2 - b1) * t);
    return (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF;
  }

  private String toHexCode(int rgb) {
    return String.format("&#%06X", rgb & 0xFFFFFF);
  }

  private double fract(double value) {
    double r = value - Math.floor(value);
    return r < 0.0 ? r + 1.0 : r;
  }

  private static interface TextAnimator {
    public String render(String var1, long var2);
  }

  private final class FramesAnimator implements TextAnimator {
    private final int intervalTicks;
    private final List<String> frames;

    private FramesAnimator(int intervalTicks, List<String> frames) {
      this.intervalTicks = intervalTicks;
      this.frames = frames;
    }

    @Override
    public String render(String text, long nowNanos) {
      long tick = (long) Math.floor((double) nowNanos / 1.0E9 * 20.0);
      int frameIndex = (int) (tick / (long) this.intervalTicks % (long) this.frames.size());
      String frame = this.frames.get(frameIndex);
      return frame.replace("{text}", text).replace("%text%", text);
    }
  }

  private final class GradientAnimator implements TextAnimator {
    private final List<Integer> colors;
    private final double speed;
    private final double spread;

    private GradientAnimator(List<Integer> colors, double speed, double spread) {
      this.colors = colors;
      this.speed = speed;
      this.spread = spread;
    }

    @Override
    public String render(String text, long nowNanos) {
      return TextAnimationManager.this.applyAnimatedGradient(
          text, this.colors, nowNanos, this.speed, this.spread);
    }
  }
}
