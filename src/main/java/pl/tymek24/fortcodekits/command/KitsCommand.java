package pl.tymek24.fortcodekits.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.animation.KitClaimAnimationManager;
import pl.tymek24.fortcodekits.gui.GuiSession;
import pl.tymek24.fortcodekits.gui.GuiType;
import pl.tymek24.fortcodekits.integration.LuckPermsHook;
import pl.tymek24.fortcodekits.model.Kit;
import pl.tymek24.fortcodekits.model.KitRarity;

public class KitsCommand implements CommandExecutor, TabCompleter {
  private static final Pattern WRAPPED_ANIM_PATTERN =
      Pattern.compile("(?is)^<anim:([a-z0-9_\\-]+)>(.*)</anim>$");
  private static final Pattern WRAPPED_GRADIENT_PATTERN =
      Pattern.compile("(?is)^<gradient:[^>]+>(.*)</gradient>$");
  private static final Pattern WRAPPED_GRAD_PATTERN =
      Pattern.compile("(?is)^<grad:[^>]+>(.*)</grad>$");
  private final FortCodeKits plugin;

  public KitsCommand(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Komenda tylko dla gracza.");
      return true;
    }
    Player player = (Player) sender;
    boolean adminLabel = label.equalsIgnoreCase("adminkit");
    if (args.length == 0) {
      if (adminLabel) {
        if (!player.hasPermission("fortcode.kits.admin")) {
          return this.noPerm(player);
        }
        this.plugin.getGuiManager().openAdmin(player);
      } else {
        this.plugin.getGuiManager().openPlayer(player);
      }
      return true;
    }
    String sub = args[0].toLowerCase(Locale.ROOT);
    if (adminLabel) {
      return this.handleAdmin(player, sub, args);
    }
    return this.handlePlayer(player, sub, args);
  }

  private boolean handlePlayer(Player player, String sub, String[] args) {
    switch (sub) {
      case "open":
        {
          this.plugin.getGuiManager().openPlayer(player);
          break;
        }
      case "preview":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          this.plugin.getGuiManager().openPreview(player, kit, GuiType.PLAYER);
          break;
        }
      case "favorites":
      case "ulubione":
        {
          if (!player.hasPermission("fortcode.kits.favorites")) {
            return this.noPerm(player);
          }
          this.plugin.getGuiManager().openFavorites(player);
          break;
        }
      case "favorite":
      case "ulubiony":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          boolean added = this.plugin.getFavoriteManager().toggle(player.getUniqueId(), kit.getId());
          this.plugin.getFavoriteManager().save();
          Map<String, String> ph = Map.of("{kit}", kit.getDisplayName());
          this.plugin.sendMessage(player, added ? "favorite-added" : "favorite-removed", ph);
          break;
        }
      case "stats":
      case "statystyki":
        {
          if (!player.hasPermission("fortcode.kits.stats")) {
            return this.noPerm(player);
          }
          this.plugin.getGuiManager().openStats(player);
          break;
        }
      case "history":
      case "historia":
        {
          if (!player.hasPermission("fortcode.kits.stats")) {
            return this.noPerm(player);
          }
          this.plugin.getGuiManager().openHistory(player);
          break;
        }
      case "top":
      case "ranking":
        {
          this.plugin.getGuiManager().openTop(player);
          break;
        }
      case "settings":
      case "ustawienia":
        {
          if (!player.hasPermission("fortcode.kits.settings")) {
            return this.noPerm(player);
          }
          this.plugin.getGuiManager().openSettings(player);
          break;
        }
      case "search":
      case "szukaj":
        {
          if (args.length < 2) {
            GuiSession.setSearch(player.getUniqueId(), "");
            this.plugin.sendMessage(player, "search-cleared");
          } else {
            String query = this.join(args, 1);
            GuiSession.setSearch(player.getUniqueId(), query);
            Map<String, String> ph = Map.of("{query}", query);
            this.plugin.sendMessage(player, "search-applied", ph);
          }
          this.plugin.getGuiManager().openPlayer(player);
          break;
        }
      case "reload":
        {
          if (!player.hasPermission("fortcode.kits.reload")) {
            return this.noPerm(player);
          }
          this.plugin.reloadPlugin();
          this.plugin.sendMessage(player, "reload");
          break;
        }
      case "give":
        {
          if (!player.hasPermission("fortcode.kits.give")) {
            return this.noPerm(player);
          }
          if (args.length < 3) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          Player target = Bukkit.getPlayerExact((String) args[2]);
          if (target == null) {
            this.plugin.sendMessage(player, "player-not-found");
            return true;
          }
          this.plugin.giveKit(target, kit, true);
          this.plugin.sendMessage(
              player,
              "kit-given",
              Map.of("{kit}", kit.getDisplayName(), "{player}", target.getName()));
          break;
        }
      case "reset":
        {
          if (!player.hasPermission("fortcode.kits.admin")) {
            return this.noPerm(player);
          }
          if (args.length < 3) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          OfflinePlayer offline = Bukkit.getOfflinePlayer((String) args[2]);
          this.plugin.getCooldownManager().reset(offline.getUniqueId(), kit.getId());
          this.plugin.sendMessage(
              player,
              "cooldown-reset",
              Map.of(
                  "{kit}",
                  kit.getDisplayName(),
                  "{player}",
                  offline.getName() == null ? "unknown" : offline.getName()));
          break;
        }
      default:
        {
          this.plugin.getGuiManager().openPlayer(player);
        }
    }
    return true;
  }

  private boolean handleAdmin(Player player, String sub, String[] args) {
    if (!player.hasPermission("fortcode.kits.admin")) {
      return this.noPerm(player);
    }
    switch (sub) {
      case "open":
        {
          this.plugin.getGuiManager().openAdmin(player);
          break;
        }
      case "create":
        {
          if (args.length < 2) {
            return false;
          }
          String id = this.sanitizeId(args[1]);
          if (id.isEmpty()) {
            this.plugin.sendMessage(player, "input-invalid-id");
            return true;
          }
          if (this.plugin.getKitManager().getKit(id).isPresent()) {
            this.plugin.sendMessage(player, "kit-exists", Map.of("{kit}", id));
            return true;
          }
          this.plugin.createKitFromInventory(player, id);
          this.plugin
              .getKitManager()
              .getKit(id)
              .ifPresent(this.plugin.getGuiManager()::refreshOpenGuis);
          break;
        }
      case "edit":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          this.plugin.getGuiManager().openEditor(player, kit);
          break;
        }
      case "delete":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          this.plugin.getKitManager().remove(kit.getId());
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-deleted", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuisAfterDelete(kit.getId());
          break;
        }
      case "setitems":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          kit.setItems(this.plugin.collectInventoryItems(player));
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "seticon":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          ItemStack hand = player.getInventory().getItemInMainHand();
          if (hand.getType().isAir()) {
            this.plugin.sendMessage(player, "need-item-hand");
            return true;
          }
          kit.setIcon(hand);
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "setname":
        {
          if (args.length < 3) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          String name = this.join(args, 2);
          kit.setDisplayName(name);
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "setcooldown":
        {
          if (args.length < 3) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          Long value = this.parseLong(args[2]);
          if (value == null || value < 0L) {
            this.plugin.sendMessage(player, "input-invalid-number");
            return true;
          }
          kit.setCooldownSeconds(value);
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "setslot":
        {
          if (args.length < 3) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          Integer slot = this.parseInt(args[2]);
          if (slot == null) {
            this.plugin.sendMessage(player, "input-invalid-number");
            return true;
          }
          if (slot < -1 || slot > 53) {
            this.plugin.sendMessage(player, "input-invalid-slot");
            return true;
          }
          kit.setGuiSlot(slot == -1 ? null : slot);
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "setrank":
        {
          if (args.length < 3) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          String raw = args[2];
          if (raw.equalsIgnoreCase("none") || raw.equalsIgnoreCase("-")) {
            kit.setRequiredGroup(null);
          } else {
            kit.setRequiredGroup(raw);
          }
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "animation":
      case "animacja":
        {
          return this.handleAnimation(player, args);
        }
      case "claimanim":
      case "animkit":
      case "kitanim":
        {
          return this.handleClaimAnimation(player, args);
        }
      case "toggle-onetime":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          kit.setOneTime(!kit.isOneTime());
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "setprice":
      case "cena":
        {
          if (args.length < 3) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          Double price = this.parseDouble(args[2].replace(',', '.'));
          if (price == null || price < 0.0D) {
            this.plugin.sendMessage(player, "input-invalid-number");
            return true;
          }
          kit.setPrice(price);
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "setcategory":
      case "kategoria":
        {
          if (args.length < 3) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          String raw = args[2];
          if (raw.equalsIgnoreCase("none") || raw.equalsIgnoreCase("-")) {
            kit.setCategory(null);
          } else {
            kit.setCategory(raw);
          }
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "setrarity":
      case "rzadkosc":
        {
          if (args.length < 3) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          String raw = args[2];
          if (raw.equalsIgnoreCase("none") || raw.equalsIgnoreCase("-")) {
            kit.setRarity(null);
          } else {
            KitRarity rarity = KitRarity.from(raw);
            if (rarity == null) {
              player.sendMessage(
                  this.plugin.formatText("&#FF5555Nieznana rzadkosc. Dostepne: common, uncommon, rare, epic, legendary"));
              return true;
            }
            kit.setRarity(rarity);
          }
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "setclaimlimit":
      case "limit":
        {
          if (args.length < 3) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          String raw = args[2];
          if (raw.equalsIgnoreCase("none") || raw.equalsIgnoreCase("-")) {
            kit.setClaimCountLimit(null);
          } else {
            Integer limit = this.parseInt(raw);
            if (limit == null || limit < 0) {
              this.plugin.sendMessage(player, "input-invalid-number");
              return true;
            }
            kit.setClaimCountLimit(limit);
          }
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "toggle-enabled":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          kit.setEnabled(!kit.isEnabled());
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "toggle-hidden":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          kit.setHidden(!kit.isHidden());
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "toggle-welcome":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          kit.setWelcome(!kit.isWelcome());
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
          this.plugin.getGuiManager().refreshOpenGuis(kit);
          break;
        }
      case "duplicate":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          String newId =
              args.length >= 3
                  ? this.sanitizeId(args[2])
                  : this.nextCopyId(kit.getId());
          if (newId.isEmpty()) {
            this.plugin.sendMessage(player, "input-invalid-id");
            return true;
          }
          if (this.plugin.getKitManager().getKit(newId).isPresent()) {
            this.plugin.sendMessage(player, "kit-exists", Map.of("{kit}", newId));
            return true;
          }
          Kit copy = kit.duplicate(newId);
          this.plugin.getKitManager().upsert(copy);
          this.plugin.getKitManager().save();
          this.plugin.sendMessage(player, "kit-duplicated", Map.of("{kit}", copy.getId()));
          this.plugin.getGuiManager().refreshOpenGuis(copy);
          break;
        }
      case "resetall":
        {
          if (args.length < 2) {
            return false;
          }
          Kit kit = this.getKitOrMsg(player, args[1]);
          if (kit == null) {
            return true;
          }
          this.plugin.getCooldownManager().resetAllForKit(kit.getId());
          this.plugin.sendMessage(player, "kit-cooldowns-reset", Map.of("{kit}", kit.getId()));
          break;
        }
      case "stats":
      case "statystyki":
        {
          if (args.length >= 2) {
            Kit kit = this.getKitOrMsg(player, args[1]);
            if (kit == null) {
              return true;
            }
            Map<String, String> ph = new java.util.HashMap<String, String>();
            ph.put("{kit}", kit.getDisplayName());
            ph.put("{claims}", String.valueOf(this.plugin.getStatsManager().getKitClaims(kit.getId())));
            this.plugin.sendMessage(player, "kit-stats-line", ph);
            break;
          }
          this.plugin.getGuiManager().openStats(player);
          break;
        }
      case "history":
      case "historia":
        {
          this.plugin.getGuiManager().openHistory(player);
          break;
        }
      case "top":
      case "ranking":
        {
          this.plugin.getGuiManager().openTop(player);
          break;
        }
      case "search":
      case "szukaj":
        {
          if (args.length < 2) {
            GuiSession.setSearch(player.getUniqueId(), "");
            this.plugin.sendMessage(player, "search-cleared");
          } else {
            String query = this.join(args, 1);
            GuiSession.setSearch(player.getUniqueId(), query);
            Map<String, String> ph = Map.of("{query}", query);
            this.plugin.sendMessage(player, "search-applied", ph);
          }
          this.plugin.getGuiManager().openAdmin(player);
          break;
        }
      case "clearhistory":
        {
          this.plugin.getStatsManager().clearHistory();
          this.plugin.getStatsManager().save();
          this.plugin.sendMessage(player, "history-cleared");
          break;
        }
      case "clearstats":
        {
          if (args.length >= 2 && !args[1].equalsIgnoreCase("all")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            this.plugin.getStatsManager().resetPlayer(target.getUniqueId());
            this.plugin.getStatsManager().save();
            Map<String, String> ph =
                Map.of("{player}", target.getName() == null ? args[1] : target.getName());
            this.plugin.sendMessage(player, "player-stats-reset", ph);
          } else {
            this.plugin.getStatsManager().clearHistory();
            for (Kit kit : this.plugin.getKitManager().getKits()) {
              this.plugin.getStatsManager().clearKit(kit.getId());
            }
            this.plugin.getStatsManager().save();
            this.plugin.sendMessage(player, "stats-cleared");
          }
          break;
        }
      case "reload":
        {
          if (!player.hasPermission("fortcode.kits.reload")) {
            return this.noPerm(player);
          }
          this.plugin.reloadPlugin();
          this.plugin.sendMessage(player, "reload");
          break;
        }
      case "give":
      case "reset":
        {
          this.handlePlayer(player, sub, args);
          break;
        }
      default:
        {
          this.plugin.getGuiManager().openAdmin(player);
        }
    }
    return true;
  }

  private boolean handleClaimAnimation(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(
          this.plugin.formatText(
              "&#FFAA00Uzycie: &f/adminkit claimanim <kit> [status|on|off|default]"));
      player.sendMessage(
          this.plugin.formatText(
              "&#AAAAAAOpcje: &fmode, interval, jitter, batch, split, max-items, delay, glow, glow-remove, glow-enchant"));
      return true;
    }

    Kit kit = this.getKitOrMsg(player, args[1]);
    if (kit == null) {
      return true;
    }

    if (args.length == 2 || args[2].equalsIgnoreCase("status") || args[2].equalsIgnoreCase("list")) {
      this.sendClaimAnimStatus(player, kit);
      return true;
    }

    String action = args[2].toLowerCase(Locale.ROOT);
    switch (action) {
      case "reset":
        {
          kit.setClaimAnimationEnabled(null);
          kit.setClaimAnimationMode(null);
          kit.setClaimAnimationIntervalTicks(null);
          kit.setClaimAnimationIntervalJitterTicks(null);
          kit.setClaimAnimationBatchSize(null);
          kit.setClaimAnimationStartDelayTicks(null);
          kit.setClaimAnimationSplitStacks(null);
          kit.setClaimAnimationMaxItems(null);
          kit.setClaimAnimationTemporaryGlowEnabled(null);
          kit.setClaimAnimationTemporaryGlowRemoveAfterTicks(null);
          kit.setClaimAnimationTemporaryGlowEnchantment(null);
          return this.finishClaimAnimUpdate(player, kit, "reset");
        }
      case "on":
      case "off":
      case "default":
        {
          Boolean value = this.parseBooleanOrDefault(action);
          kit.setClaimAnimationEnabled(value);
          return this.finishClaimAnimUpdate(player, kit, "enabled");
        }
      case "mode":
        {
          if (args.length < 4) {
            player.sendMessage(
                this.plugin.formatText(
                    "&#FFAA00Uzycie: &f/adminkit claimanim " + kit.getId() + " mode <" + this.getClaimModesHelp() + "|default>"));
            return true;
          }
          String rawMode = args[3];
          if (rawMode.equalsIgnoreCase("default")) {
            kit.setClaimAnimationMode(null);
          } else {
            if (!this.isSupportedClaimMode(rawMode)) {
              player.sendMessage(
                  this.plugin.formatText(
                      "&#FF5555Niepoprawny mode. Uzyj: " + this.getClaimModesHelp() + "."));
              return true;
            }
            kit.setClaimAnimationMode(rawMode);
          }
          return this.finishClaimAnimUpdate(player, kit, "mode");
        }
      case "interval":
      case "speed":
        {
          if (args.length < 4) {
            player.sendMessage(
                this.plugin.formatText(
                    "&#FFAA00Uzycie: &f/adminkit claimanim " + kit.getId() + " interval <ticks|default>"));
            return true;
          }
          if (args[3].equalsIgnoreCase("default")) {
            kit.setClaimAnimationIntervalTicks(null);
          } else {
            Integer ticks = this.parseInt(args[3]);
            if (ticks == null || ticks < 1) {
              player.sendMessage(this.plugin.formatText("&#FF5555Ticks musi byc >= 1."));
              return true;
            }
            kit.setClaimAnimationIntervalTicks(ticks);
          }
          return this.finishClaimAnimUpdate(player, kit, "interval");
        }
      case "jitter":
      case "interval-jitter":
        {
          if (args.length < 4) {
            player.sendMessage(
                this.plugin.formatText(
                    "&#FFAA00Uzycie: &f/adminkit claimanim " + kit.getId() + " jitter <ticks|default>"));
            return true;
          }
          if (args[3].equalsIgnoreCase("default")) {
            kit.setClaimAnimationIntervalJitterTicks(null);
          } else {
            Integer ticks = this.parseInt(args[3]);
            if (ticks == null || ticks < 0) {
              player.sendMessage(this.plugin.formatText("&#FF5555Ticks musi byc >= 0."));
              return true;
            }
            kit.setClaimAnimationIntervalJitterTicks(ticks);
          }
          return this.finishClaimAnimUpdate(player, kit, "jitter");
        }
      case "batch":
      case "batch-size":
        {
          if (args.length < 4) {
            player.sendMessage(
                this.plugin.formatText(
                    "&#FFAA00Uzycie: &f/adminkit claimanim " + kit.getId() + " batch <ilosc|default>"));
            return true;
          }
          if (args[3].equalsIgnoreCase("default")) {
            kit.setClaimAnimationBatchSize(null);
          } else {
            Integer value = this.parseInt(args[3]);
            if (value == null || value < 1) {
              player.sendMessage(this.plugin.formatText("&#FF5555Batch musi byc >= 1."));
              return true;
            }
            kit.setClaimAnimationBatchSize(value);
          }
          return this.finishClaimAnimUpdate(player, kit, "batch");
        }
      case "split":
      case "split-stacks":
        {
          if (args.length < 4) {
            player.sendMessage(
                this.plugin.formatText(
                    "&#FFAA00Uzycie: &f/adminkit claimanim " + kit.getId() + " split <on|off|default>"));
            return true;
          }
          Boolean value = this.parseBooleanOrDefault(args[3]);
          if (value == null && !args[3].equalsIgnoreCase("default")) {
            player.sendMessage(this.plugin.formatText("&#FF5555Uzyj: on/off/default."));
            return true;
          }
          kit.setClaimAnimationSplitStacks(value);
          return this.finishClaimAnimUpdate(player, kit, "split");
        }
      case "max-items":
      case "limit":
        {
          if (args.length < 4) {
            player.sendMessage(
                this.plugin.formatText(
                    "&#FFAA00Uzycie: &f/adminkit claimanim " + kit.getId() + " max-items <0+|default>"));
            return true;
          }
          if (args[3].equalsIgnoreCase("default")) {
            kit.setClaimAnimationMaxItems(null);
          } else {
            Integer value = this.parseInt(args[3]);
            if (value == null || value < 0) {
              player.sendMessage(this.plugin.formatText("&#FF5555Wartosc musi byc >= 0."));
              return true;
            }
            kit.setClaimAnimationMaxItems(value);
          }
          return this.finishClaimAnimUpdate(player, kit, "max-items");
        }
      case "delay":
      case "start-delay":
        {
          if (args.length < 4) {
            player.sendMessage(
                this.plugin.formatText(
                    "&#FFAA00Uzycie: &f/adminkit claimanim " + kit.getId() + " delay <ticks|default>"));
            return true;
          }
          if (args[3].equalsIgnoreCase("default")) {
            kit.setClaimAnimationStartDelayTicks(null);
          } else {
            Integer ticks = this.parseInt(args[3]);
            if (ticks == null || ticks < 0) {
              player.sendMessage(this.plugin.formatText("&#FF5555Ticks musi byc >= 0."));
              return true;
            }
            kit.setClaimAnimationStartDelayTicks(ticks);
          }
          return this.finishClaimAnimUpdate(player, kit, "delay");
        }
      case "glow":
        {
          if (args.length < 4) {
            player.sendMessage(
                this.plugin.formatText(
                    "&#FFAA00Uzycie: &f/adminkit claimanim " + kit.getId() + " glow <on|off|default>"));
            return true;
          }
          Boolean value = this.parseBooleanOrDefault(args[3]);
          if (value == null && !args[3].equalsIgnoreCase("default")) {
            player.sendMessage(this.plugin.formatText("&#FF5555Uzyj: on/off/default."));
            return true;
          }
          kit.setClaimAnimationTemporaryGlowEnabled(value);
          return this.finishClaimAnimUpdate(player, kit, "glow");
        }
      case "glow-remove":
      case "glow-time":
        {
          if (args.length < 4) {
            player.sendMessage(
                this.plugin.formatText(
                    "&#FFAA00Uzycie: &f/adminkit claimanim " + kit.getId() + " glow-remove <ticks|default>"));
            return true;
          }
          if (args[3].equalsIgnoreCase("default")) {
            kit.setClaimAnimationTemporaryGlowRemoveAfterTicks(null);
          } else {
            Integer ticks = this.parseInt(args[3]);
            if (ticks == null || ticks < 1) {
              player.sendMessage(this.plugin.formatText("&#FF5555Ticks musi byc >= 1."));
              return true;
            }
            kit.setClaimAnimationTemporaryGlowRemoveAfterTicks(ticks);
          }
          return this.finishClaimAnimUpdate(player, kit, "glow-remove");
        }
      case "glow-enchant":
      case "enchant":
        {
          if (args.length < 4) {
            player.sendMessage(
                this.plugin.formatText(
                    "&#FFAA00Uzycie: &f/adminkit claimanim " + kit.getId() + " glow-enchant <minecraft:key|default>"));
            return true;
          }
          if (args[3].equalsIgnoreCase("default")) {
            kit.setClaimAnimationTemporaryGlowEnchantment(null);
          } else {
            kit.setClaimAnimationTemporaryGlowEnchantment(args[3]);
          }
          return this.finishClaimAnimUpdate(player, kit, "glow-enchant");
        }
      default:
        {
          player.sendMessage(this.plugin.formatText("&#FF5555Nieznana opcja claimanim: &f" + action));
          this.sendClaimAnimStatus(player, kit);
          return true;
        }
    }
  }

  private boolean finishClaimAnimUpdate(Player player, Kit kit, String changed) {
    this.plugin.getKitManager().save();
    this.plugin.getGuiManager().refreshOpenGuis(kit);
    this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
    player.sendMessage(
        this.plugin.formatText(
            "&#55FF55Zmieniono claimanim (&f" + changed + "&#55FF55) dla kita: &f" + kit.getId()));
    return true;
  }

  private void sendClaimAnimStatus(Player player, Kit kit) {
    player.sendMessage(this.plugin.formatText(""));
    player.sendMessage(
        this.plugin.formatText("&#6FD3FFClaimAnim dla kita &f" + kit.getId() + "&#6FD3FF:"));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7enabled: &f"
                + this.valueOrDefault(kit.getClaimAnimationEnabled())));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7mode: &f"
                + this.valueOrDefault(kit.getClaimAnimationMode())));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7interval: &f"
                + this.valueOrDefault(kit.getClaimAnimationIntervalTicks())));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7jitter: &f"
                + this.valueOrDefault(kit.getClaimAnimationIntervalJitterTicks())));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7batch: &f"
                + this.valueOrDefault(kit.getClaimAnimationBatchSize())));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7split: &f"
                + this.valueOrDefault(kit.getClaimAnimationSplitStacks())));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7max-items: &f"
                + this.valueOrDefault(kit.getClaimAnimationMaxItems())));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7delay: &f"
                + this.valueOrDefault(kit.getClaimAnimationStartDelayTicks())));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7modes: &f"
                + this.getClaimModesHelp()));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7glow: &f"
                + this.valueOrDefault(kit.getClaimAnimationTemporaryGlowEnabled())));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7glow-remove: &f"
                + this.valueOrDefault(kit.getClaimAnimationTemporaryGlowRemoveAfterTicks())));
    player.sendMessage(
        this.plugin.formatText(
            " &8- &7glow-enchant: &f"
                + this.valueOrDefault(kit.getClaimAnimationTemporaryGlowEnchantment())));
    player.sendMessage(
        this.plugin.formatText(
            "&#AAAAAAUzycie: &f/adminkit claimanim "
                + kit.getId()
                + " <status|on|off|default|reset|mode|interval|jitter|batch|split|max-items|delay|glow|glow-remove|glow-enchant>"));
  }

  private String valueOrDefault(Object value) {
    return value == null ? "default" : String.valueOf(value);
  }

  private boolean handleAnimation(Player player, String[] args) {
    if (args.length < 2) {
      player.sendMessage(
          this.plugin.formatText("&#FFAA00Uzycie: &f/adminkit animation <kit> [nazwa|none|list]"));
      player.sendMessage(
          this.plugin.formatText("&#AAAAAAPrzyklad: &f/adminkit animation start rainbow"));
      player.sendMessage(
          this.plugin.formatText(
              "&#AAAAAAPolski alias: &f/adminkit animacja <kit> [nazwa|none|list]"));
      List<String> ids =
          this.plugin.getKitManager().getKits().stream().map(Kit::getId).sorted().toList();
      if (!ids.isEmpty()) {
        player.sendMessage(
            this.plugin.formatText(
                "&#6FD3FFDostepne kity: &f" + String.join((CharSequence) ", ", ids)));
      }
      return true;
    }
    Kit kit = this.getKitOrMsg(player, args[1]);
    if (kit == null) {
      return true;
    }
    if (args.length == 2 || args[2].equalsIgnoreCase("list")) {
      this.sendAnimationList(player, kit);
      return true;
    }
    String selected = args[2].trim().toLowerCase(Locale.ROOT);
    if (selected.isBlank()) {
      this.sendAnimationList(player, kit);
      return true;
    }
    if (selected.equals("none") || selected.equals("off") || selected.equals("-")) {
      String base = this.stripDisplayEffects(kit.getDisplayName());
      kit.setDisplayName(base.isBlank() ? kit.getId() : base);
      this.plugin.getKitManager().save();
      this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
      player.sendMessage(
          this.plugin.formatText("&#55FF55Animacja usunieta dla kita: &f" + kit.getId()));
      this.plugin.getGuiManager().refreshOpenGuis(kit);
      return true;
    }
    if (!this.plugin.getTextAnimationManager().hasAnimation(selected)) {
      player.sendMessage(this.plugin.formatText("&#FF5555Nie ma animacji: &f" + args[2]));
      this.sendAnimationList(player, kit);
      return true;
    }
    String base = this.stripDisplayEffects(kit.getDisplayName());
    if (base.isBlank()) {
      base = kit.getId();
    }
    kit.setDisplayName("<anim:" + selected + ">" + base + "</anim>");
    this.plugin.getKitManager().save();
    this.plugin.sendMessage(player, "kit-updated", Map.of("{kit}", kit.getDisplayName()));
    player.sendMessage(
        this.plugin.formatText(
            "&#55FF55Ustawiono animacje &f" + selected + " &#55FF55dla kita: &f" + kit.getId()));
    this.plugin.getGuiManager().refreshOpenGuis(kit);
    return true;
  }

  private void sendAnimationList(Player player, Kit kit) {
    List<String> names = this.plugin.getTextAnimationManager().getAnimationNames();
    String current = this.extractAnimationName(kit.getDisplayName());
    player.sendMessage(this.plugin.formatText(""));
    player.sendMessage(
        this.plugin.formatText("&#6FD3FFAnimacje dla kita &f" + kit.getId() + "&#6FD3FF:"));
    player.sendMessage(
        this.plugin.formatText("&#AAAAAAAktualna: &f" + (current == null ? "brak" : current)));
    if (names.isEmpty()) {
      player.sendMessage(this.plugin.formatText("&#FF5555Brak animacji w animations.yml"));
      return;
    }
    for (String name : names) {
      boolean active = current != null && current.equalsIgnoreCase(name);
      String marker = active ? " &a(aktualna)" : "";
      String preview = "<anim:" + name + ">" + name.toUpperCase(Locale.ROOT) + "</anim>";
      player.sendMessage(
          this.plugin.formatText(" &8- " + preview + " &7(&f" + name + "&7)" + marker));
    }
    player.sendMessage(
        this.plugin.formatText(
            "&#AAAAAAUzycie: &f/adminkit animation " + kit.getId() + " <nazwa|none|list>"));
  }

  private String stripDisplayEffects(String value) {
    Matcher gradMatcher;
    Matcher gradientMatcher;
    String result = value == null ? "" : value.trim();
    Matcher animMatcher = WRAPPED_ANIM_PATTERN.matcher(result);
    if (animMatcher.matches()) {
      result = animMatcher.group(2);
    }
    if ((gradientMatcher = WRAPPED_GRADIENT_PATTERN.matcher(result)).matches()) {
      result = gradientMatcher.group(1);
    }
    if ((gradMatcher = WRAPPED_GRAD_PATTERN.matcher(result)).matches()) {
      result = gradMatcher.group(1);
    }
    return result;
  }

  private String extractAnimationName(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    Matcher matcher = WRAPPED_ANIM_PATTERN.matcher(value.trim());
    if (!matcher.matches()) {
      return null;
    }
    return matcher.group(1).toLowerCase(Locale.ROOT);
  }

  private Kit getKitOrMsg(Player player, String id) {
    return this.plugin
        .getKitManager()
        .getKit(id.toLowerCase(Locale.ROOT))
        .orElseGet(
            () -> {
              this.plugin.sendMessage(player, "kit-not-found");
              return null;
            });
  }

  private boolean noPerm(Player player) {
    this.plugin.sendMessage(player, "no-permission");
    return true;
  }

  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    boolean adminLabel = alias.equalsIgnoreCase("adminkit");
    List<String> subs =
        adminLabel
            ? List.of(
                "open",
                "create",
                "edit",
                "delete",
                "setitems",
                "seticon",
                "setname",
                "setcooldown",
                "setslot",
                "setrank",
                "setprice",
                "setcategory",
                "setrarity",
                "setclaimlimit",
                "toggle-onetime",
                "toggle-enabled",
                "toggle-hidden",
                "toggle-welcome",
                "duplicate",
                "resetall",
                "animation",
                "animacja",
                "claimanim",
                "animkit",
                "kitanim",
                "stats",
                "history",
                "top",
                "search",
                "clearhistory",
                "clearstats",
                "reload",
                "give",
                "reset")
            : List.of(
                "open",
                "preview",
                "reload",
                "give",
                "reset",
                "favorites",
                "favorite",
                "stats",
                "history",
                "top",
                "settings",
                "search");
    if (args.length == 1) {
      return subs.stream()
          .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
          .collect(Collectors.toList());
    }
    if (args.length == 2
        && (args[0].equalsIgnoreCase("give")
            || args[0].equalsIgnoreCase("reset")
            || args[0].equalsIgnoreCase("preview")
            || args[0].equalsIgnoreCase("edit")
            || args[0].equalsIgnoreCase("delete")
            || args[0].equalsIgnoreCase("setitems")
            || args[0].equalsIgnoreCase("seticon")
            || args[0].equalsIgnoreCase("setname")
            || args[0].equalsIgnoreCase("setcooldown")
            || args[0].equalsIgnoreCase("setslot")
            || args[0].equalsIgnoreCase("setrank")
            || args[0].equalsIgnoreCase("setprice")
            || args[0].equalsIgnoreCase("setcategory")
            || args[0].equalsIgnoreCase("setrarity")
            || args[0].equalsIgnoreCase("setclaimlimit")
            || args[0].equalsIgnoreCase("toggle-onetime")
            || args[0].equalsIgnoreCase("toggle-enabled")
            || args[0].equalsIgnoreCase("toggle-hidden")
            || args[0].equalsIgnoreCase("toggle-welcome")
            || args[0].equalsIgnoreCase("duplicate")
            || args[0].equalsIgnoreCase("resetall")
            || args[0].equalsIgnoreCase("favorite")
            || args[0].equalsIgnoreCase("ulubiony")
            || args[0].equalsIgnoreCase("animation")
            || args[0].equalsIgnoreCase("animacja")
            || args[0].equalsIgnoreCase("claimanim")
            || args[0].equalsIgnoreCase("animkit")
            || args[0].equalsIgnoreCase("kitanim"))) {
      return this.plugin.getKitManager().getKits().stream()
          .map(Kit::getId)
          .filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
          .collect(Collectors.toList());
    }
    if (args.length == 3
        && (args[0].equalsIgnoreCase("animation") || args[0].equalsIgnoreCase("animacja"))) {
      ArrayList<String> suggestions = new ArrayList<String>();
      suggestions.add("list");
      suggestions.add("none");
      suggestions.addAll(this.plugin.getTextAnimationManager().getAnimationNames());
      String prefix = args[2].toLowerCase(Locale.ROOT);
      return suggestions.stream()
          .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
          .distinct()
          .collect(Collectors.toList());
    }
    if (args.length == 3
        && (args[0].equalsIgnoreCase("claimanim")
            || args[0].equalsIgnoreCase("animkit")
            || args[0].equalsIgnoreCase("kitanim"))) {
      List<String> suggestions =
          List.of(
              "status",
              "reset",
              "on",
              "off",
              "default",
              "mode",
              "interval",
              "jitter",
              "batch",
              "split",
              "max-items",
              "delay",
              "glow",
              "glow-remove",
              "glow-enchant");
      String prefix = args[2].toLowerCase(Locale.ROOT);
      return suggestions.stream()
          .filter(s -> s.startsWith(prefix))
          .collect(Collectors.toList());
    }
    if (args.length == 4
        && (args[0].equalsIgnoreCase("claimanim")
            || args[0].equalsIgnoreCase("animkit")
            || args[0].equalsIgnoreCase("kitanim"))) {
      String mode = args[2].toLowerCase(Locale.ROOT);
      List<String> suggestions;
      if (mode.equals("mode")) {
        suggestions = new ArrayList<>(KitClaimAnimationManager.getSupportedModeNames());
        suggestions.add("default");
      } else if (mode.equals("glow") || mode.equals("split")) {
        suggestions = List.of("on", "off", "default");
      } else if (mode.equals("interval")
          || mode.equals("batch")
          || mode.equals("glow-remove")) {
        suggestions = List.of("1", "2", "5", "10", "20", "40", "default");
      } else if (mode.equals("jitter")
          || mode.equals("max-items")
          || mode.equals("delay")) {
        suggestions = List.of("0", "1", "2", "5", "10", "20", "40", "default");
      } else if (mode.equals("glow-enchant") || mode.equals("enchant")) {
        suggestions = List.of("minecraft:unbreaking", "minecraft:luck", "default");
      } else {
        suggestions = List.of();
      }
      String prefix = args[3].toLowerCase(Locale.ROOT);
      return suggestions.stream()
          .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
          .collect(Collectors.toList());
    }
    if (args.length == 3 && args[0].equalsIgnoreCase("setrank")) {
      ArrayList<String> suggestions = new ArrayList<String>();
      suggestions.add("none");
      if (LuckPermsHook.isLuckPermsAvailable()) {
        suggestions.addAll(
            LuckPermsHook.getGroupsSorted().stream()
                .map(group -> group.getName())
                .filter(
                    name ->
                        name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList()));
      }
      return suggestions.stream()
          .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
          .collect(Collectors.toList());
    }
    if (args.length == 3 && args[0].equalsIgnoreCase("setrarity")) {
      List<String> suggestions = List.of("none", "common", "uncommon", "rare", "epic", "legendary");
      String prefix = args[2].toLowerCase(Locale.ROOT);
      return suggestions.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
    }
    if (args.length == 3 && args[0].equalsIgnoreCase("setclaimlimit")) {
      List<String> suggestions = List.of("none", "1", "3", "5", "10");
      String prefix = args[2].toLowerCase(Locale.ROOT);
      return suggestions.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
    }
    if (args.length == 3 && args[0].equalsIgnoreCase("setprice")) {
      List<String> suggestions = List.of("0", "100", "500", "1000", "5000");
      String prefix = args[2].toLowerCase(Locale.ROOT);
      return suggestions.stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
    }
    if (args.length == 3
        && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("reset"))) {
      return Bukkit.getOnlinePlayers().stream()
          .map(OfflinePlayer::getName)
          .filter(Objects::nonNull)
          .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
          .collect(Collectors.toList());
    }
    return new ArrayList<String>();
  }

  private String join(String[] args, int start) {
    StringBuilder builder = new StringBuilder();
    for (int i = start; i < args.length; ++i) {
      if (i > start) {
        builder.append(' ');
      }
      builder.append(args[i]);
    }
    return builder.toString();
  }

  private String getClaimModesHelp() {
    return String.join(", ", KitClaimAnimationManager.getSupportedModeNames());
  }

  private boolean isSupportedClaimMode(String value) {
    return KitClaimAnimationManager.isSupportedMode(value);
  }

  private Boolean parseBooleanOrDefault(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    if (normalized.equals("default")) {
      return null;
    }
    if (normalized.equals("on")
        || normalized.equals("true")
        || normalized.equals("yes")
        || normalized.equals("tak")
        || normalized.equals("1")) {
      return true;
    }
    if (normalized.equals("off")
        || normalized.equals("false")
        || normalized.equals("no")
        || normalized.equals("nie")
        || normalized.equals("0")) {
      return false;
    }
    return null;
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

  private Double parseDouble(String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private String nextCopyId(String base) {
    String candidate = base + "_copy";
    int index = 2;
    while (this.plugin.getKitManager().getKit(candidate).isPresent()) {
      candidate = base + "_copy" + index;
      index++;
    }
    return candidate;
  }

  private String sanitizeId(String value) {
    String id = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    id = id.replace(' ', '_');
    return id.replaceAll("[^a-z0-9_\\-]", "");
  }
}
