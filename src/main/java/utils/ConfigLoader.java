package utils;

import javax.validation.constraints.NotNull;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    /**
     * Wrapper around config properties to make support only valid property names
     * */
    public enum ConfigProperty {
        GOOGLE_MAPS_API_KEY("GOOGLE_MAPS_API_KEY"),
        TELEGRAM_BOT_API_KEY("TELEGRAM_BOT_API_KEY"),
        TELEGRAM_BOT_USERNAME("TELEGRAM_BOT_USERNAME"),
        TELEGRAM_BOT_CREATOR_ID("TELEGRAM_BOT_CREATOR_ID");

        private final String text;

        /**
         * @param text name of the key to the config property
         */
        ConfigProperty(final String text) {
            this.text = text;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
        }
    }

    public static final String configPath = "src/main/resources/config.properties";

    /**
     * Returns String property from the config file located at {@code ConfigLoader.configPath}
     *
     * @param key ConfigProperty that needs to be parsed from properties
     * @return String value of the property. Never {@code null}. Returns empty string if such property was not config
     * @throws IOException if there was no config file, or it's reading failed
     * */
    public static String getProperty(@NotNull ConfigProperty key) throws IOException {
        Properties configProperties = new Properties();
        try {
            configProperties.load(new FileInputStream(configPath));
        } catch (IOException e) {
            throw new IOException(e.getMessage() + "\nConfig file should be located under " + configPath + "!");
        }
        return configProperties.getProperty(key.toString(), "");
    }
}
