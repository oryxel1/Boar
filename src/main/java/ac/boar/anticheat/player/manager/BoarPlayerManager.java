package ac.boar.anticheat.player.manager;

import ac.boar.anticheat.data.PlayerAttributeData;
import ac.boar.anticheat.player.BoarPlayer;

import ac.boar.geyser.util.GeyserUtil;
import ac.boar.plugin.BoarSpigot;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.session.GeyserSession;

import java.util.HashMap;

public class BoarPlayerManager extends HashMap<GeyserConnection, BoarPlayer> {
    public void add(GeyserConnection connection) {
        if (!(connection instanceof GeyserSession)) {
            return;
        }

        final BoarPlayer player = new BoarPlayer((GeyserSession) connection);
        GeyserUtil.hookIntoCloudburstMC(player);
        this.put(connection, player);

        BoarSpigot.LOGGER.info(connection.bedrockUsername() + " joined!");
    }
}