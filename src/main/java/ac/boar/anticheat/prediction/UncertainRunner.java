package ac.boar.anticheat.prediction;

import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UncertainRunner {
    private final BoarPlayer player;

    public void doTickEndUncertain() {
        // For whatever the fuck the reason is, slime on bedrock is acting weird as SHIT, according to BDS it acts the same as on Java
        // expect the fact that it also multiply on the y-axis, but no the tick end that I debug is completely different, whatever
        // just allow the player decide if their velocity is slower than our predicted one.
        // Maybe because the last BDS version with uhhh PDB file that I use is pretty outdated? eh whatever, TODO: Figure this out.

        // Oh btw, bouncing still act the same so we don't have to worry cheater abuse this to go to the sun lol.
        boolean slimeSmallerXZ = player.unvalidatedTickEnd.horizontalLengthSquared() < player.velocity.horizontalLengthSquared();
        boolean slimeSmallerOrEqualsY = player.unvalidatedTickEnd.y <= player.velocity.y;
        if (slimeSmallerXZ && slimeSmallerOrEqualsY && player.thisTickSlimeUncertain) {
            player.velocity = player.unvalidatedTickEnd.clone();

            // Hardcoded.
            player.velocity.y = Math.max(-0.6F, player.velocity.y);
        }
    }

    public float reduceOffset(float offset) {
        return offset;
    }
}
