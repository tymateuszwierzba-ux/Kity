package pl.tymek24.fortcodekits.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import pl.tymek24.fortcodekits.FortCodeKits;
import pl.tymek24.fortcodekits.gui.GuiSession;

public class PlayerJoinListener implements Listener {
  private final FortCodeKits plugin;

  public PlayerJoinListener(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    this.plugin.getCooldownManager().loadPlayer(event.getPlayer().getUniqueId());
    GuiSession.clear(event.getPlayer().getUniqueId());
  }
}
