package ac.boar.geyser;

import ac.boar.anticheat.Boar;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;

public class GeyserSessionJoinEvent implements EventRegistrar {
    public GeyserSessionJoinEvent() {
        GeyserApi.api().eventBus().subscribe(this, SessionLoginEvent.class, this::onSessionJoin);
        GeyserApi.api().eventBus().subscribe(this, SessionDisconnectEvent.class, this::onSessionLeave);
    }

    public void onSessionJoin(SessionLoginEvent event) {
        Boar.getInstance().getPlayerManager().add(event.connection());
    }

    public void onSessionLeave(SessionDisconnectEvent event) {
        Boar.getInstance().getPlayerManager().remove(event.connection());
    }
}