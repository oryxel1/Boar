package ac.boar.anticheat.prediction.ticker.impl;

import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.level.block.Fluid;

public class PlayerTicker extends LivingTicker {
    public PlayerTicker(BoarPlayer player) {
        super(player);
    }

    @Override
    public void applyInput() {
        super.applyInput();
        boolean sneaking = player.getFlagTracker().has(EntityFlag.SNEAKING) || player.getInputData().contains(PlayerAuthInputData.STOP_SNEAKING);
        if ((sneaking || player.getFlagTracker().has(EntityFlag.CRAWLING)) && !player.getFlagTracker().has(EntityFlag.GLIDING) && !player.isInLava() && !player.touchingWater) {
            player.input = player.input.multiply(0.3F);
        }

        if (player.getFlagTracker().has(EntityFlag.USING_ITEM)) {
            player.input = player.input.multiply(0.122499995F);
        }
    }

    @Override
    public void aiStep() {
        if (player.touchingWater && player.getInputData().contains(PlayerAuthInputData.SNEAKING) /*&& this.isAffectedByFluids()*/) {
            player.velocity.y -= 0.04F;
        }

        super.aiStep();
    }

    @Override
    protected void travel() {
        if (player.getFlagTracker().has(EntityFlag.SWIMMING)) {
            float d = MathUtil.getRotationVector(player.pitch, player.yaw).y;
            float e = d < -0.2 ? 0.085F : 0.06F;
            final FluidState state = player.compensatedWorld.getFluidState(player.position.toVector3i());
            if ((d <= 0.0 || state.fluid() != Fluid.EMPTY) && !player.getInputData().contains(PlayerAuthInputData.JUMPING)) {
                player.velocity = player.velocity.add(0, (d - player.velocity.y) * e, 0);
            }
        }
        super.travel();
    }
}