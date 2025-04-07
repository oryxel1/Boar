package ac.boar.anticheat.prediction.ticker.impl;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.prediction.engine.impl.GlidingPredictionEngine;
import ac.boar.anticheat.prediction.engine.impl.GroundAndAirPredictionEngine;
import ac.boar.anticheat.prediction.engine.impl.fluid.LavaPredictionEngine;
import ac.boar.anticheat.prediction.engine.impl.fluid.WaterPredictionEngine;
import ac.boar.anticheat.prediction.ticker.base.EntityTicker;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.level.block.Fluid;

public class LivingTicker extends EntityTicker {
    public LivingTicker(BoarPlayer player) {
        super(player);
    }

    @Override
    public void tick() {
        super.tick();
        this.aiStep();
    }

    public void aiStep() {
        // Note: There is no 0.003 movement limiter on Bedrock, I think.
        // But there seems to be one for extremely small movement.
        player.velocity.x = Math.abs(player.velocity.x) < 1.0E-8 ? 0 : player.velocity.x;
        player.velocity.y = Math.abs(player.velocity.y) < 1.0E-8 ? 0 : player.velocity.y;
        player.velocity.z = Math.abs(player.velocity.z) < 1.0E-8 ? 0 : player.velocity.z;

        player.input = player.input.multiply(0.98F);

        boolean jumping = player.getInputData().contains(PlayerAuthInputData.JUMPING) || player.getInputData().contains(PlayerAuthInputData.WANT_UP) ||
                player.getInputData().contains(PlayerAuthInputData.START_JUMPING);
        if (jumping /*&& this.isAffectedByFluids() */) {
            float g = player.isInLava() ? player.getFluidHeight(Fluid.LAVA) : player.getFluidHeight(Fluid.WATER);
            boolean bl = player.touchingWater && g > 0.0;
            float h = player.getFluidJumpThreshold();
            if (bl && (!player.onGround || g > h)) {
                player.velocity = player.velocity.add(0, 0.04F, 0);
            } else if (player.isInLava() && (!player.onGround || g > h)) {
                player.velocity = player.velocity.add(0, 0.04F, 0);
            } else if ((player.onGround || bl && g <= h) && player.getInputData().contains(PlayerAuthInputData.START_JUMPING)) {
                player.velocity = player.jumpFromGround(player.velocity);
            }
        }

        this.travelRidden();

//        if (this.autoSpinAttackTicks > 0) {
//            --this.autoSpinAttackTicks;
//            this.checkAutoSpinAttack(aABB, this.getBoundingBox());
//        }
//
//        if (this.autoSpinAttackTicks > 0) {
//            --this.autoSpinAttackTicks;
//            this.checkAutoSpinAttack(aABB, this.getBoundingBox());
//        }
    }

    protected void travelRidden() {
//        Vec3 vec32 = this.getRiddenInput(player, vec3);
//        this.tickRidden(player, vec32);
//        if (this.canSimulateMovement()) {
//            this.setSpeed(this.getRiddenSpeed(player));
//            this.travel(vec32);
//        } else {
//            this.setDeltaMovement(Vec3.ZERO);
//        }

        this.travel();
    }

    protected void travel() {
        if (player.isInLava() || player.touchingWater) {
            this.travelInFluid();
        } else if (player.gliding) {
//            if (this.onClimbable()) {
//                this.travelInAir(vec3);
//                this.stopFallFlying();
//                return;
//            }

            player.velocity = new GlidingPredictionEngine(player).travel(player.velocity);
            this.doSelfMove(player.velocity.clone()); // this.move(MoverType.SELF, this.getDeltaMovement());
        } else {
            travelInAir();
        }
    }

    private void travelInAir() {
        final PredictionEngine engine = new GroundAndAirPredictionEngine(player);
        player.velocity = engine.travel(player.velocity);
        this.doSelfMove(player.velocity.clone()); // this.move(MoverType.SELF, this.getDeltaMovement());
        engine.finalizeMovement();
    }

    private void travelInFluid() {
        float d = player.position.y;
        final PredictionEngine engine;
        if (player.touchingWater) {
            engine = new WaterPredictionEngine(player);
        } else {
            engine = new LavaPredictionEngine(player);
        }
        player.velocity = engine.travel(player.velocity);
        this.doSelfMove(player.velocity.clone());
        engine.finalizeMovement();

        Vec3 vec33 = player.velocity;
        if (player.horizontalCollision && player.doesNotCollide(vec33.x, vec33.y + 0.6f - player.position.y + d, vec33.z)) {
            player.velocity.y = 0.3F;
        }
    }
}