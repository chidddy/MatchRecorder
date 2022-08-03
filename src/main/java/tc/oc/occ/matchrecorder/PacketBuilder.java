package tc.oc.occ.matchrecorder;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.EnumWrappers.ChatType;
import com.comphenix.protocol.wrappers.EnumWrappers.ScoreboardAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.minecraft.server.v1_8_R3.MathHelper;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunkBulk;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.party.Party;
import tc.oc.pgm.api.player.MatchPlayer;

@SuppressWarnings("deprecation")
public class PacketBuilder {

  public static final HashMap<PacketType, Integer> packetTypes = new HashMap<PacketType, Integer>();

  static {
    packetTypes.put(PacketType.Play.Server.LOGIN, 0x01);
    packetTypes.put(PacketType.Play.Server.CHAT, 0x02);
    packetTypes.put(PacketType.Play.Server.UPDATE_TIME, 0x03);
    packetTypes.put(PacketType.Play.Server.ENTITY_EQUIPMENT, 0x04);
    packetTypes.put(PacketType.Play.Server.SPAWN_POSITION, 0x05);
    packetTypes.put(PacketType.Play.Server.RESPAWN, 0x07);
    packetTypes.put(PacketType.Play.Server.POSITION, 0x8);
    packetTypes.put(PacketType.Play.Server.BED, 0x0A);
    packetTypes.put(PacketType.Play.Server.ANIMATION, 0x0B);
    packetTypes.put(PacketType.Play.Server.NAMED_ENTITY_SPAWN, 0x0C);
    packetTypes.put(PacketType.Play.Server.COLLECT, 0x0D);
    packetTypes.put(PacketType.Play.Server.SPAWN_ENTITY, 0x0E);
    packetTypes.put(PacketType.Play.Server.SPAWN_ENTITY_LIVING, 0x0F);
    packetTypes.put(PacketType.Play.Server.SPAWN_ENTITY_PAINTING, 0x10);
    packetTypes.put(PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB, 0x11);
    packetTypes.put(PacketType.Play.Server.ENTITY_VELOCITY, 0x12);
    packetTypes.put(PacketType.Play.Server.ENTITY_DESTROY, 0x13);
    packetTypes.put(PacketType.Play.Server.ENTITY, 0x14);
    packetTypes.put(PacketType.Play.Server.REL_ENTITY_MOVE, 0x15);
    packetTypes.put(PacketType.Play.Server.ENTITY_LOOK, 0x16);
    packetTypes.put(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, 0x17);
    packetTypes.put(PacketType.Play.Server.ENTITY_TELEPORT, 0x18);
    packetTypes.put(PacketType.Play.Server.ENTITY_HEAD_ROTATION, 0x19);
    packetTypes.put(PacketType.Play.Server.ENTITY_STATUS, 0x1A);
    packetTypes.put(PacketType.Play.Server.ATTACH_ENTITY, 0x1B);
    packetTypes.put(PacketType.Play.Server.ENTITY_METADATA, 0x1C);
    packetTypes.put(PacketType.Play.Server.ENTITY_EFFECT, 0x1D);
    packetTypes.put(PacketType.Play.Server.REMOVE_ENTITY_EFFECT, 0x1E);
    packetTypes.put(PacketType.Play.Server.EXPERIENCE, 0x1F);
    packetTypes.put(PacketType.Play.Server.UPDATE_ATTRIBUTES, 0x20);
    packetTypes.put(PacketType.Play.Server.MAP_CHUNK, 0x21);
    packetTypes.put(PacketType.Play.Server.MULTI_BLOCK_CHANGE, 0x22);
    packetTypes.put(PacketType.Play.Server.BLOCK_CHANGE, 0x23);
    packetTypes.put(PacketType.Play.Server.BLOCK_ACTION, 0x24);
    packetTypes.put(PacketType.Play.Server.BLOCK_BREAK_ANIMATION, 0x25);
    packetTypes.put(PacketType.Play.Server.MAP_CHUNK_BULK, 0x26);
    packetTypes.put(PacketType.Play.Server.EXPLOSION, 0x27);
    packetTypes.put(PacketType.Play.Server.WORLD_EVENT, 0x28);
    packetTypes.put(PacketType.Play.Server.NAMED_SOUND_EFFECT, 0x29);
    packetTypes.put(PacketType.Play.Server.WORLD_PARTICLES, 0x2A);
    packetTypes.put(PacketType.Play.Server.GAME_STATE_CHANGE, 0x2B);
    packetTypes.put(PacketType.Play.Server.SPAWN_ENTITY_WEATHER, 0x2C);
    packetTypes.put(PacketType.Play.Server.UPDATE_SIGN, 0x33);
    packetTypes.put(PacketType.Play.Server.PLAYER_INFO, 0x38);
    packetTypes.put(PacketType.Play.Server.SCOREBOARD_OBJECTIVE, 0x3B);
    packetTypes.put(PacketType.Play.Server.SCOREBOARD_SCORE, 0x3C);
    packetTypes.put(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE, 0x3D);
    packetTypes.put(PacketType.Play.Server.SCOREBOARD_TEAM, 0x3E);
    packetTypes.put(PacketType.Play.Server.VIEW_CENTRE, 0x41);
    packetTypes.put(PacketType.Play.Server.CAMERA, 0x43);
    packetTypes.put(PacketType.Play.Server.WORLD_BORDER, 0x44);
    packetTypes.put(PacketType.Play.Server.PLAYER_LIST_HEADER_FOOTER, 0x47);
    packetTypes.put(PacketType.Play.Server.UPDATE_ENTITY_NBT, 0x49);
  }

  public static int getPacketID(PacketType type) {
    if (type.getProtocol().equals(PacketType.Protocol.PLAY)) {
      return packetTypes.getOrDefault(type, type.getCurrentId());
    }
    return type.getCurrentId();
  }

  public static PacketContainer createLoginSuccessPacket(UUID uuid) {
    PacketContainer packet = new PacketContainer(PacketType.Login.Server.SUCCESS);
    packet.getGameProfiles().write(0, new WrappedGameProfile(uuid, "MatchRecorder"));
    return packet;
  }

  public static PacketContainer createLoginPacket(Match match, int entityId) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.LOGIN);
    packet.getIntegers().write(0, entityId);
    packet.getGameModes().write(0, EnumWrappers.NativeGameMode.SURVIVAL);
    packet.getIntegers().write(1, match.getWorld().getEnvironment().ordinal());
    packet
        .getDifficulties()
        .write(0, EnumWrappers.Difficulty.valueOf(match.getWorld().getDifficulty().name()));
    packet.getIntegers().write(2, Bukkit.getMaxPlayers());
    packet.getWorldTypeModifier().write(0, match.getWorld().getWorldType());
    packet.getBooleans().write(0, false);
    return packet;
  }

  public static PacketContainer createChatPacket(String message) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.CHAT);
    packet.getChatComponents().write(0, WrappedChatComponent.fromLegacyText(message));
    packet.getChatTypes().write(0, ChatType.SYSTEM);
    return packet;
  }

  public static PacketContainer createChatPacket(Component message) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.CHAT);
    packet.getChatComponents().write(0, AdventureComponentConverter.fromComponent(message));
    packet.getChatTypes().write(0, ChatType.SYSTEM);
    return packet;
  }

  public static PacketContainer createUpdateTimePacket() {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.UPDATE_TIME);
    return packet;
  }

  public static PacketContainer createEntityEquipmentPacket(
      Entity entity, int slot, ItemStack item) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
    packet.getIntegers().write(0, entity.getEntityId());
    packet.getIntegers().write(1, slot);
    packet.getItemModifier().write(0, item);
    return packet;
  }

  public static PacketContainer createSpawnPositionPacket(Location location) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SPAWN_POSITION);
    packet
        .getBlockPositionModifier()
        .write(
            0, new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    return packet;
  }

  public static PacketContainer createPositionPacket(Location location) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.POSITION);
    packet
        .getDoubles()
        .write(0, location.getX())
        .write(1, location.getY())
        .write(2, location.getZ());
    packet.getFloat().write(0, location.getYaw()).write(1, location.getPitch());
    packet.getWatchableCollectionModifier().writeDefaults();
    return packet;
  }

  public static PacketContainer createAnimationPacket(Player player, PlayerAnimationType anim) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);
    packet.getIntegers().write(0, player.getEntityId());
    packet.getIntegers().write(1, anim.ordinal());
    return packet;
  }

  public static PacketContainer createNamedEntitySpawnPacket(MatchPlayer entity) {
    return createNamedEntitySpawnPacket(entity.getBukkit(), entity.getLocation());
  }

  public static PacketContainer createNamedEntitySpawnPacket(
      int entityID, UUID uuid, Location location) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
    packet.getIntegers().write(0, entityID);
    packet.getUUIDs().write(0, uuid);
    packet
        .getIntegers()
        .write(0, toFixedPoint(location.getX()))
        .write(1, toFixedPoint(location.getY()))
        .write(2, toFixedPoint(location.getZ()));
    packet.getBytes().write(0, toAngle(0)).write(1, toAngle(0));
    packet.getIntegers().write(4, 0);
    WrappedDataWatcher wdw = new WrappedDataWatcher();
    wdw.setObject(0, (byte) 0);
    wdw.setObject(1, 300);
    wdw.setObject(2, "");
    wdw.setObject(3, (byte) 0);
    wdw.setObject(4, (byte) 0);
    wdw.setObject(6, (float) 20);
    wdw.setObject(7, 0);
    wdw.setObject(8, (byte) 0);
    wdw.setObject(9, (byte) 0);
    wdw.setObject(10, (byte) 0);
    wdw.setObject(17, (float) 0);
    wdw.setObject(18, 0);
    packet.getDataWatcherModifier().write(0, wdw);
    packet.getIntegers().write(0, entityID);
    return packet;
  }

  public static PacketContainer createNamedEntitySpawnPacket(Player entity, Location location) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
    packet.getIntegers().write(0, entity.getEntityId());
    packet.getUUIDs().write(0, entity.getUniqueId());
    packet
        .getIntegers()
        .write(1, toFixedPoint(location.getX()))
        .write(2, toFixedPoint(location.getY()))
        .write(3, toFixedPoint(location.getZ()));
    packet
        .getBytes()
        .write(0, toAngle(entity.getLocation().getYaw()))
        .write(1, toAngle(entity.getLocation().getPitch()));
    packet.getDataWatcherModifier().write(0, WrappedDataWatcher.getEntityWatcher(entity));
    packet.getIntegers().write(0, entity.getEntityId());
    packet.getUUIDs().write(0, entity.getUniqueId());
    return packet;
  }

  public static PacketContainer createSpawnEntityExperienceOrb() {
    PacketContainer packet =
        new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB);
    return packet;
  }

  public static PacketContainer createEntityVelocityPacket(Entity entity, Vector velocity) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_VELOCITY);
    packet
        .getIntegers()
        .write(0, entity.getEntityId())
        .write(1, toVelocity(velocity.getX()))
        .write(2, toVelocity(velocity.getY()))
        .write(3, toVelocity(velocity.getZ()));
    return packet;
  }

  public static PacketContainer createEntityDestroyPacket(Entity entity) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
    packet.getIntegerArrays().write(0, new int[] {entity.getEntityId()});
    return packet;
  }

  public static PacketContainer createRelativeEntityMovePacket(
      Entity entity, Location from, Location to) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.REL_ENTITY_MOVE);
    packet.getIntegers().write(0, entity.getEntityId());
    packet
        .getBytes()
        .write(0, (byte) (toFixedPoint(to.getX()) - toFixedPoint(from.getX())))
        .write(1, (byte) (toFixedPoint(to.getY()) - toFixedPoint(from.getY())))
        .write(2, (byte) (toFixedPoint(to.getZ()) - toFixedPoint(from.getZ())));
    packet.getBooleans().write(0, entity.isOnGround());
    return packet;
  }

  public static PacketContainer createEntityLookPacket(Entity entity, float yaw, float pitch) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_LOOK);
    packet.getIntegers().write(0, entity.getEntityId());
    packet.getBytes().write(0, toAngle(yaw)).write(1, toAngle(pitch));
    packet.getBooleans().write(0, entity.isOnGround());
    return packet;
  }

  public static PacketContainer createRelativeEntityMoveLookPacket(
      Entity entity, Location from, Location to, float yaw, float pitch) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.REL_ENTITY_MOVE_LOOK);
    packet.getIntegers().write(0, entity.getEntityId());
    packet
        .getBytes()
        .write(0, (byte) (toFixedPoint(to.getX()) - toFixedPoint(from.getX())))
        .write(1, (byte) (toFixedPoint(to.getY()) - toFixedPoint(from.getY())))
        .write(2, (byte) (toFixedPoint(to.getZ()) - toFixedPoint(from.getZ())))
        .write(3, toAngle(yaw))
        .write(4, toAngle(pitch));
    packet.getBooleans().write(0, entity.isOnGround());
    return packet;
  }

  public static PacketContainer createEntityTeleportPacket(EntityTeleportEvent event) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
    packet
        .getIntegers()
        .write(0, event.getEntity().getEntityId())
        .write(1, toFixedPoint(event.getTo().getX()))
        .write(2, toFixedPoint(event.getTo().getY()))
        .write(3, toFixedPoint(event.getTo().getZ()));
    packet
        .getBytes()
        .write(0, toAngle(event.getTo().getYaw()))
        .write(1, toAngle(event.getTo().getPitch()));
    packet.getBooleans().write(0, event.getEntity().isOnGround());
    return packet;
  }

  public static PacketContainer createEntityTeleportPacket(PlayerTeleportEvent event) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
    packet
        .getIntegers()
        .write(0, event.getPlayer().getEntityId())
        .write(1, toFixedPoint(event.getTo().getX()))
        .write(2, toFixedPoint(event.getTo().getY()))
        .write(3, toFixedPoint(event.getTo().getZ()));
    packet
        .getBytes()
        .write(0, toAngle(event.getTo().getYaw()))
        .write(1, toAngle(event.getTo().getPitch()));
    packet.getBooleans().write(0, event.getPlayer().isOnGround());
    return packet;
  }

  public static PacketContainer createEntityHeadRotationPacket(Entity entity) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
    packet.getIntegers().write(0, entity.getEntityId());
    packet.getBytes().write(0, toAngle(entity.getLocation().getYaw()));
    return packet;
  }

  public static PacketContainer createEntityHeadRotationPacket(Entity entity, float yaw) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
    packet.getIntegers().write(0, entity.getEntityId());
    packet.getBytes().write(0, toAngle(yaw));
    return packet;
  }

  public static PacketContainer createEntityMetadataPacket(Entity entity) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
    packet.getIntegers().write(0, entity.getEntityId());
    WrappedDataWatcher wdw = new WrappedDataWatcher(entity);
    wdw.setObject(0, (byte) 0);
    wdw.setObject(1, 300);
    wdw.setObject(2, "");
    wdw.setObject(3, (byte) 0);
    wdw.setObject(4, (byte) 0);
    wdw.setObject(6, (float) 20.0);
    wdw.setObject(7, 0);
    wdw.setObject(8, (byte) 0);
    wdw.setObject(9, (byte) 0);
    wdw.setObject(10, (byte) 0);
    wdw.setObject(16, (byte) 0);
    wdw.setObject(17, (float) 0);
    wdw.setObject(18, 0);
    packet.getWatchableCollectionModifier().write(0, wdw.getWatchableObjects());
    return packet;
  }

  public static PacketContainer createEntityMetadataPacket_Dead(Entity entity) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
    packet.getIntegers().write(0, entity.getEntityId());
    WrappedDataWatcher wdw = new WrappedDataWatcher(entity);
    wdw.setObject(0, (byte) 0);
    wdw.setObject(1, 300);
    wdw.setObject(2, "");
    wdw.setObject(3, (byte) 0);
    wdw.setObject(4, (byte) 0);
    wdw.setObject(6, (float) 0.0);
    wdw.setObject(7, 0);
    wdw.setObject(8, (byte) 0);
    wdw.setObject(9, (byte) 0);
    wdw.setObject(10, (byte) 0);
    wdw.setObject(16, (byte) 0);
    wdw.setObject(17, (float) 0);
    wdw.setObject(18, 0);
    packet.getWatchableCollectionModifier().write(0, wdw.getWatchableObjects());
    return packet;
  }

  public static PacketContainer createUpdateAttributesPacket(Entity entity) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.UPDATE_ATTRIBUTES);
    packet.getIntegers().write(0, entity.getEntityId());

    return packet;
  }

  public static PacketContainer createPlayerInfoPacket_AddPlayer(UUID uuid) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
    packet.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);
    WrappedGameProfile gameProfile = new WrappedGameProfile(uuid, "MatchRecorder");
    PlayerInfoData playerInfo =
        new PlayerInfoData(
            gameProfile,
            1,
            EnumWrappers.NativeGameMode.CREATIVE,
            WrappedChatComponent.fromText("MatchRecorder"));
    packet.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfo));
    return packet;
  }

  public static PacketContainer createPlayerInfoPacket_AddPlayer(
      EnumWrappers.PlayerInfoAction action, Player player) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
    packet.getPlayerInfoAction().write(0, action);
    PlayerInfoData playerInfo =
        new PlayerInfoData(
            WrappedGameProfile.fromPlayer(player),
            player.spigot().getPing(),
            EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode()),
            WrappedChatComponent.fromText(player.getDisplayName()));
    packet.getPlayerInfoDataLists().write(0, Collections.singletonList(playerInfo));
    return packet;
  }

  public static PacketContainer createPlayerInfoPacket_UpdateGamemode(
      EnumWrappers.PlayerInfoAction action, Player player) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
    packet.getPlayerInfoAction().write(0, action);
    return packet;
  }

  public static PacketContainer createPlayerInfoPacket_UpdateLatency(
      EnumWrappers.PlayerInfoAction action, Player player) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
    packet.getPlayerInfoAction().write(0, action);
    return packet;
  }

  public static PacketContainer createPlayerInfoPacket_UpdateDisplayName(
      EnumWrappers.PlayerInfoAction action, Player player) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
    packet.getPlayerInfoAction().write(0, action);
    return packet;
  }

  public static PacketContainer createPlayerInfoPacket_RemovePlayer(
      EnumWrappers.PlayerInfoAction action, Player player) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
    packet.getPlayerInfoAction().write(0, action);
    return packet;
  }

  public static PacketContainer createScoreboardObjectivePacket_Create(
      String name, String display) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
    packet.getStrings().write(0, name);
    packet.getChatComponents().write(0, WrappedChatComponent.fromLegacyText(display));
    packet.getIntegers().write(0, 0);
    packet.getEnumModifier(HealthDisplay.class, 2).write(0, HealthDisplay.INTEGER);
    return packet;
  }

  public static PacketContainer createScoreboardObjectivePacket_Update(
      String name, String display) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
    packet.getStrings().write(0, name);
    packet.getChatComponents().write(0, WrappedChatComponent.fromLegacyText(display));
    packet.getIntegers().write(0, 2);
    packet.getEnumModifier(HealthDisplay.class, 2).write(0, HealthDisplay.INTEGER);
    return packet;
  }

  public static PacketContainer createScoreboardScorePacket_Order(String name, int order) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_SCORE);
    packet.getStrings().write(0, "§" + order);
    packet.getScoreboardActions().write(0, ScoreboardAction.CHANGE);
    packet.getStrings().write(1, name);
    packet.getIntegers().write(0, order);
    return packet;
  }

  public static PacketContainer createScoreboardDisplayObjectivePacket(String name) {
    PacketContainer packet =
        new PacketContainer(PacketType.Play.Server.SCOREBOARD_DISPLAY_OBJECTIVE);
    packet.getStrings().write(0, name);
    packet.getIntegers().write(0, 1);
    return packet;
  }

  public static PacketContainer createScoreboardTeamPacket_Create(Party party) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
    packet.getStrings().write(0, toShortName(party.getNameLegacy()));
    packet.getIntegers().write(1, 0);
    packet.getStrings().write(1, party.getColor().toString() + toShortName(party.getNameLegacy()));
    packet.getStrings().write(2, party.getColor().toString());
    packet.getStrings().write(3, "§f");
    packet.getIntegers().write(2, 2);
    packet.getStrings().write(4, "always");
    return packet;
  }

  public static PacketContainer createScoreboardTeamPacket_Remove(Party party) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
    packet.getStrings().write(0, toShortName(party.getNameLegacy()));
    packet.getIntegers().write(1, 1);
    return packet;
  }

  public static PacketContainer createScoreboardTeamPacket_Modify(Party party) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
    packet.getStrings().write(0, toShortName(party.getNameLegacy()));
    packet.getIntegers().write(1, 0);
    packet.getStrings().write(1, party.getColor().toString() + toShortName(party.getNameLegacy()));
    packet.getStrings().write(2, party.getColor().toString());
    packet.getStrings().write(3, "§f");
    packet.getIntegers().write(2, 2);
    packet.getStrings().write(4, "always");
    Collection<String> playerNames =
        party.getPlayers().stream()
            .map(player -> player.getNameLegacy())
            .collect(Collectors.toList());
    packet.getSpecificModifier(Collection.class).write(0, playerNames);
    return packet;
  }

  public static PacketContainer createScoreboardTeamPacket_AddPlayer(
      MatchPlayer player, Party party) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
    packet.getStrings().write(0, toShortName(party.getNameLegacy()));
    packet.getIntegers().write(1, 3);
    Collection<String> playerNames = Stream.of(player.getNameLegacy()).collect(Collectors.toList());
    packet.getSpecificModifier(Collection.class).write(0, playerNames);
    return packet;
  }

  public static PacketContainer createScoreboardTeamPacket_RemovePlayer(
      MatchPlayer player, Party party) {
    PacketContainer packet = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
    packet.getStrings().write(0, toShortName(party.getNameLegacy()));
    packet.getIntegers().write(1, 4);
    Collection<String> playerNames = Stream.of(player.getNameLegacy()).collect(Collectors.toList());
    packet.getSpecificModifier(Collection.class).write(0, playerNames);
    return packet;
  }

  public static PacketContainer createMapChunkPacket(Chunk chunk) {
    net.minecraft.server.v1_8_R3.Chunk rawChunk = ((CraftChunk) chunk).getHandle();
    PacketPlayOutMapChunk rawPacket = new PacketPlayOutMapChunk(rawChunk, true, 0);
    return PacketContainer.fromPacket(rawPacket);
  }

  public static PacketContainer createMapChunkBulkPacket(List<Chunk> chunks) {
    List<net.minecraft.server.v1_8_R3.Chunk> rawChunks =
        chunks.stream().map(chunk -> ((CraftChunk) chunk).getHandle()).collect(Collectors.toList());
    PacketPlayOutMapChunkBulk rawPacket = new PacketPlayOutMapChunkBulk(rawChunks);
    return PacketContainer.fromPacket(rawPacket);
  }

  private static String toShortName(String legacyName) {
    String lower = legacyName.toLowerCase();
    if (lower.endsWith(" team")) {
      return legacyName.substring(0, lower.length() - " team".length());
    } else if (lower.startsWith("team ")) {
      return legacyName.substring("team ".length());
    } else {
      return legacyName;
    }
  }

  private static int toFixedPoint(double val) {
    return (int) MathHelper.floor(val * 32.0D);
  }

  private static byte toAngle(float val) {
    return (byte) ((int) (val * 256.0F / 360.0F));
  }

  private static int toVelocity(double val) {
    return (int) (val * 8000.0D);
  }

  public static enum HealthDisplay {
    INTEGER,
    HEARTS
  }
}
