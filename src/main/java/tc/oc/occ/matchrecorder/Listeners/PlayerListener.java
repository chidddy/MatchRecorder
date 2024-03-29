package tc.oc.occ.matchrecorder.Listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;
import tc.oc.occ.matchrecorder.MatchRecorder;
import tc.oc.occ.matchrecorder.PacketBuilder;
import tc.oc.occ.matchrecorder.Recorder;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.death.DeathMessageBuilder;
import tc.oc.pgm.kits.ApplyKitEvent;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;
import tc.oc.pgm.util.text.TextTranslations;

public class PlayerListener implements Listener {
  private final Recorder recorder;

  public PlayerListener(Recorder recorder) {
    this.recorder = recorder;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(ParticipantSpawnEvent event) {
    if (!recorder.isRecording()) return;
    recorder.createPlayer(event.getPlayer(), event.getLocation());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerLeave(ParticipantDespawnEvent event) {
    if (!event.getMatch().isRunning() || !recorder.isRecording()) return;
    if (event.getPlayer() != null && event.getPlayer().isDead()) {
      recorder.killPlayer(event.getPlayer());
    } else {
      recorder.removePlayer(event.getPlayer(), true);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerAnimation(PlayerAnimationEvent event) {
    if (!recorder.isRecording()) return;
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving() || player.isDead()) return;
    recorder.addPacket(
        PacketBuilder.createAnimationPacket(player.getBukkit(), event.getAnimationType()));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!recorder.isRecording()) return;
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving() || player.isDead()) return;
    if (event.getFrom().getYaw() != event.getTo().getYaw()) {
      // * head rotation
      recorder.addPacket(
          PacketBuilder.createEntityHeadRotationPacket(event.getPlayer(), event.getTo().getYaw()));
    }
    if (isDifferent(event.getFrom(), event.getTo())) {
      // * position and look packet
      recorder.addPacket(
          PacketBuilder.createRelativeEntityMoveLookPacket(
              event.getPlayer(),
              event.getFrom(),
              event.getTo(),
              event.getTo().getYaw(),
              event.getTo().getPitch()));

    } else if (hasRotated(event.getFrom(), event.getTo())) {
      // * look packet
      recorder.addPacket(
          PacketBuilder.createEntityLookPacket(
              event.getPlayer(), event.getTo().getYaw(), event.getTo().getPitch()));
    } else {
      // * position packet
      recorder.addPacket(
          PacketBuilder.createRelativeEntityMovePacket(
              event.getPlayer(), event.getFrom(), event.getTo()));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerTeleport(PlayerTeleportEvent event) {
    if (!recorder.isRecording()) return;
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving()) return;
    if (event.getCause() == TeleportCause.PLUGIN && player.isDead()) return;
    recorder.addPacket(PacketBuilder.createEntityTeleportPacket(event));
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerRide(EntityMountEvent event) {
    if (!recorder.isRecording()) return;
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getEntity());
    if (player == null) return;
    if (player.isObserving() || player.isDead()) return;
    recorder.addPacket(PacketBuilder.createAttachEntityPacket(event.getEntity(), event.getMount()));
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onPlayerDismount(EntityDismountEvent event) {
    if (!recorder.isRecording()) return;
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getEntity());
    if (player == null) return;
    if (player.isObserving() || player.isDead()) return;
    recorder.addPacket(PacketBuilder.createAttachEntityPacket_Dismount(event.getEntity()));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerHeldChange(PlayerItemHeldEvent event) {
    if (!recorder.isRecording()) return;
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving() || player.isDead()) return;
    recorder.updatePlayerItems(event.getPlayer(), event.getNewSlot());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerPickup(PlayerPickupItemEvent event) {
    if (!recorder.isRecording()) return;
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving()) return;
    recorder.addPacket(PacketBuilder.createCollectPacket(event.getPlayer(), event.getItem()));
  }

  private boolean isDifferent(Location one, Location two) {
    return (hasMoved(one, two) && hasRotated(one, two));
  }

  private boolean hasMoved(Location one, Location two) {
    return (one.getX() != two.getX() || one.getY() != two.getY() || one.getZ() != two.getZ());
  }

  private boolean hasRotated(Location one, Location two) {
    return (one.getYaw() != two.getYaw() || one.getPitch() != two.getPitch());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerDeath(MatchPlayerDeathEvent event) {
    if (!recorder.isRecording()) return;
    DeathMessageBuilder builder = new DeathMessageBuilder(event, MatchRecorder.get().getLogger());
    Component message = TextTranslations.translate(builder.getMessage().color(NamedTextColor.GRAY));
    recorder.addPacket(PacketBuilder.createChatPacket(message));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onKitApply(ApplyKitEvent event) {
    if (!recorder.isRecording()) return;
    if (event.getPlayer().isDead()) return;
    if (event.getPlayer().isObserving()) return;
    Bukkit.getScheduler()
        .runTaskLater(
            MatchRecorder.get(),
            () -> {
              recorder.updatePlayerItems(event.getPlayer().getBukkit());
            },
            3);
  }
}
