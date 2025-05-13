package ac.boar.anticheat.util;

import ac.boar.anticheat.player.BoarPlayer;

public class ChatUtil {
    public final static String PREFIX = "§3Boar §7>§r ";

    public static void alert(BoarPlayer player, Object message) {
        if (!player.isAlertEnabled()) {
            return;
        }

        player.getSession().sendMessage(ChatUtil.PREFIX + "§3" + message);
    }
}