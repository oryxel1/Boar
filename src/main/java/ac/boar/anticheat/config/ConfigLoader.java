package ac.boar.anticheat.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.geysermc.geyser.api.extension.Extension;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;

// Credit to https://github.com/onebeastchris/MagicMenu
public class ConfigLoader {
    public static <T> T load(Extension extension, Class<?> extensionClass, Class<T> configClass, T  defaultConfig) {
        File configFile = extension.dataFolder().resolve("config.yml").toFile();

        // Ensure the data folder exists
        if (!extension.dataFolder().toFile().exists()) {
            if (!extension.dataFolder().toFile().mkdirs()) {
                extension.logger().error("Failed to create data folder");
                return null;
            }
        }

        // Create the config file if it doesn't exist
        if (!configFile.exists()) {
            if (writeConfigFile(configFile, extensionClass, extension, null)) {
                return defaultConfig;
            }
        }

        // Load the config file
        try {
            return new ObjectMapper(new YAMLFactory())
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                    .disable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                    .readValue(configFile, configClass);
        } catch (IOException e) {
            extension.logger().error("Failed to load config", e);
            return null;
        }
    }

    public static void save(Extension extension, Class<?> extensionClass, Config config) {
        File configFile = extension.dataFolder().resolve("config.yml").toFile();
        // Well in case the old config file the server have is outdated.
        writeConfigFile(configFile, extensionClass, extension, config);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean writeConfigFile(File configFile, Class<?> extensionClass, Extension extension, Config config) {
        try (FileWriter writer = new FileWriter(configFile)) {
            try (FileSystem fileSystem = FileSystems.newFileSystem(new File(extensionClass.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath(), Collections.emptyMap())) {
                try (InputStream input = Files.newInputStream(fileSystem.getPath("config.yml"))) {
                    byte[] bytes = new byte[input.available()];
                    input.read(bytes);

                    String s = new String(bytes);
                    // not really cool code ;(, and a bit hacky but ok....
                    if (config != null) {
                        s = s.replace("player-rewind-history-size-ticks: 20", "player-rewind-history-size-ticks: " + config.rewindHistory());
                        s = s.replace("player-position-acceptance-threshold: 1.0E-4", "player-position-acceptance-threshold: " + config.acceptanceThreshold());
                        s = s.replace("disabled-checks: []", "disabled-checks: " + Arrays.toString(config.disabledChecks().toArray(new String[0])));
                        s = s.replace("ignore-ghost-block-place: false", "ignore-ghost-block-place: " + config.ignoreGhostBlockPlace());
                    }

                    writer.write(s.toCharArray());
                    writer.flush();
                }
            }
        } catch (IOException | URISyntaxException e) {
            extension.logger().error("Failed to create config", e);
            return false;
        }

        return true;
    }
}