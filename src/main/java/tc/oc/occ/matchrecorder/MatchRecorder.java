package tc.oc.occ.matchrecorder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import tc.oc.occ.matchrecorder.Listeners.MatchListener;
import tc.oc.occ.matchrecorder.Listeners.PacketListener;
import tc.oc.occ.matchrecorder.Listeners.PlayerListener;
import tc.oc.occ.matchrecorder.Listeners.WorldListener;

public class MatchRecorder extends JavaPlugin implements Listener {

  public static final String PREFIX = "[MatchRecorder]";
  private static MatchRecorder instance;
  public static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private Recorder recorder;
  private HashManager hashManager;
  private File replayFolder;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    ConfigManager.setConfig(getConfig());
    instance = this;
    this.recorder = new Recorder();
    registerEvents();
    registerReplayFolder();
    registerHashManager();
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

  public HashManager getHashManager() {
    return this.hashManager;
  }

  private void registerReplayFolder() {
    if (ConfigManager.ABSOLUTE) {
      this.replayFolder = new File(ConfigManager.OUTPUT_DIR).getAbsoluteFile();
    } else {
      this.replayFolder = new File(getDataFolder().getPath(), ConfigManager.OUTPUT_DIR);
      this.replayFolder.mkdirs();
    }
  }

  private void registerHashManager() {
    if (ConfigManager.SIGN_REPLAYS) {
      try {
        this.hashManager =
            new HashManager(
                new File(getDataFolder().getPath(), ConfigManager.PRIVATE_KEY),
                new File(getDataFolder().getPath(), ConfigManager.PUBLIC_KEY));
      } catch (Exception e) {
        getLogger().log(Level.SEVERE, "Cannot start hashManager:" + e.toString());
      }
    }
  }

  private void registerEvents() {
    Bukkit.getPluginManager().registerEvents(new MatchListener(this.recorder), this);
    Bukkit.getPluginManager().registerEvents(new WorldListener(this.recorder), this);
    Bukkit.getPluginManager().registerEvents(new PlayerListener(this.recorder), this);
    Bukkit.getPluginManager().registerEvents(new PacketListener(this), this);
  }
}
