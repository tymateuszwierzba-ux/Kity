package pl.tymek24.fortcodekits.integration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LuckPermsHook {
  private LuckPermsHook() {}

  public static boolean isLuckPermsAvailable() {
    return Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
  }

  public static LuckPerms getApi() {
    if (!LuckPermsHook.isLuckPermsAvailable()) {
      return null;
    }
    try {
      return LuckPermsProvider.get();
    } catch (IllegalStateException ex) {
      return null;
    }
  }

  public static String normalizeGroup(String raw) {
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

  public static List<Group> getGroupsSorted() {
    LuckPerms api = LuckPermsHook.getApi();
    if (api == null) {
      return List.of();
    }
    ArrayList<Group> groups = new ArrayList<>(api.getGroupManager().getLoadedGroups());
    groups.sort(Comparator.comparing((Group group) -> group.getName().toLowerCase(Locale.ROOT)));
    return groups;
  }

  public static String getGroupPrefix(String groupName) {
    LuckPerms api = LuckPermsHook.getApi();
    if (api == null) {
      return "";
    }
    String normalized = LuckPermsHook.normalizeGroup(groupName);
    if (normalized == null) {
      return "";
    }
    Group group = api.getGroupManager().getGroup(normalized);
    if (group == null) {
      return "";
    }
    QueryOptions options = api.getContextManager().getStaticQueryOptions();
    String prefix = group.getCachedData().getMetaData(options).getPrefix();
    return prefix == null ? "" : prefix;
  }

  public static boolean isUserInGroup(Player player, String groupName) {
    LuckPerms api = LuckPermsHook.getApi();
    if (api == null) {
      return false;
    }
    String normalized = LuckPermsHook.normalizeGroup(groupName);
    if (normalized == null) {
      return false;
    }
    User user = api.getUserManager().getUser(player.getUniqueId());
    if (user == null) {
      return false;
    }
    Optional<QueryOptions> queryOptions = api.getContextManager().getQueryOptions(user);
    QueryOptions options =
        queryOptions.orElseGet(() -> api.getContextManager().getStaticQueryOptions());
    return user.getInheritedGroups(options).stream()
        .anyMatch(group -> group.getName().equalsIgnoreCase(normalized));
  }
}
