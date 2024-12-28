package ac.boar.anticheat.util;

import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import static org.geysermc.mcprotocollib.protocol.data.game.entity.Effect.*;

public class EntityUtil {
    public static Effect toJavaEffect(int id) {
        return switch (id) {
            case 0 -> null;
            case 24 -> LEVITATION;
            case 26 -> CONDUIT_POWER;
            case 27 -> SLOW_FALLING;
            case 28 -> BAD_OMEN;
            case 29 -> HERO_OF_THE_VILLAGE;
            case 30 -> DARKNESS;
            case 31 -> TRIAL_OMEN;
            case 32 -> WIND_CHARGED;
            case 33 -> WEAVING;
            case 34 -> OOZING;
            case 35 -> INFESTED;
            case 36 -> RAID_OMEN;
            default -> Effect.from(id - 1);
        };
    }
}