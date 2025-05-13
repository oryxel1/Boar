package ac.boar.anticheat.check.api;

import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.annotations.Experimental;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.ChatUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Check {
    protected final BoarPlayer player;

    private final String name = getClass().getDeclaredAnnotation(CheckInfo.class).name(),
            type = getClass().getDeclaredAnnotation(CheckInfo.class).type();
    private final int maxVl = getClass().getDeclaredAnnotation(CheckInfo.class).maxVl();
    private final boolean experimental = getClass().getDeclaredAnnotation(Experimental.class) != null;
    private int vl = 0;

    public void fail() {
        fail("");
    }

    public void fail(String verbose) {
        this.vl++;

        final String msg = "§3" + player.getSession().getPlayerEntity().getDisplayName() + "§7 failed §6" + name + "(" + type + ") " +
                (experimental ? "§2(Experimental) " : "") + "§7x" + vl + " " + verbose;
        ChatUtil.alert(player, msg);
    }
}
