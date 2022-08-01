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
  private static long startTime = 0;
  private static ReplayMeta meta;
  private static boolean recording = false;
  private static final LinkedHashSet<ReplayPacket> packets = new LinkedHashSet<>();
  private static UUID recorderUUID;
  private static final LinkedHashSet<PacketContainer> chunkPackets = new LinkedHashSet<>();
  private static final AtomicInteger ENTITY_IDS = new AtomicInteger(Integer.MAX_VALUE);

  public static void initalize() {
    startTime = 0;
    recording = false;
    packets.clear();
    chunkPackets.clear();
    UUID uuid = UUID.randomUUID();
    recorderUUID =
        new UUID(
            (uuid.getMostSignificantBits() & ~0xf000 | 0x2000), uuid.getLeastSignificantBits());
  }

  public static boolean isRecording() {
    return recording;
  }

  public static void startRecording(Match match) {
    startTime = System.currentTimeMillis();
    int entityId = ENTITY_IDS.decrementAndGet();

    addPacket(PacketCreator.createLoginSuccessPacket(recorderUUID));
    addPacket(PacketCreator.createLoginPacket(match, entityId));
    addPacket(PacketCreator.createPositionPacket(match.getWorld().getSpawnLocation()));
    addPacket(PacketCreator.createSpawnPositionPacket(match.getWorld().getSpawnLocation()));

    recording = true;

    chunkPackets.forEach((packet -> addPacket(packet)));

    addPacket(PacketCreator.createPlayerInfoPacket_AddPlayer(recorderUUID));
    addPacket(
        PacketCreator.createNamedEntitySpawnPacket(
            entityId, recorderUUID, match.getWorld().getSpawnLocation()));

    match
        .getParties()
        .forEach(party -> addPacket(PacketCreator.createScoreboardTeamPacket_Create(party)));
  }

  public static void stopRecording(Match match) {
    meta =
        new ReplayMeta(match.getMap().getName(), System.currentTimeMillis() - startTime, startTime);
    try {
      BukkitTask createFile =
          new ReplayWriter(meta.getFile(), meta, (LinkedHashSet<ReplayPacket>) packets.clone())
              .runTask(MatchRecorderPlugin.get());
    } catch (IOException e) {
      MatchRecorderPlugin.get().getLogger().log(Level.SEVERE, "Failed to write", e);
    }
    packets.clear();
    chunkPackets.clear();
    recording = false;
  }

  public static void createPlayer(MatchPlayer player, Location location) {
    addPacket(
        PacketCreator.createPlayerInfoPacket_AddPlayer(
            EnumWrappers.PlayerInfoAction.ADD_PLAYER, player.getBukkit()));
    addPacket(
        PacketCreator.createPlayerInfoPacket_UpdateGamemode(
            EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE, player.getBukkit()));
    addPacket(PacketCreator.createNamedEntitySpawnPacket(player.getBukkit(), location));
    addPacket(PacketCreator.createEntityMetadataPacket(player.getBukkit()));
    addPacket(PacketCreator.createUpdateAttributesPacket(player.getBukkit()));
    addPacket(PacketCreator.createEntityHeadRotationPacket(player.getBukkit()));
    addPacket(PacketCreator.createScoreboardTeamPacket_AddPlayer(player, player.getParty()));
  }

  public static void removePlayer(MatchPlayer player) {
    addPacket(PacketCreator.createEntityDestroyPacket(player.getBukkit()));
    addPacket(PacketCreator.createScoreboardTeamPacket_RemovePlayer(player, player.getParty()));
  }

  public static void addPacket(PacketContainer packet) {
    ReplayPacket pack = new ReplayPacket((int) (System.currentTimeMillis() - startTime), packet);
    packets.add(pack);
  }

  public static void addChunkPacket(PacketContainer packet) {
    chunkPackets.add(packet);
  }
}
