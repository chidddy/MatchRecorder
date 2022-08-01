package tc.oc.occ.matchrecorder.Listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;
import tc.oc.occ.matchrecorder.MatchRecorderPlugin;
import tc.oc.occ.matchrecorder.PacketCreator;
import tc.oc.occ.matchrecorder.Replay;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.death.DeathMessageBuilder;
import tc.oc.pgm.spawns.events.ParticipantDespawnEvent;
import tc.oc.pgm.spawns.events.ParticipantSpawnEvent;

public class PlayerListener implements Listener {

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(ParticipantSpawnEvent event) {
    Replay.createPlayer(event.getPlayer(), event.getLocation());
  }

  // TODO:
  // ? [ ] remove players on /obs?
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerLeave(ParticipantDespawnEvent event) {
    if (!event.getMatch().isRunning()) return;
    if (!event.getPlayer().isParticipating()) return;
    Replay.removePlayer(event.getPlayer());
  }

  // TODO:
  // ! [x] filter only participants
  // ! [x] filter between move, movelook, velocity etc packets
  // ! [x] construct and add packets
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(PlayerMoveEvent event) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving()) return;
    if (event.getFrom().getYaw() != event.getTo().getYaw()) {
      // * head rotation
      Replay.addPacket(
          PacketCreator.createEntityHeadRotationPacket(event.getPlayer(), event.getTo().getYaw()));
    }
    Vector move = event.getTo().clone().subtract(event.getFrom()).toVector();
    if (isDifferent(event.getFrom(), event.getTo())) {
      // * position and look packet
      Replay.addPacket(
          PacketCreator.createRelativeEntityMoveLookPacket(
              event.getPlayer(), move, event.getTo().getYaw(), event.getTo().getPitch()));

    } else if (hasRotated(event.getFrom(), event.getTo())) {
      // * look packet
      Replay.addPacket(
          PacketCreator.createEntityLookPacket(
              event.getPlayer(), event.getTo().getYaw(), event.getTo().getPitch()));
    } else {
      // * position packet
      Replay.addPacket(PacketCreator.createRelativeEntityMovePacket(event.getPlayer(), move));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerVelocity(PlayerVelocityEvent event) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving()) return;
    Replay.addPacket(
        PacketCreator.createEntityVelocityPacket(event.getPlayer(), event.getVelocity()));
  }

  // TODO:
  // ! [x] make entity_equipment event for slot 0
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerHeldChange(PlayerItemHeldEvent event) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving()) return;
    // held item
    Replay.addPacket(
        PacketCreator.createEntityEquipmentPacket(
            event.getPlayer(), 0, event.getPlayer().getItemInHand()));
    // helmet
    Replay.addPacket(
        PacketCreator.createEntityEquipmentPacket(
            event.getPlayer(), 4, event.getPlayer().getEquipment().getHelmet()));
    // chestpiece
    Replay.addPacket(
        PacketCreator.createEntityEquipmentPacket(
            event.getPlayer(), 3, event.getPlayer().getEquipment().getChestplate()));
    // legs
    Replay.addPacket(
        PacketCreator.createEntityEquipmentPacket(
            event.getPlayer(), 2, event.getPlayer().getEquipment().getHelmet()));
    // boots
    Replay.addPacket(
        PacketCreator.createEntityEquipmentPacket(
            event.getPlayer(), 1, event.getPlayer().getEquipment().getBoots()));
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
    DeathMessageBuilder builder =
        new DeathMessageBuilder(event, MatchRecorderPlugin.get().getLogger());
    Component message = builder.getMessage().color(NamedTextColor.GRAY);
    Replay.addPacket(
        PacketCreator.createChatPacket(
            LegacyComponentSerializer.legacySection().serialize(message)));
  }
}
