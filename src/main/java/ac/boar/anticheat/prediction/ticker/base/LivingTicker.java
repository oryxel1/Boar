package ac.boar.anticheat.prediction.ticker.base;

import ac.boar.anticheat.collision.Collision;
import ac.boar.anticheat.data.PredictionData;
import ac.boar.anticheat.data.VelocityData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.prediction.engine.data.VectorType;
import ac.boar.anticheat.prediction.engine.impl.*;
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
    }

    public void tickMovement() {
        player.input = player.input.multiply(0.98F);

        this.travel();
        this.tickBlockCollision();
    }

    public void travel() {
        if (player.abilities.contains(Ability.MAY_FLY) || player.flying || player.wasFlying) {
            player.velocity = player.unvalidatedTickEnd.clone();
            player.setPos(player.unvalidatedPosition);
            return;
        }

        final PredictionEngine engine;
        if (player.touchingWater || player.isInLava()) {
            if (player.touchingWater) {
                engine = new PredictionEngineWater(player);
            } else {
                engine = new PredictionEngineLava(player);
            }
        } else if (player.gliding) {
            engine = new PredictionEngineGliding(player);
        } else {
            engine = new PredictionEngineNormal(player);
        }

        final boolean isThereStuckSpeed = player.stuckSpeedMultiplier.lengthSquared() > 1.0E-7;

        double closetOffset = Double.MAX_VALUE;
        Vec3 velocity = Vec3.ZERO, collided = Vec3.ZERO;
        for (final Vector vector : engine.gatherAllPossibilities()) {
            Vec3 movement = vector.getVelocity();
            if (isThereStuckSpeed) {
                movement = movement.multiply(player.stuckSpeedMultiplier);
            }

            movement = Collision.maybeBackOffFromEdge(player, movement);
            final Vec3 lv2 = Collision.collide(player, movement, true);
            final double offset = player.position.add(lv2).distanceTo(player.position);
            if (offset < closetOffset) {
                closetOffset = offset;
                velocity = movement;
                collided = lv2;
                player.closetVector = vector;
            }
        }

        player.velocity = velocity.clone();

        // Well we're going to use player sent last position instead of our own since if we use ours then it will lose precision.
        player.setPos(player.prevUnvalidatedPosition.add(collided));

        boolean bl = velocity.x != collided.x;
        boolean bl2 = velocity.z != collided.z;
        player.horizontalCollision = bl || bl2;

        player.verticalCollision = velocity.y != collided.y;
        player.groundCollision = player.verticalCollision && velocity.y < 0;

        if (isThereStuckSpeed) {
            player.stuckSpeedMultiplier = Vec3.ZERO;
            player.velocity = Vec3.ZERO.clone();
        }

        if (player.horizontalCollision) {
            player.velocity = new Vec3(bl ? 0 : player.velocity.x, player.velocity.y, bl2 ? 0 : player.velocity.z);
        }

        if (player.verticalCollision) {
            final Vector3i lv = player.getOnPos(0.2F);
            final BlockState lv2 = player.compensatedWorld.getBlockState(lv);
            BlockUtil.onEntityLand(true, player, lv2);
        }

        engine.finalizeMovement();
        player.predictionResult = new PredictionData(player.closetVector, velocity, collided, player.velocity.clone());

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
    }
}
