package ac.boar.anticheat.data.vanilla;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

@AllArgsConstructor
@Getter
public class StatusEffect {
    private final Effect effect;
    private final int amplifier;
    private int duration;

    public void tick() {
        if (duration > 0) {
            duration--;
        }
    }
}