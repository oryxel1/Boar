package ac.boar.anticheat.prediction.ticker.impl;

import ac.boar.anticheat.compensated.entity.impl.HorseEntityCache;
import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.math.TrigMath;

public class HorseTicker extends LivingTicker {
    public HorseTicker(BoarPlayer player) {
        super(player);
    }

    @Override
    public void applyInput() {
    }

    @Override
    protected void tickRidden() {
        HorseEntityCache entity = (HorseEntityCache) player.vehicle;

        if (player.onGround) {
            if (entity.playerJumpPendingScale > 0.0F) {
                player.velocity.y = player.getJumpPower() * entity.playerJumpPendingScale;
                if (player.input.z > 0.0) {
                    float sin = TrigMath.sin(player.yaw * 0.017453292F);
                    float cos = TrigMath.cos(player.yaw * 0.017453292F);
                    player.velocity = player.velocity.add(-0.4F * sin * entity.playerJumpPendingScale, 0, 0.4F * cos * entity.playerJumpPendingScale);
                }
            }

            entity.playerJumpPendingScale = 0.0F;
        }
    }
}
