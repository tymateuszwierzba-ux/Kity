package pl.tymek24.fortcodekits.util;

public final class TimeUtil {
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
}
