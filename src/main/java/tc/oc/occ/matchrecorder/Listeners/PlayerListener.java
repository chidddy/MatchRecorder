package tc.oc.occ.matchrecorder.Listeners;

import java.util.Locale;
import java.util.logging.Level;
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
import tc.oc.occ.matchrecorder.MatchRecorder;
import tc.oc.occ.matchrecorder.PacketBuilder;
import tc.oc.occ.matchrecorder.Recorder;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.api.player.event.MatchPlayerDeathEvent;
import tc.oc.pgm.death.DeathMessageBuilder;
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
    recorder.createPlayer(event.getPlayer(), event.getLocation());
  }

  // TODO:
  // ? [ ] remove players on /obs?
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerLeave(ParticipantDespawnEvent event) {
    if (!event.getMatch().isRunning()) return;
    if (!event.getPlayer().isParticipating()) return;
    recorder.removePlayer(event.getPlayer());
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
      recorder.addPacket(
          PacketBuilder.createEntityHeadRotationPacket(event.getPlayer(), event.getTo().getYaw()));
    }
    Vector move = event.getTo().clone().subtract(event.getFrom()).toVector();
    if (isDifferent(event.getFrom(), event.getTo())) {
      // * position and look packet
      recorder.addPacket(
          PacketBuilder.createRelativeEntityMoveLookPacket(
              event.getPlayer(), move, event.getTo().getYaw(), event.getTo().getPitch()));

    } else if (hasRotated(event.getFrom(), event.getTo())) {
      // * look packet
      recorder.addPacket(
          PacketBuilder.createEntityLookPacket(
              event.getPlayer(), event.getTo().getYaw(), event.getTo().getPitch()));
    } else {
      // * position packet
      recorder.addPacket(PacketBuilder.createRelativeEntityMovePacket(event.getPlayer(), move));
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerVelocity(PlayerVelocityEvent event) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving()) return;
    recorder.addPacket(
        PacketBuilder.createEntityVelocityPacket(event.getPlayer(), event.getVelocity()));
  }

  // TODO:
  // ! [x] make entity_equipment event for slot 0
  // ! [x] fix inconsistency with getItemInHand(), find a new method to get more updated information
  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerHeldChange(PlayerItemHeldEvent event) {
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving()) return;
    // held item
    recorder.addPacket(
        PacketBuilder.createEntityEquipmentPacket(
            event.getPlayer(), 0, event.getPlayer().getInventory().getItem(event.getNewSlot())));
    // helmet
    recorder.addPacket(
        PacketBuilder.createEntityEquipmentPacket(
            event.getPlayer(), 4, event.getPlayer().getEquipment().getHelmet()));
    // chestpiece
    recorder.addPacket(
        PacketBuilder.createEntityEquipmentPacket(
            event.getPlayer(), 3, event.getPlayer().getEquipment().getChestplate()));
    // legs
    recorder.addPacket(
        PacketBuilder.createEntityEquipmentPacket(
            event.getPlayer(), 2, event.getPlayer().getEquipment().getHelmet()));
    // boots
    recorder.addPacket(
        PacketBuilder.createEntityEquipmentPacket(
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
    DeathMessageBuilder builder = new DeathMessageBuilder(event, MatchRecorder.get().getLogger());
    Component message =
        TextTranslations.translate(builder.getMessage().color(NamedTextColor.GRAY), Locale.ENGLISH);
    MatchRecorder.get()
        .getLogger()
        .log(Level.INFO, LegacyComponentSerializer.legacySection().serialize(message));
    recorder.addPacket(PacketBuilder.createChatPacket(message));
  }
}
