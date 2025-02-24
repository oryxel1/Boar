package ac.boar.anticheat.prediction.ticker.base;

import ac.boar.anticheat.collision.Collision;
import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.data.PredictionData;
import ac.boar.anticheat.data.VelocityData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.prediction.engine.data.VectorType;
import ac.boar.anticheat.prediction.engine.impl.PredictionEngineElytra;
import ac.boar.anticheat.prediction.engine.impl.PredictionEngineLava;
import ac.boar.anticheat.prediction.engine.impl.PredictionEngineNormal;
import ac.boar.anticheat.prediction.engine.impl.PredictionEngineWater;
import ac.boar.anticheat.util.BlockUtil;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.geysermc.geyser.level.block.type.BlockState;

import java.util.Iterator;
import java.util.Map;

public class LivingTicker extends EntityTicker {
    public LivingTicker(BoarPlayer player) {
        super(player);
    }

    @Override
    public void tick() {
        super.tick();
        tickMovement();

        // Update bounding box based off our calculated movement, don't trust player position value.
        // This patches some bypasses abuses bounding box, also this is a workaround for floating point errors breaking collision.
        // Well since we're not predicting anything when player flying, this is going to be inaccurate as soon as we start flying.
        // Depends on how bad is the floating point errors.
        player.updateBoundingBox(player.predictedPosition);

        this.tickBlockCollision();
    }

    public void tickMovement() {
        player.movementInput = player.movementInput.multiply(0.98F);

        if (player.movementInput.x != 0 && player.movementInput.z != 0) {
            player.movementInput = player.movementInput.multiply(0.70710677F);
        }

        this.travel();
//        if (!this.getWorld().isClient() || this.isLogicalSideForUpdatingMovement()) {
//            this.tickBlockCollision();
//        }

    }

    public void travel() {
        final PredictionEngine engine;
        if (player.touchingWater || player.isInLava()) {
            if (player.touchingWater) {
                engine = new PredictionEngineWater(player);
            } else {
                engine = new PredictionEngineLava(player);
            }
        } else if (player.gliding) {
            engine = new PredictionEngineElytra(player);
        } else {
            engine = new PredictionEngineNormal(player);
        }

        player.engine = engine;

        // Can't just check for flying since player movement can also act weirdly when have may_fly ability.
        if (player.abilities.contains(Ability.MAY_FLY) || player.flying || player.wasFlying) {
            player.prevEotVelocity = player.eotVelocity;
            player.eotVelocity = player.claimedEOT;
            player.predictedPosition = player.position;
            return;
        }

        final boolean isThereMovementMultiplier = player.movementMultiplier.lengthSquared() > 1.0E-7;

        double closetOffset = Double.MAX_VALUE;
        Vec3 beforeCollision = Vec3.ZERO, afterCollision = Vec3.ZERO;
        for (final Vector vector : engine.gatherAllPossibilities()) {
            Vec3 movement = vector.getVelocity();
            if (isThereMovementMultiplier) {
                movement = movement.multiply(player.movementMultiplier);
            }

            movement = Collision.adjustMovementForSneaking(player, movement);
            final Vec3 lv2 = Collision.adjustMovementForCollisions(player, movement, true);
            final double offset = player.prevPosition.add(lv2).distanceTo(player.position);
            if (offset < closetOffset) {
                closetOffset = offset;
                beforeCollision = movement;
                afterCollision = lv2;
                player.closetVector = vector;
            }

            if (vector.getType() == VectorType.VELOCITY) {
                player.postPredictionVelocities.put(vector.getTransactionId(), new PredictionData(vector, movement, lv2));
            }
        }

        player.predictedData = new PredictionData(player.closetVector, beforeCollision, afterCollision);
        boolean bl = beforeCollision.x != afterCollision.x;
        boolean bl2 = beforeCollision.z != afterCollision.z;
        player.horizontalCollision = bl || bl2;

        player.verticalCollision = beforeCollision.y != afterCollision.y;
        player.wasGround = player.onGround;
        player.onGround = player.verticalCollision && beforeCollision.y < 0;

        Vec3 eotVelocity = beforeCollision.clone();
        if (isThereMovementMultiplier) {
            player.movementMultiplier = eotVelocity = Vec3.ZERO;
        }

        if (player.horizontalCollision) {
            eotVelocity = new Vec3(bl ? 0 : eotVelocity.x, eotVelocity.y, bl2 ? 0 : eotVelocity.z);
        }

        if (player.verticalCollision) {
            final Vector3i lv = player.getPosWithYOffset(false, 0.2F);
            final BlockState lv2 = player.compensatedWorld.getBlockState(lv);
            BlockUtil.onEntityLand(true, player, eotVelocity, lv2);
        }

        float f = player.getVelocityMultiplier();
        eotVelocity = eotVelocity.multiply(f, 1, f);
        player.eotVelocity = engine.applyEndOfTick(eotVelocity);

        if (player.closetVector.getType() == VectorType.VELOCITY) {
            Iterator<Map.Entry<Long, VelocityData>> iterator = player.queuedVelocities.entrySet().iterator();

            Map.Entry<Long, VelocityData> entry;
            while (iterator.hasNext() && (entry = iterator.next()) != null) {
                if (entry.getKey() > player.closetVector.getTransactionId()) {
                    break;
                } else {
                    iterator.remove();
                }
            }
        }

        player.predictedPosition = player.prevPosition.add(afterCollision);
    }
}
