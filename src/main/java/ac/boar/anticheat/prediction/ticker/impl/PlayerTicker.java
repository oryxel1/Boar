package ac.boar.anticheat.prediction.ticker.impl;

import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.data.Pose;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import com.google.common.collect.ImmutableMap;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Fluid;

import java.util.Map;

public class PlayerTicker extends LivingTicker {
    public static final Map<Pose, EntityDimensions> POSES = ImmutableMap.<Pose, EntityDimensions>builder().
            put(Pose.STANDING, EntityDimensions.changing(0.6f, 1.8f).withEyeHeight(1.62f)).put(Pose.SLEEPING,
                    EntityDimensions.fixed(0.2f, 0.2f).withEyeHeight(0.2f)).
            put(Pose.GLIDING, EntityDimensions.changing(0.6f, 0.6f).withEyeHeight(0.4f)).put(Pose.SWIMMING,
                    EntityDimensions.changing(0.6f, 0.6f).withEyeHeight(0.4f)).put(Pose.SPIN_ATTACK, EntityDimensions.changing(0.6f, 0.6f)
                    .withEyeHeight(0.4f)).put(Pose.CROUCHING, EntityDimensions.changing(0.6f, 1.5f).withEyeHeight(1.27f)).put(Pose.DYING,
                    EntityDimensions.fixed(0.2f, 0.2f).withEyeHeight(1.62f)).build();

    public PlayerTicker(BoarPlayer player) {
        super(player);
    }

    @Override
    public void tick() {
        super.tick();
        this.updatePlayerPose();
    }

    @Override
    protected void travel() {
        if (player.swimming) {
            float d = MathUtil.getRotationVector(player.pitch, player.yaw).y;
            float e = d < -0.2 ? 0.085F : 0.06F;
            final FluidState state = player.compensatedWorld.getFluidState(player.position.add(0, 1.0F - 0.1F, 0).toVector3i());
            if (d <= 0.0 || player.getInputData().contains(PlayerAuthInputData.JUMPING) || state.fluid() != Fluid.EMPTY) {
                player.velocity = player.velocity.add(0, (d - player.velocity.y) * e, 0);
            }
        }
        super.travel();
    }

    private void updatePlayerPose() {
        if (!this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
            return;
        }
        Pose pose = this.getDesiredPose();
        player.pose = /* this.isSpectator() || this.isPassenger() || */ this.canPlayerFitWithinBlocksAndEntitiesWhen(pose) ? pose : (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING) ? Pose.CROUCHING : Pose.SWIMMING);
    }

    private Pose getDesiredPose() {
//        if (this.isSleeping()) {
//            return Pose.SLEEPING;
//        }
        if (player.swimming) {
            return Pose.SWIMMING;
        }
        if (player.gliding) {
            return Pose.GLIDING;
        }
//        if (this.isAutoSpinAttack()) {
//            return Pose.SPIN_ATTACK;
//        }
        if (player.sneaking/* && !this.abilities.flying */) {
            return Pose.CROUCHING;
        }
        return Pose.STANDING;
    }

    protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose) {
        return player.compensatedWorld.noCollision(player.getDimensions(pose).getBoxAt(player.position).contract(1.0E-7F));
    }
}