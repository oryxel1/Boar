package ac.boar.geyser;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.data.cache.UseDurationCache;
import ac.boar.anticheat.player.BoarPlayer;
import lombok.Getter;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.bedrock.SessionDisconnectEvent;
import org.geysermc.geyser.api.event.bedrock.SessionLoginEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.extension.ExtensionLogger;
import org.geysermc.geyser.session.GeyserSession;

import java.util.List;

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

        UseDurationCache.init();
        Boar.getInstance().init();
    }

    @Subscribe
    public void onGeyserShutdown(GeyserShutdownEvent event) {
        Boar.getInstance().terminate();
    }

    @Subscribe
    public void onDefineCommands(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this) // "this" is the extension's main class
                .name("preddebug")
                .bedrockOnly(true)
                .playerOnly(true)
                .source(CommandSource.class)
                .aliases(List.of("debug", "db"))
                .description("Enable prediction debug message")
                .permission("geyser")
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

        event.register(Command.builder(this) // "this" is the extension's main class
                .name("alert")
                .bedrockOnly(true)
                .playerOnly(true)
                .source(CommandSource.class)
                .aliases(List.of("al"))
                .description("Enable alert messages")
                .permission("geyser")
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

                    player.setAlertEnabled(!player.isAlertEnabled());
                    source.sendMessage("Done! Current alert state: " + player.isAlertEnabled());
                })
                .build());
    }
}
