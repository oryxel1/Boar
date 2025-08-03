package ac.boar.anticheat.alert;

import org.geysermc.geyser.api.command.CommandSource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AlertManager {
    public final static String PREFIX = "§3Boar §7>§r ";
    public final static String BEDROCK_PREFIX = "§sBoar §i>§r ";

    private final Map<UUID, CommandSource> sources = new ConcurrentHashMap<>();

    public void alert(String verbose) {
        sources.values().forEach(source -> source.sendMessage(PREFIX + "§3" + verbose));
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
