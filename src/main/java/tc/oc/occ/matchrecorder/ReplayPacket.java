package tc.oc.occ.matchrecorder;

import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.injector.netty.WirePacket;
import java.io.Serializable;

public class ReplayPacket extends WirePacket implements Serializable {
  private final int time;
  private final PacketContainer packet;

  ReplayPacket(int time, PacketContainer packet) {
    super(PacketBuilder.getPacketID(packet.getType()), WirePacket.bytesFromPacket(packet));
    this.time = time;
    this.packet = packet;
  }

  public int getTime() {
    return this.time;
  }

  @Override
  public int getId() {
    return PacketBuilder.getPacketID(this.packet.getType());
  }

  @Override
  public int hashCode() {
    return this.packet.hashCode();
  }
}
