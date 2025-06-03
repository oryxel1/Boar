package ac.boar.anticheat.prediction;

import ac.boar.anticheat.collision.Collider;
import ac.boar.anticheat.data.input.PredictionData;
import ac.boar.anticheat.data.input.VelocityData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.prediction.engine.data.VectorType;
import ac.boar.anticheat.prediction.engine.impl.GlidingPredictionEngine;
import ac.boar.anticheat.prediction.engine.impl.GroundAndAirPredictionEngine;
import ac.boar.anticheat.prediction.engine.impl.fluid.LavaPredictionEngine;
import ac.boar.anticheat.prediction.engine.impl.fluid.WaterPredictionEngine;
import ac.boar.anticheat.prediction.ticker.impl.PlayerTicker;
import ac.boar.anticheat.util.math.Vec3;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.level.block.Fluid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class PredictionRunner {
    private final BoarPlayer player;

    public void run(long tick) {
        if (!this.findBestTickStartVelocity()) {
            return;
        }

        player.hasLeastRunPredictionOnce = true;

        new PlayerTicker(player).tick();
        player.predictionResult = new PredictionData(player.bestPossibility, player.beforeCollision.clone(), player.afterCollision.clone(), player.velocity.clone());
        player.getTeleportUtil().cachePosition(tick, player.position.add(0, player.getYOffset(), 0).toVector3f());
    }

    private boolean findBestTickStartVelocity() {
        final PredictionEngine engine;

        if (player.touchingWater) {
            engine = new WaterPredictionEngine(player);
        } else if (player.isInLava()) {
            engine = new LavaPredictionEngine(player);
        } else if (player.getFlagTracker().has(EntityFlag.GLIDING)) {
            engine = new GlidingPredictionEngine(player);
        } else {
            engine = new GroundAndAirPredictionEngine(player);
        }

        final List<Vector> possibleVelocities = new ArrayList<>();

        boolean forceVelocity = false;

        VelocityData forcedVelocity = null;
        for (final VelocityData data : player.queuedVelocities.values()) {
            if (data.stackId() > player.receivedStackId.get()) {
                break;
            }

            forcedVelocity = data;
            forceVelocity = true;
        }

        // TODO: Figure out if old velocity affect rewind or not.
        if (forceVelocity) {
            // Player already accepted the second latency stack, player HAVE to accept this velocity.
            possibleVelocities.add(new Vector(VectorType.VELOCITY, forcedVelocity.velocity(), forcedVelocity.stackId()));
        } else {
            possibleVelocities.add(new Vector(VectorType.NORMAL, player.velocity.clone()));

            // Find the nearest velocity that player already accept the first latency stack.
            VelocityData nearestVelocity = null;
            for (final VelocityData data : player.queuedVelocities.values()) {
                if ((data.stackId() - 1) > player.receivedStackId.get()) {
                    break;
                }

                // This should only be ONE result, player cannot accept 2 velocity at once since velocity is wrapped between 2 latency stack.
                nearestVelocity = data;
            }

            if (nearestVelocity != null) {
                possibleVelocities.add(new Vector(VectorType.VELOCITY, nearestVelocity.velocity(), nearestVelocity.stackId()));
                System.out.println("nearest velocity!");
            }
        }

        float closetDistance = Float.MAX_VALUE;

        if (possibleVelocities.size() == 1) {
            // There is only one possibility, no need to bruteforce or anything.
            player.bestPossibility = possibleVelocities.get(0);
        } else {
            final Vec3 oldInput = player.input.clone();

            // Mini prediction engine!
            for (Vector possibility : possibleVelocities) {
                Vec3 vec3 = possibility.getVelocity().clone();

                boolean jumping = player.getInputData().contains(PlayerAuthInputData.JUMPING) || player.getInputData().contains(PlayerAuthInputData.WANT_UP) ||
                        player.getInputData().contains(PlayerAuthInputData.START_JUMPING);
                if (jumping) {
                    float g = player.isInLava() ? player.getFluidHeight(Fluid.LAVA) : player.getFluidHeight(Fluid.WATER);
                    if (g != 0) {
                        vec3 = vec3.add(0, 0.04F, 0);
                    } else if (player.onGround && player.getInputData().contains(PlayerAuthInputData.START_JUMPING)) {
                        vec3 = player.jumpFromGround(vec3);
                    }
                }

                vec3 = engine.travel(vec3);

                if (player.stuckSpeedMultiplier.lengthSquared() > 1.0E-7) {
                    vec3 = vec3.multiply(player.stuckSpeedMultiplier);
                }

                vec3 = Collider.collide(player, Collider.maybeBackOffFromEdge(player, vec3));

                float distance = player.position.add(vec3).squaredDistanceTo(player.unvalidatedPosition);

                // Do <= to priority velocity over normal last tick in case if both have the same velocity result.
                if (distance <= closetDistance) {
                    closetDistance = distance;
                    player.bestPossibility = possibility;
                }
            }

            player.input = oldInput;
        }

        if (player.bestPossibility == null) {
            return false;
        }

        // We can start the ACTUAL prediction now.
        player.velocity = player.bestPossibility.getVelocity();

        // Also clear out old velocity.
        if (player.bestPossibility.getType() == VectorType.VELOCITY) {
            Iterator<Map.Entry<Long, VelocityData>> iterator = player.queuedVelocities.entrySet().iterator();

            Map.Entry<Long, VelocityData> entry;
            while (iterator.hasNext() && (entry = iterator.next()) != null) {
                if (entry.getKey() > player.bestPossibility.getStackId()) {
                    break;
                } else {
                    iterator.remove();
                }
            }
        }
        return true;
    }
}