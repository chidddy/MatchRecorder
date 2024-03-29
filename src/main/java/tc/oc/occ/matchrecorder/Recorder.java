package tc.oc.occ.matchrecorder;

import com.comphenix.protocol.events.PacketContainer;
import java.time.Duration;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.goals.Goal;

public class Recorder {
  private final Replay replay = new Replay();

  public void startRecording(Match match) {
    replay.startRecording(match);
  }

  public void stopRecording(Match match) {
    replay.stopRecording(match);
  }

  public boolean isRecording() {
    return replay.isRecording();
  }

  public void createPlayer(MatchPlayer player, Location location) {
    replay.createPlayer(player, location);
  }

  public void updatePlayerItems(Player player) {
    replay.updatePlayerItems(player);
  }

  public void updatePlayerItems(Player player, int slot) {
    replay.updatePlayerItems(player, slot);
  }

  public void removePlayer(MatchPlayer player, boolean remove) {
    replay.removePlayer(player, remove);
  }

  public void addPacket(PacketContainer packet) {
    replay.addPacket(packet);
  }

  public void addChunkPacket(PacketContainer packet) {
    replay.addChunkPacket(packet);
  }

  public void clearChunks() {
    replay.clearChunks();
  }

  public void killPlayer(MatchPlayer player) {
    replay.killPlayer(player);
  }

  public void updateSidebar() {
    replay.updateSidebar();
  }

  public void blinkGoal(Goal goal, float rateHz, @Nullable Duration duration) {
    replay.blinkGoal(goal, rateHz, duration);
  }

  public void stopBlinkingGoal(Goal goal) {
    replay.stopBlinkingGoal(goal);
  }
}
