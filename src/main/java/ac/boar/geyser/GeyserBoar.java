package ac.boar.geyser;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.acks.BoarAcknowledgement;
import ac.boar.anticheat.alert.AlertManager;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.injector.BoarInjector;
import lombok.Getter;
import org.cloudburstmc.netty.channel.raknet.RakChildChannel;
import org.cloudburstmc.netty.handler.codec.raknet.common.RakSessionCodec;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.*;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.session.GeyserSession;

public class GeyserBoar implements Extension {
    @Getter
    private static ExtensionLogger logger;

    @Subscribe
    public void onSessionJoin(SessionLoginEvent event) {
        BoarPlayer player = Boar.getInstance().getPlayerManager().add(event.connection());
        if (player == null) {
            return;
        }

        RakSessionCodec rakSessionCodec = ((RakChildChannel) player.getSession().getUpstream().getSession().getPeer().getChannel()).rakPipeline().get(RakSessionCodec.class);
        BoarAcknowledgement.getRakSessionToPlayer().put(rakSessionCodec, player);
    }

    @Subscribe
    public void onSessionLeave(SessionDisconnectEvent event) {
        BoarPlayer player = Boar.getInstance().getPlayerManager().remove(event.connection());
        if (player == null) {
            return;
        }

        RakSessionCodec rakSessionCodec = ((RakChildChannel) player.getSession().getUpstream().getSession().getPeer().getChannel()).rakPipeline().get(RakSessionCodec.class);
        BoarAcknowledgement.getRakSessionToPlayer().remove(rakSessionCodec);
    }

    @Subscribe
    public void onGeyserPreInitializeEvent(GeyserPreInitializeEvent event) {
        BoarInjector.injectToRak();
    }

    @Subscribe
    public void onGeyserPostInitializeEvent(GeyserPostInitializeEvent event) {
        logger = this.logger();

        Boar.getInstance().init(this);
    }

    @Subscribe
    public void onGeyserShutdown(GeyserShutdownEvent event) {
        Boar.getInstance().terminate(this);
    }

    @Subscribe
    public void onRegisterPermissions(GeyserRegisterPermissionsEvent event) {
        event.register("boar.exempt", TriState.FALSE);
        event.register("boar.alert", TriState.NOT_SET);
        event.register("boar.preddebug", TriState.NOT_SET);
    }

    @Subscribe
    public void onDefineCommands(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this).source(CommandSource.class)
                .name("alert")
                .playerOnly(true)
                .description("Enable alert messages.")
                .permission("boar.alert")
                .executor((source, cmd, args) -> {
                    AlertManager alertManager = Boar.getInstance().getAlertManager();

                    String prefix = alertManager.getPrefix(source);
                    if (alertManager.hasAlert(source)) {
                        alertManager.removeAlert(source);
                        source.sendMessage(prefix + "§fDisabled alerts.");
                    } else {
                        alertManager.addAlert(source);
                        source.sendMessage(prefix + "§fEnabled alerts.");
                    }
                })
                .build());

        event.register(Command.builder(this).source(CommandSource.class)
                .name("preddebug").playerOnly(true).bedrockOnly(true)
                .description("Enable prediction debug message.")
                .permission("boar.preddebug")
                .executor((source, cmd, args) -> {
                    if (!(source.connection() instanceof GeyserSession session)) {
                        source.sendMessage("Failed, not GeyserSession!");
                        return;
                    }

                    BoarPlayer player = Boar.getInstance().getPlayerManager().get(session);
                    if (player == null) {
                        source.sendMessage("Failed, can't find player session!");
                        return;
                    }

                    player.setDebugMode(!player.isDebugMode());
                    source.sendMessage("Done! Current debug state: " + player.isDebugMode());
                })
                .build());
    }
}
