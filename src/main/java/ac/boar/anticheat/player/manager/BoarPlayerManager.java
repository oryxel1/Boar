package ac.boar.anticheat.player.manager;

import ac.boar.anticheat.data.AttributeData;
import ac.boar.anticheat.player.BoarPlayer;

import ac.boar.util.GeyserUtil;
import org.geysermc.geyser.api.connection.GeyserConnection;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;

import java.util.HashMap;

public class BoarPlayerManager extends HashMap<GeyserConnection, BoarPlayer> {
    public void add(GeyserConnection connection) {
        if (!(connection instanceof GeyserSession)) {
            return;
        }

        final BoarPlayer player = new BoarPlayer((GeyserSession) connection);
        GeyserUtil.injectCloudburst(player);
        for (final AttributeType.Builtin type : AttributeType.Builtin.values()) {
            player.attributes.put(type.getId(), new AttributeData((float) type.getDef()));
        }
        player.attributes.put(AttributeType.Builtin.MOVEMENT_SPEED.getId(), new AttributeData(0.1F));
        this.put(connection, player);
    }
}