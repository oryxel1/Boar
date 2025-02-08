package ac.boar.anticheat.player.manager;

import ac.boar.anticheat.data.PlayerAttributeData;
import ac.boar.anticheat.player.BoarPlayer;

import ac.boar.util.GeyserUtil;
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
        GeyserUtil.injectCloudburst(player);
        for (final GeyserAttributeType type : GeyserAttributeType.values()) {
            player.attributes.put(type.getBedrockIdentifier(), new PlayerAttributeData(type.getDefaultValue()));
        }
        player.attributes.put(GeyserAttributeType.MOVEMENT_SPEED.getBedrockIdentifier(), new PlayerAttributeData(0.1F));
        this.put(connection, player);
    }
}