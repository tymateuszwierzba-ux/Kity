package pl.tymek24.fortcodekits.listener;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.gui.GuiSession;
import pl.tymek24.fortcodekits.model.Kit;
import pl.tymek24.fortcodekits.util.TimeUtil;

public class ChatInputListener implements Listener {
  private final FortCodeKits plugin;
  private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<UUID, PendingInput>();

  public ChatInputListener(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  public void requestInput(Player player, ChatInputType type, String kitId) {
    this.pending.put(player.getUniqueId(), new PendingInput(type, kitId));
    this.plugin.sendMessage(player, this.promptKey(type));
    this.plugin.playConfiguredSound(player, "sounds.click");
  }

  public void clear(UUID uuid) {
    this.pending.remove(uuid);
  }

  @EventHandler
  public void onChat(AsyncPlayerChatEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    PendingInput input = this.pending.get(uuid);
    if (input == null) {
      return;
    }
    event.setCancelled(true);
    String message = event.getMessage().trim();
    Bukkit.getScheduler()
        .runTask(this.plugin, () -> this.handleInput(event.getPlayer(), input, message));
  }

  private void handleInput(Player player, PendingInput input, String message) {
    if (message.equalsIgnoreCase("cancel")
        || message.equalsIgnoreCase("anuluj")
        || message.equalsIgnoreCase("-")) {
      this.pending.remove(player.getUniqueId());
      this.plugin.sendMessage(player, "input-cancelled");
      return;
    }
    switch (input.type()) {
      case SET_NAME -> {
        Kit kit = this.getKit(player, input.kitId());
        if (kit == null) return;
        kit.setDisplayName(message);
        this.finishEdit(player, kit);
      }
      case SET_COOLDOWN -> {
        Kit kit = this.getKit(player, input.kitId());
        if (kit == null) return;
        long value = TimeUtil.parseDuration(message);
        if (value < 0L) {
          Long raw = this.parseLong(message);
          if (raw == null || raw < 0L) {
            this.plugin.sendMessage(player, "input-invalid-number");
            return;
          }
          value = raw;
        }
        kit.setCooldownSeconds(value);
        this.finishEdit(player, kit);
      }
      case SET_SLOT -> {
        Kit kit = this.getKit(player, input.kitId());
        if (kit == null) return;
        Integer slot = this.parseInt(message);
        if (slot == null) {
          this.plugin.sendMessage(player, "input-invalid-number");
          return;
        }
        if (slot < -1 || slot > 53) {
          this.plugin.sendMessage(player, "input-invalid-slot");
          return;
        }
        kit.setGuiSlot(slot == -1 ? null : slot);
        this.finishEdit(player, kit);
      }
      case SET_PRICE -> {
        Kit kit = this.getKit(player, input.kitId());
        if (kit == null) return;
        String normalized = message.replace(',', '.').trim();
        Double price;
        try {
          price = Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
          this.plugin.sendMessage(player, "input-invalid-number");
          return;
        }
        if (price == null || price < 0.0D) {
          this.plugin.sendMessage(player, "input-invalid-number");
          return;
        }
        kit.setPrice(price);
        this.finishEdit(player, kit);
      }
      case SET_CATEGORY -> {
        Kit kit = this.getKit(player, input.kitId());
        if (kit == null) return;
        if (message.equalsIgnoreCase("none") || message.equalsIgnoreCase("brak")) {
          kit.setCategory(null);
        } else {
          kit.setCategory(message);
        }
        this.finishEdit(player, kit);
      }
      case CREATE_KIT -> {
        String id = this.sanitizeId(message);
        if (id.isEmpty()) {
          this.plugin.sendMessage(player, "input-invalid-id");
          return;
        }
        if (this.plugin.getKitManager().getKit(id).isPresent()) {
          this.plugin.sendMessage(player, "kit-exists", Map.of("{kit}", id));
          return;
        }
        this.pending.remove(player.getUniqueId());
        this.plugin.createKitFromInventory(player, id);
        this.plugin.getGuiManager().openAdmin(player);
        this.plugin
            .getGuiManager()
            .refreshOpenGuis(this.plugin.getKitManager().getKit(id).orElse(null));
      }
      case SEARCH_PLAYER -> {
        this.pending.remove(player.getUniqueId());
        GuiSession.setSearch(player.getUniqueId(), message);
        if (message.isBlank()) {
          this.plugin.sendMessage(player, "search-cleared");
        } else {
          Map<String, String> ph = Map.of("{query}", message);
          this.plugin.sendMessage(player, "search-applied", ph);
        }
        this.plugin.getGuiManager().openPlayer(player);
      }
      case SEARCH_ADMIN -> {
        this.pending.remove(player.getUniqueId());
        GuiSession.setSearch(player.getUniqueId(), message);
        if (message.isBlank()) {
          this.plugin.sendMessage(player, "search-cleared");
        } else {
          Map<String, String> ph = Map.of("{query}", message);
          this.plugin.sendMessage(player, "search-applied", ph);
        }
        this.plugin.getGuiManager().openAdmin(player);
      }
    }
  }

  private void finishEdit(Player player, Kit kit) {
    this.plugin.getKitManager().save();
    this.pending.remove(player.getUniqueId());
    this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
    this.plugin.getGuiManager().openEditor(player, kit);
    this.plugin.getGuiManager().refreshOpenGuis(kit);
    this.plugin.playConfiguredSound(player, "sounds.click");
  }

  private Kit getKit(Player player, String kitId) {
    if (kitId == null) {
      this.plugin.sendMessage(player, "kit-not-found");
      return null;
    }
    return this.plugin
        .getKitManager()
        .getKit(kitId)
        .orElseGet(
            () -> {
              this.plugin.sendMessage(player, "kit-not-found");
              this.pending.remove(player.getUniqueId());
              return null;
            });
  }

  private String promptKey(ChatInputType type) {
    return switch (type) {
      case SET_NAME -> "input-request-name";
      case SET_COOLDOWN -> "input-request-cooldown";
      case SET_SLOT -> "input-request-slot";
      case SET_PRICE -> "input-request-price";
      case SET_CATEGORY -> "input-request-category";
      case CREATE_KIT -> "input-request-create-id";
      case SEARCH_PLAYER -> "input-request-search";
      case SEARCH_ADMIN -> "input-request-search";
    };
  }

  private Long parseLong(String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private Integer parseInt(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private String sanitizeId(String value) {
    String id = Objects.requireNonNullElse(value, "").trim().toLowerCase(Locale.ROOT);
    id = id.replace(' ', '_');
    return id.replaceAll("[^a-z0-9_\\-]", "");
  }

  private record PendingInput(ChatInputType type, String kitId) {}
}
