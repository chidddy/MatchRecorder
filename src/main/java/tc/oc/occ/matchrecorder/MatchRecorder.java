package tc.oc.occ.matchrecorder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.occ.matchrecorder.Listeners.MatchListener;
import tc.oc.occ.matchrecorder.Listeners.PacketListener;
import tc.oc.occ.matchrecorder.Listeners.PlayerListener;
import tc.oc.occ.matchrecorder.Listeners.WorldListener;

public class MatchRecorder extends JavaPlugin implements Listener {

  public static final String PREFIX = "[MatchRecorder]";
  private static MatchRecorder instance = null;
  public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private Recorder recorder = null;
  private File replayFolder;

  @Override
  public void onEnable() {
    instance = this;
    this.recorder = new Recorder();
    registerEvents();
    this.replayFolder = new File(getDataFolder().getPath() + "/replays/");
  }

  public static MatchRecorder get() {
    return instance;
  }

  public Recorder getRecorder() {
    return this.recorder;
  }

  public File getReplayFolder() {
    return this.replayFolder;
  }

  private void registerEvents() {
    Bukkit.getPluginManager().registerEvents(new PlayerListener(this.recorder), this);
    Bukkit.getPluginManager().registerEvents(new MatchListener(this.recorder), this);
    Bukkit.getPluginManager().registerEvents(new PacketListener(this), this);
    Bukkit.getPluginManager().registerEvents(new WorldListener(this.recorder), this);
  }
}
