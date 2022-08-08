package tc.oc.occ.matchrecorder;

import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
  public static boolean SIGN_REPLAYS = false;
  public static String PRIVATE_KEY = "recorder_rsa";
  public static String PUBLIC_KEY = "recorder_rsa.pub";
  public static boolean ABSOLUTE = false;
  public static String OUTPUT_DIR = "replays";

  public static void setConfig(FileConfiguration config) {
    SIGN_REPLAYS = config.getBoolean("signing.sign", SIGN_REPLAYS);
    PRIVATE_KEY = config.getString("signing.private-key", PRIVATE_KEY);
    PUBLIC_KEY = config.getString("signing.public-key", PUBLIC_KEY);
    ABSOLUTE = config.getBoolean("output.absolute", ABSOLUTE);
    OUTPUT_DIR = config.getString("output.dir", OUTPUT_DIR);
  }
}
