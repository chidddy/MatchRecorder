package tc.oc.occ.matchrecorder;

import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.Location;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.player.MatchPlayer;

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

  public void removePlayer(MatchPlayer player) {
    replay.removePlayer(player);
  }

  public void addPacket(PacketContainer packet) {
    replay.addPacket(packet);
  }

  public void addChunkPacket(PacketContainer packet) {
    replay.addChunkPacket(packet);
  }
}
