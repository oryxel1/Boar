package ac.boar.anticheat.check.api;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.annotations.Experimental;
import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Check {
    protected final BoarPlayer player;

    private final String name = getClass().getDeclaredAnnotation(CheckInfo.class).name(),
            type = getClass().getDeclaredAnnotation(CheckInfo.class).type();
    private final boolean experimental = getClass().getDeclaredAnnotation(Experimental.class) != null;
    private int vl = 0;

    public void fail() {
        fail("");
    }

    public void fail(String verbose) {
        this.vl++;

        final StringBuilder builder = new StringBuilder("§3" + player.getSession().getPlayerEntity().getDisplayName() + "§7 failed§6 " + name);
        if (!this.type.isBlank()) {
            builder.append("(").append(type).append(")");
        }

        if (this.experimental) {
            builder.append(" §2(Experimental)");
        }

        builder.append(" §7x").append(vl).append(" ").append(verbose);
        Boar.getInstance().getAlertManager().alert(builder.toString());
    }
}
