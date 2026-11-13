package pl.tymek24.fortcodekits.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.tymek24.fortcodekits.FortCodeKits;

public class PlayerQuitListener implements Listener {
  private final FortCodeKits plugin;

  public PlayerQuitListener(FortCodeKits plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    this.plugin.clearClaimFeedbackVisual(event.getPlayer());
    if (this.plugin.getKitClaimAnimationManager() != null) {
      this.plugin.getKitClaimAnimationManager().clearPlayer(event.getPlayer());
    }
    this.plugin.getCooldownManager().unloadPlayer(event.getPlayer().getUniqueId());
    this.plugin.getChatInputListener().clear(event.getPlayer().getUniqueId());
  }
}
