package ac.boar.geyser;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.data.cache.UseDurationCache;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;

public class GeyserBoar implements Extension {
    @Subscribe
    public void onSessionJoin(SessionLoginEvent event) {
        Boar.getInstance().getPlayerManager().add(event.connection());
    }

    @Subscribe
    public void onSessionLeave(SessionDisconnectEvent event) {
        Boar.getInstance().getPlayerManager().remove(event.connection());
    }

    @Subscribe
    public void onGeyserPostInitializeEvent(GeyserPostInitializeEvent event) {
        UseDurationCache.init();
        Boar.getInstance().init();
    }

    @Subscribe
    public void onGeyserShutdown(GeyserShutdownEvent event) {
        Boar.getInstance().terminate();
    }
}
