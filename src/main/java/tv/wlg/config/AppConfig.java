package tv.wlg.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Properties;

public class AppConfig {
    private static final Properties configs = new Properties();

    @SuppressWarnings({"InstantiationOfUtilityClass", "unused"})
    private static final AppConfig instance = new AppConfig();
    private AppConfig() {
        loadConfigurations();
    }

    private static void loadConfigurations() {
        File file = new File("./config.properties");
        try(FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(fileInputStream, Charset.forName("windows-1251"))) {
            configs.load(reader);
        } catch (Exception ignore) {}
    }

    public static String getIRC() {
        return configs.getProperty("twitch.irc");
    }

    public static String getUser() {
        return configs.getProperty("twitch.user").toLowerCase();
    }

    public static String getChannel() {
        return configs.getProperty("twitch.channel", getUser()).toLowerCase();
    }

    public static String getToken() {
        return configs.getProperty("twitch.token");
    }

    public static String getDefaultGamePad() {
        return configs.getProperty("default.gamepad", "nes");
    }
}
