package tc.oc.occ.matchrecorder;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReplayMeta implements Serializable {
  private String name;
  private final boolean singleplayer = false;
  private final String generator = "OCC Match Recorder";
  private final String serverName = "play.oc.tc";
  private long duration;
  private long date;
  private final String mcversion = "1.8.9";
  private final int protocol = 47;
  private final int fileFormatVersion = 14;

  public ReplayMeta(String name, long duration, long date) {
    this.name = name;
    this.duration = duration;
    this.date = date;
  }

  public String getName() {
    return this.name;
  }

  public boolean isSingleplayer() {
    return this.singleplayer;
  }

  public String getGenerator() {
    return this.generator;
  }

  public String getServer() {
    return this.serverName;
  }

  public long getDuration() {
    return this.duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public long getDate() {
    return this.date;
  }

  public void setDate(long date) {
    this.date = date;
  }

  public String getVersion() {
    return this.mcversion;
  }

  public int getProtocol() {
    return this.protocol;
  }

  public int getFormat() {
    return this.fileFormatVersion;
  }

  public File getFile() {
    return new File(
        MatchRecorder.get().getReplayFolder(),
        this.name
            + "-"
            + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(this.date))
            + ".mcpr");
  }
}
