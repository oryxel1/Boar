package ac.boar.anticheat;

import ac.boar.anticheat.player.BoarPlayerManager;
import ac.boar.geyser.GeyserSessionJoinEvent;
import lombok.Getter;

@Getter
public class Boar {
    @Getter
    private static final Boar instance = new Boar();
    private Boar() {}

    private BoarPlayerManager playerManager;

    public void init() {
        this.playerManager = new BoarPlayerManager();
        new GeyserSessionJoinEvent();
    }

    public void terminate() {
        this.playerManager.clear();
    }
}
