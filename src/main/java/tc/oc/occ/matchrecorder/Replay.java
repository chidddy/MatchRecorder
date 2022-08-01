package tc.oc.occ.matchrecorder;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

@SuppressWarnings({"unchecked", "unused"})
public class Replay {
  private long startTime = 0;
  private ReplayMeta meta;
  private boolean recording = false;
  private final LinkedHashSet<ReplayPacket> packets = new LinkedHashSet<>();
  private UUID recorderUUID;
  private final LinkedHashSet<PacketContainer> chunkPackets = new LinkedHashSet<>();
  private static final AtomicInteger ENTITY_IDS = new AtomicInteger(Integer.MAX_VALUE);

  public Replay() {
    this.startTime = 0;
    this.recording = false;
    this.packets.clear();
    this.chunkPackets.clear();
    UUID uuid = UUID.randomUUID();
    this.recorderUUID =
        new UUID(
            (uuid.getMostSignificantBits() & ~0xf000 | 0x2000), uuid.getLeastSignificantBits());
  }

  public boolean isRecording() {
    return this.recording;
  }

  public void startRecording(Match match) {
    this.startTime = System.currentTimeMillis();
    int entityId = ENTITY_IDS.decrementAndGet();

    addPacket(PacketBuilder.createLoginSuccessPacket(this.recorderUUID));
    addPacket(PacketBuilder.createLoginPacket(match, entityId));
    addPacket(PacketBuilder.createPositionPacket(match.getWorld().getSpawnLocation()));
    addPacket(PacketBuilder.createSpawnPositionPacket(match.getWorld().getSpawnLocation()));

    this.recording = true;

    this.chunkPackets.forEach((packet -> addPacket(packet)));

    addPacket(PacketBuilder.createPlayerInfoPacket_AddPlayer(recorderUUID));
    addPacket(
        PacketBuilder.createNamedEntitySpawnPacket(
            entityId, recorderUUID, match.getWorld().getSpawnLocation()));

    match
        .getParties()
        .forEach(party -> addPacket(PacketBuilder.createScoreboardTeamPacket_Create(party)));
  }

  public void stopRecording(Match match) {
    this.meta =
        new ReplayMeta(match.getMap().getName(), System.currentTimeMillis() - startTime, startTime);
    try {
      BukkitTask createFile =
          new ReplayWriter(meta.getFile(), meta, (LinkedHashSet<ReplayPacket>) packets.clone())
              .runTask(MatchRecorder.get());
    } catch (IOException e) {
      MatchRecorder.get().getLogger().log(Level.SEVERE, "Failed to write", e);
    }
    this.packets.clear();
    this.chunkPackets.clear();
    this.recording = false;
  }

  public void createPlayer(MatchPlayer player, Location location) {
    addPacket(
        PacketBuilder.createPlayerInfoPacket_AddPlayer(
            EnumWrappers.PlayerInfoAction.ADD_PLAYER, player.getBukkit()));
    addPacket(
        PacketBuilder.createPlayerInfoPacket_UpdateGamemode(
            EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE, player.getBukkit()));
    addPacket(PacketBuilder.createNamedEntitySpawnPacket(player.getBukkit(), location));
    addPacket(PacketBuilder.createEntityMetadataPacket(player.getBukkit()));
    addPacket(PacketBuilder.createUpdateAttributesPacket(player.getBukkit()));
    addPacket(PacketBuilder.createEntityHeadRotationPacket(player.getBukkit()));
    addPacket(PacketBuilder.createScoreboardTeamPacket_AddPlayer(player, player.getParty()));
  }

  public void removePlayer(MatchPlayer player) {
    addPacket(PacketBuilder.createEntityDestroyPacket(player.getBukkit()));
    addPacket(PacketBuilder.createScoreboardTeamPacket_RemovePlayer(player, player.getParty()));
  }

  public void addPacket(PacketContainer packet) {
    ReplayPacket pack = new ReplayPacket((int) (System.currentTimeMillis() - startTime), packet);
    this.packets.add(pack);
  }

  public void addChunkPacket(PacketContainer packet) {
    this.chunkPackets.add(packet);
  }
}
