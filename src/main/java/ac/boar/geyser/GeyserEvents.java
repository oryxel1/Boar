package ac.boar.geyser;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.data.cache.UseDurationCache;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;

public class GeyserEvents implements EventRegistrar {
    public GeyserEvents() {
        GeyserApi.api().eventBus().subscribe(this, SessionLoginEvent.class, this::onSessionJoin);
        GeyserApi.api().eventBus().subscribe(this, SessionDisconnectEvent.class, this::onSessionLeave);

        GeyserApi.api().eventBus().subscribe(this, GeyserPostInitializeEvent.class, this::onGeyserPostInitializeEvent);
    }

    public void onSessionJoin(SessionLoginEvent event) {
        Boar.getInstance().getPlayerManager().add(event.connection());
    }

    public void onSessionLeave(SessionDisconnectEvent event) {
        Boar.getInstance().getPlayerManager().remove(event.connection());
    }

    public void onGeyserPostInitializeEvent(GeyserPostInitializeEvent event) {
        UseDurationCache.init();
    }
}