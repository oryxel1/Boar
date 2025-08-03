package ac.boar.anticheat.alert;

import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManager {
    private final static String PREFIX = "§3Boar §7>§r ";
    private final static String BEDROCK_PREFIX = "§sBoar §i>§r ";

    private final Map<UUID, CommandSource> sources = new ConcurrentHashMap<>();

    public void alert(String verbose) {
        sources.values().forEach(source -> source.sendMessage(getPrefix(source) + "§3" + verbose));
    }

    public String getPrefix(CommandSource source) {
        if (source instanceof GeyserConnection) {
            return BEDROCK_PREFIX;
        }

        return PREFIX;
    }

    public boolean hasAlert(CommandSource source) {
        return this.sources.containsKey(source.playerUuid());
    }

    public void addAlert(CommandSource source) {
        this.sources.put(source.playerUuid(), source);
    }

    public void removeAlert(CommandSource source) {
        this.sources.remove(source.playerUuid());
    }
}
