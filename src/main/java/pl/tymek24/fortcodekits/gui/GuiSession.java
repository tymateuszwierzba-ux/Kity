package pl.tymek24.fortcodekits.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import pl.tymek24.fortcodekits.model.SortMode;

public class GuiSession {
  private static final class Entry {
    SortMode sort = SortMode.DEFAULT;
    String category = "";
    String search = "";
  }

  private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<UUID, Entry>();

  private GuiSession() {}

  private static Entry entry(UUID uuid) {
    return ENTRIES.computeIfAbsent(uuid, key -> new Entry());
  }

  public static SortMode sort(UUID uuid) {
    return entry(uuid).sort;
  }

  public static SortMode cycleSort(UUID uuid) {
    Entry entry = entry(uuid);
    entry.sort = entry.sort.next();
    return entry.sort;
  }

  public static void setSort(UUID uuid, SortMode mode) {
    entry(uuid).sort = mode == null ? SortMode.DEFAULT : mode;
  }

  public static String category(UUID uuid) {
    return entry(uuid).category;
  }

  public static void setCategory(UUID uuid, String category) {
    entry(uuid).category = category == null ? "" : category;
  }

  public static String cycleCategory(UUID uuid, java.util.List<String> available) {
    Entry entry = entry(uuid);
    if (available == null || available.isEmpty()) {
      entry.category = "";
      return entry.category;
    }
    int index = -1;
    for (int i = 0; i < available.size(); i++) {
      if (available.get(i).equalsIgnoreCase(entry.category)) {
        index = i;
        break;
      }
    }
    int next = index + 1;
    if (next >= available.size()) {
      entry.category = "";
    } else {
      entry.category = available.get(next);
    }
    return entry.category;
  }

  public static String search(UUID uuid) {
    return entry(uuid).search;
  }

  public static void setSearch(UUID uuid, String query) {
    entry(uuid).search = query == null ? "" : query;
  }

  public static void clear(UUID uuid) {
    ENTRIES.remove(uuid);
  }
}
