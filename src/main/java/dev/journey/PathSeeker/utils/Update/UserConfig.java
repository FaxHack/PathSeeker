package dev.journey.PathSeeker.utils.Update;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class UserConfig {
    private static final String CONFIG_FILE = "pathseeker_config.properties";
    private static final Properties properties = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            // If the file doesn't exist, it will be created when saving.
        }
    }

    public static boolean isUpdateCheckDisabled() {
        return Boolean.parseBoolean(properties.getProperty("updateCheckDisabled", "false"));
    }

    public static void setUpdateCheckDisabled(boolean disabled) {
        properties.setProperty("updateCheckDisabled", Boolean.toString(disabled));
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Pathseeker Addon Config");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}