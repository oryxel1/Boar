package ac.boar.anticheat.player.api;

import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.session.GeyserSession;

import java.util.HashMap;

public class BoarPlayerManager extends HashMap<GeyserConnection, BoarPlayer> {
    public void add(GeyserConnection connection) {
        if (!(connection instanceof GeyserSession)) {
            return;
        }

        final BoarPlayer player = new BoarPlayer((GeyserSession) connection);
        player.init();
        this.put(connection, player);
    }
}