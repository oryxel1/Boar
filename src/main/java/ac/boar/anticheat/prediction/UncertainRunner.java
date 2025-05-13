package ac.boar.anticheat.prediction;

import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;

// Used to have some stuff here but now that I figured stuff out uhh this is useless, for now.
@RequiredArgsConstructor
public class UncertainRunner {
    private final BoarPlayer player;

    public void doTickEndUncertain() {
    }

    public float extraOffset() {
        if (player.thisTickSpinAttack) {
            return 0.007F;
        }

        return 0;
    }
}
