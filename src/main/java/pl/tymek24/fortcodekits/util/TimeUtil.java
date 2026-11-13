package pl.tymek24.fortcodekits.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {
  private static final DateTimeFormatter CLOCK =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
  private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private TimeUtil() {}

  public static String formatDuration(long seconds) {
    if (seconds <= 0L) {
      return "0s";
    }
    long days = seconds / 86400L;
    long hours = seconds % 86400L / 3600L;
    long minutes = seconds % 3600L / 60L;
    long secs = seconds % 60L;
    StringBuilder builder = new StringBuilder();
    if (days > 0L) {
      builder.append(days).append("d ");
    }
    if (hours > 0L) {
      builder.append(hours).append("h ");
    }
    if (minutes > 0L) {
      builder.append(minutes).append("m ");
    }
    if (secs > 0L || builder.isEmpty()) {
      builder.append(secs).append("s");
    }
    return builder.toString().trim();
  }

  public static String formatTimestamp(long epochMillis) {
    LocalDateTime time =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    return CLOCK.format(time);
  }

  public static String formatDate(long epochMillis) {
    LocalDateTime time =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    return DATE_ONLY.format(time);
  }

  public static String formatAgo(long epochMillis) {
    long diff = System.currentTimeMillis() - epochMillis;
    if (diff < 0L) diff = 0L;
    long seconds = diff / 1000L;
    if (seconds < 60L) return seconds + "s temu";
    long minutes = seconds / 60L;
    if (minutes < 60L) return minutes + "m temu";
    long hours = minutes / 60L;
    if (hours < 24L) return hours + "h temu";
    long days = hours / 24L;
    return days + "d temu";
  }

  public static long parseDuration(String raw) {
    if (raw == null || raw.isBlank()) {
      return -1L;
    }
    String value = raw.trim().toLowerCase();
    long total = 0L;
    StringBuilder number = new StringBuilder();
    boolean any = false;
    for (char c : value.toCharArray()) {
      if (Character.isDigit(c)) {
        number.append(c);
        continue;
      }
      if (number.isEmpty()) {
        return -1L;
      }
      long amount;
      try {
        amount = Long.parseLong(number.toString());
      } catch (NumberFormatException ex) {
        return -1L;
      }
      number.setLength(0);
      any = true;
      switch (c) {
        case 'd' -> total += amount * 86400L;
        case 'h' -> total += amount * 3600L;
        case 'm' -> total += amount * 60L;
        case 's' -> total += amount;
        default -> {
          return -1L;
        }
      }
    }
    if (!number.isEmpty()) {
      try {
        total += Long.parseLong(number.toString());
        any = true;
      } catch (NumberFormatException ex) {
        return -1L;
      }
    }
    return any ? total : -1L;
  }
}
