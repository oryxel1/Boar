package ac.boar.geyser;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.alert.AlertManager;
import ac.boar.anticheat.player.BoarPlayer;
import lombok.Getter;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserRegisterPermissionsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.api.util.TriState;
import org.geysermc.geyser.session.GeyserSession;

public class GeyserBoar implements Extension {
    @Getter
    private static ExtensionLogger logger;

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
                    if (alertManager.hasAlert(source)) {
                        alertManager.removeAlert(source);
                        source.sendMessage(AlertManager.PREFIX + "§fDisabled alerts.");
                    } else {
                        alertManager.addAlert(source);
                        source.sendMessage(AlertManager.PREFIX + "§fEnabled alerts.");
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
