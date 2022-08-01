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

public class MatchRecorderPlugin extends JavaPlugin implements Listener {

  public static final String PREFIX = "[MatchRecorder]";
  private static MatchRecorderPlugin instance = null;
  public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private File replayFolder;

  @Override
  public void onEnable() {
    instance = this;
    Replay.initalize();
    registerEvents();
    this.replayFolder = new File(getDataFolder().getPath() + "/replays/");
  }

  public static MatchRecorderPlugin get() {
    return instance;
  }

  public File getReplayFolder() {
    return this.replayFolder;
  }

  private void registerEvents() {
    Bukkit.getPluginManager().registerEvents(new PlayerListener(), this);
    Bukkit.getPluginManager().registerEvents(new MatchListener(), this);
    Bukkit.getPluginManager().registerEvents(new PacketListener(this), this);
  }
}
