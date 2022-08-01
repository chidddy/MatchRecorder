package tc.oc.occ.matchrecorder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bukkit.scheduler.BukkitRunnable;

public class ReplayWriter extends BukkitRunnable {

  private ZipOutputStream outputStream;
  private final ReplayMeta meta;
  private LinkedHashSet<ReplayPacket> packets;

  public ReplayWriter(File file, ReplayMeta meta, LinkedHashSet<ReplayPacket> packets)
      throws IOException {
    this.meta = meta;
    file.getParentFile().mkdirs();
    file.createNewFile();
    this.outputStream = new ZipOutputStream(new FileOutputStream(file));
    this.packets = packets;
  }

  @Override
  public void run() {
    try {
      writeFile();
      writeMeta(this.meta);
      this.outputStream.close();
    } catch (IOException e) {
      this.cancel();
    }
    MatchRecorder.get().getLogger().log(Level.INFO, "finished writing");
  }

  private void writeFile() throws IOException {
    this.outputStream.putNextEntry(new ZipEntry("recording.tmcpr"));
    // ? find a better way of doing this while handling the IOException
    Iterator<ReplayPacket> it = this.packets.iterator();
    while (it.hasNext()) {
      writePacket(it.next());
    }
    this.outputStream.closeEntry();
  }

  private void writePacket(ReplayPacket packet) throws IOException {
    byte[] packetID = toVarInt(packet.getId());
    this.outputStream.write(toByteArray(packet.getTime()));
    this.outputStream.write(toByteArray(packetID.length + packet.getBytes().length));
    this.outputStream.write(packetID);
    this.outputStream.write(packet.getBytes());
    this.outputStream.flush();
  }

  private void writeMeta(ReplayMeta meta) throws IOException {
    this.outputStream.putNextEntry(new ZipEntry("metaData.json"));
    this.outputStream.write(MatchRecorder.GSON.toJson(meta, ReplayMeta.class).getBytes());
    this.outputStream.flush();
    this.outputStream.closeEntry();
  }

  private byte[] toVarInt(int val) {
    ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
    do {
      byte temp = (byte) (val & 0b01111111);
      val >>>= 7;
      if (val != 0) {
        temp |= 0b10000000;
      }
      arrayOutputStream.write(temp);
    } while (val != 0);
    return arrayOutputStream.toByteArray();
  }

  private byte[] toByteArray(int val) {
    return new byte[] {(byte) (val >> 24), (byte) (val >> 16), (byte) (val >> 8), (byte) val};
  }
}
