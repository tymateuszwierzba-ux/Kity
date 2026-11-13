package pl.tymek24.fortcodekits.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;

public final class ColorUtil {
  private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

  private ColorUtil() {}

  public static String colorize(String text) {
    if (text == null) {
      return "";
    }
    Matcher matcher = HEX_PATTERN.matcher(text);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(sb, ChatColor.of((String) ("#" + matcher.group(1))).toString());
    }
    matcher.appendTail(sb);
    return ChatColor.translateAlternateColorCodes((char) '&', (String) sb.toString());
  }

  public static List<String> colorize(List<String> lines) {
    return lines.stream().map(ColorUtil::colorize).collect(Collectors.toList());
  }
}
