package tc.oc.occ.matchrecorder.Listeners;

import com.google.common.collect.Lists;
import java.util.Arrays;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import tc.oc.occ.matchrecorder.PacketBuilder;
import tc.oc.occ.matchrecorder.Recorder;
import tc.oc.pgm.api.match.event.MatchLoadEvent;

public class WorldListener implements Listener {
  private final Recorder recorder;

  public WorldListener(Recorder recorder) {
    this.recorder = recorder;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onChunkLoad(ChunkLoadEvent event) {
    if (event.isNewChunk()) {
      if (recorder.isRecording()) {
        recorder.addPacket(PacketBuilder.createMapChunkPacket(event.getChunk()));
      } else {
        recorder.addChunkPacket(PacketBuilder.createMapChunkPacket(event.getChunk()));
      }
    }
  }

  // *NOTES:
  // * EACH CHUNK BULK HAS 10 CHUNKS
  @EventHandler(priority = EventPriority.MONITOR)
  public void onMatchLoad(MatchLoadEvent event) {
    recorder.chearChunks();
    Lists.partition(Arrays.asList(event.getWorld().getLoadedChunks()), 10)
        .forEach(
            chunkList -> {
              recorder.addChunkPacket(PacketBuilder.createMapChunkBulkPacket(chunkList));
            });
  }
}
