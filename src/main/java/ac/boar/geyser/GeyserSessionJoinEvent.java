package ac.boar.geyser;

import ac.boar.anticheat.Boar;
import org.geysermc.event.PostOrder;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.EventRegistrar;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;

public class GeyserSessionJoinEvent implements EventRegistrar {
    public GeyserSessionJoinEvent() {
        GeyserApi.api().eventBus().register(this, this);
    }

    @Subscribe(postOrder = PostOrder.FIRST)
    public void onSessionJoin(SessionLoginEvent event) {
        Boar.getInstance().getPlayerManager().add(event.connection());
    }

    @Subscribe(postOrder = PostOrder.FIRST)
    public void onSessionLeave(SessionDisconnectEvent event) {
        Boar.getInstance().getPlayerManager().remove(event.connection());
    }
}