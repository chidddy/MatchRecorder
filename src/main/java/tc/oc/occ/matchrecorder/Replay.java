package tc.oc.occ.matchrecorder;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.goals.Goal;

@SuppressWarnings({"unchecked"})
public class Replay {
  private long startTime = 0;
  private ReplayMeta meta;
  private boolean recording = false;
  private final LinkedHashSet<ReplayPacket> packets = new LinkedHashSet<>();
  private UUID recorderUUID;
  private final LinkedHashSet<PacketContainer> chunkPackets = new LinkedHashSet<>();
  private static final AtomicInteger ENTITY_IDS = new AtomicInteger(Integer.MAX_VALUE);
  private SideBarGenerator sidebar = null;

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
    this.sidebar = new SideBarGenerator(this, match);
    addPacket(PacketBuilder.createLoginSuccessPacket(this.recorderUUID));
    addPacket(PacketBuilder.createLoginPacket(match, entityId));
    addPacket(PacketBuilder.createPositionPacket(match.getWorld().getSpawnLocation()));
    addPacket(PacketBuilder.createSpawnPositionPacket(match.getWorld().getSpawnLocation()));

    this.recording = true;

    this.chunkPackets.forEach((packet -> addPacket(packet)));

    addPacket(PacketBuilder.createPlayerInfoPacket_AddPlayer(recorderUUID));

    match
        .getParties()
        .forEach(party -> addPacket(PacketBuilder.createScoreboardTeamPacket_Create(party)));

    this.sidebar.createSidebar();
    this.sidebar.displayUpdatedSidebar(this.sidebar.constructSidebar());
  }

  public void stopRecording(Match match) {
    this.meta =
        new ReplayMeta(match.getMap().getName(), System.currentTimeMillis() - startTime, startTime);
    try {
      new ReplayWriter(meta.getFile(), meta, (LinkedHashSet<ReplayPacket>) packets.clone())
          .runTask(MatchRecorder.get());
    } catch (Exception e) {
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

  public void removePlayer(MatchPlayer player, boolean remove) {
    addPacket(PacketBuilder.createEntityDestroyPacket(player.getBukkit()));
    if (remove)
      addPacket(PacketBuilder.createScoreboardTeamPacket_RemovePlayer(player, player.getParty()));
  }

  public void updatePlayerItems(Player player) {
    // held item
    addPacket(
        PacketBuilder.createEntityEquipmentPacket(
            player, 0, player.getInventory().getItem(player.getInventory().getHeldItemSlot())));
    // helmet
    addPacket(
        PacketBuilder.createEntityEquipmentPacket(player, 4, player.getEquipment().getHelmet()));
    // chestpiece
    addPacket(
        PacketBuilder.createEntityEquipmentPacket(
            player, 3, player.getEquipment().getChestplate()));
    // legs
    addPacket(
        PacketBuilder.createEntityEquipmentPacket(player, 2, player.getEquipment().getHelmet()));
    // boots
    addPacket(
        PacketBuilder.createEntityEquipmentPacket(player, 1, player.getEquipment().getBoots()));
  }

  public void updatePlayerItems(Player player, int slot) {
    // held item
    addPacket(
        PacketBuilder.createEntityEquipmentPacket(player, 0, player.getInventory().getItem(slot)));
    // helmet
    addPacket(
        PacketBuilder.createEntityEquipmentPacket(player, 4, player.getEquipment().getHelmet()));
    // chestpiece
    addPacket(
        PacketBuilder.createEntityEquipmentPacket(
            player, 3, player.getEquipment().getChestplate()));
    // legs
    addPacket(
        PacketBuilder.createEntityEquipmentPacket(player, 2, player.getEquipment().getHelmet()));
    // boots
    addPacket(
        PacketBuilder.createEntityEquipmentPacket(player, 1, player.getEquipment().getBoots()));
  }

  public void addPacket(PacketContainer packet) {
    ReplayPacket pack = new ReplayPacket((int) (System.currentTimeMillis() - startTime), packet);
    this.packets.add(pack);
  }

  public void addChunkPacket(PacketContainer packet) {
    this.chunkPackets.add(packet);
  }

  public void clearChunks() {
    this.chunkPackets.clear();
  }

  public void killPlayer(MatchPlayer player) {
    addPacket(PacketBuilder.createEntityMetadataPacket_Dead(player.getBukkit()));
    Bukkit.getScheduler()
        .runTaskLater(
            MatchRecorder.get(),
            () -> {
              removePlayer(player, false);
            },
            15);
  }

  public void updateSidebar() {
    this.sidebar.renderSidebarDebounce();
  }

  public void blinkGoal(Goal goal, float rateHz, @Nullable Duration duration) {
    this.sidebar.blinkGoal(goal, rateHz, duration);
  }

  public void stopBlinkingGoal(Goal goal) {
    this.sidebar.stopBlinkingGoal(goal);
  }
}
