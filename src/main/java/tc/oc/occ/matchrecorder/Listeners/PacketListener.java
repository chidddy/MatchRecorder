package tc.oc.occ.matchrecorder.Listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import tc.oc.occ.matchrecorder.MatchRecorder;
import tc.oc.occ.matchrecorder.Recorder;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.player.MatchPlayer;

@SuppressWarnings("deprecation")
public class PacketListener extends PacketAdapter implements Listener {
  private final Recorder recorder;

  public PacketListener(MatchRecorder plugin) {
    super(plugin, ListenerPriority.MONITOR, getPacketTypes());
    this.recorder = plugin.getRecorder();
    ProtocolLibrary.getProtocolManager().addPacketListener(this);
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    PacketContainer packet = event.getPacket();
    if (!recorder.isRecording()) {
      if (packet.getType() == PacketType.Play.Server.MAP_CHUNK
          || packet.getType() == PacketType.Play.Server.MAP_CHUNK_BULK) {
        recorder.addChunkPacket(packet);
      }
      return;
    }
    // ! look at thread safety of this, potentially find better method
    MatchPlayer player = PGM.get().getMatchManager().getPlayer(event.getPlayer());
    if (player == null) return;
    if (player.isObserving()) return;

    if (packet.getType() == PacketType.Play.Server.NAMED_ENTITY_SPAWN) return;
    if (packet.getType() == PacketType.Play.Server.MAP_CHUNK) return;
    if (packet.getType() == PacketType.Play.Server.MAP_CHUNK_BULK) return;
    if (packet.getType() == PacketType.Play.Server.SCOREBOARD_TEAM
        || packet.getType() == PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE
        || packet.getType() == PacketType.Play.Server.SCOREBOARD_SCORE
        || packet.getType() == PacketType.Play.Server.SCOREBOARD_OBJECTIVE) {
      return;
    }
    // ? add teleport to this list?
    if (packet.getType() == PacketType.Play.Server.REL_ENTITY_MOVE
        || packet.getType() == PacketType.Play.Server.REL_ENTITY_MOVE_LOOK
        || packet.getType() == PacketType.Play.Server.ENTITY_LOOK
        || packet.getType() == PacketType.Play.Server.ENTITY_HEAD_ROTATION
        || packet.getType() == PacketType.Play.Server.ENTITY_VELOCITY) {
      Entity ent = packet.getEntityModifier(event.getPlayer().getWorld()).read(0);
      if (ent != null) {
        if (ent.getType() == EntityType.PLAYER) {
          return;
        }
      }
    } else if (packet.getType() == PacketType.Play.Server.ENTITY_DESTROY) {
      Entity ent =
          ProtocolLibrary.getProtocolManager()
              .getEntityFromID(event.getPlayer().getWorld(), packet.getIntegerArrays().read(0)[0]);
      if (ent != null) {
        if (ent.getType() == EntityType.PLAYER) {
          return;
        }
      }
    }
    recorder.addPacket(packet);
  }

  static Set<PacketType> getPacketTypes() {
    Collection<PacketType> types =
        new HashSet<>(
            Arrays.asList(
                // PacketType.Play.Server.CHAT,
                PacketType.Play.Server.RESPAWN,
                // PacketType.Play.Server.POSITION,
                PacketType.Play.Server.ANIMATION,
                // PacketType.Play.Server.PLAYER_INFO,
                PacketType.Play.Server.REL_ENTITY_MOVE,
                PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
                PacketType.Play.Server.ENTITY_LOOK,
                PacketType.Play.Server.MAP_CHUNK,
                PacketType.Play.Server.MAP_CHUNK_BULK,
                PacketType.Play.Server.WORLD_BORDER,
                PacketType.Play.Server.WORLD_PARTICLES,
                PacketType.Play.Server.WORLD_EVENT,
                PacketType.Play.Server.NAMED_SOUND_EFFECT,
                PacketType.Play.Server.UPDATE_TIME,
                PacketType.Play.Server.COLLECT,
                PacketType.Play.Server.NAMED_ENTITY_SPAWN,
                PacketType.Play.Server.SPAWN_ENTITY,
                PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB,
                PacketType.Play.Server.SPAWN_ENTITY_LIVING,
                PacketType.Play.Server.SPAWN_ENTITY_WEATHER,
                PacketType.Play.Server.SPAWN_ENTITY_PAINTING,
                PacketType.Play.Server.ENTITY,
                PacketType.Play.Server.ENTITY_VELOCITY,
                PacketType.Play.Server.ENTITY_TELEPORT,
                PacketType.Play.Server.ENTITY_STATUS,
                PacketType.Play.Server.ATTACH_ENTITY,
                PacketType.Play.Server.ENTITY_EFFECT,
                PacketType.Play.Server.REMOVE_ENTITY_EFFECT,
                PacketType.Play.Server.ENTITY_EQUIPMENT,
                PacketType.Play.Server.EXPLOSION,
                PacketType.Play.Server.ENTITY_METADATA,
                PacketType.Play.Server.ENTITY_DESTROY,
                PacketType.Play.Server.MULTI_BLOCK_CHANGE,
                PacketType.Play.Server.BLOCK_CHANGE,
                PacketType.Play.Server.BLOCK_ACTION,
                PacketType.Play.Server.UPDATE_SIGN,
                PacketType.Play.Server.BLOCK_BREAK_ANIMATION,
                // ! FINISH MANUALLY ADDING THESE
                PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE,
                PacketType.Play.Server.SCOREBOARD_OBJECTIVE,
                PacketType.Play.Server.SCOREBOARD_SCORE,
                PacketType.Play.Server.SCOREBOARD_TEAM,
                PacketType.Play.Server.GAME_STATE_CHANGE));
    return types.stream().collect(Collectors.toSet());
  }
}
