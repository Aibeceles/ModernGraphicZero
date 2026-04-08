package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbConfig {
    private static final Properties props = new Properties();
    static {
        try (InputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(
                "Missing db.properties — copy db.properties.example and fill in your values", e);
        }
    }
    public static String get(String key) {
        return props.getProperty(key);
    }
}
