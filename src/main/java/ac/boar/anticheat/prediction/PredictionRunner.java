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
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Vec3;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PredictionRunner {
    private final BoarPlayer player;

    public void run() {
        if (!this.findBestTickStartVelocity()) {
            return;
        }

        new PlayerTicker(player).tick();
        player.predictionResult = new PredictionData(player.beforeCollision.clone(), player.afterCollision.clone(), player.velocity.clone());
        player.lastTickFinalVelocity = player.velocity.clone();
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

            // So here is the thing, this implementation is wrong, this could false in cases where the player
            // velocity and tick end result in the same motion. Now this is actually quite easy to check for, but
            // I'm too lazy to check for that now sooooo, ignore for now, if this were to be implemented, just allow
            // velocity to be taken twice if the velocity have the same offset as tick end, not actually an advantage.

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
                // System.out.println("nearest velocity!");
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

                vec3 = player.jump(vec3);
                vec3 = engine.travel(vec3);

                if (player.stuckSpeedMultiplier.lengthSquared() > 1.0E-7) {
                    vec3 = vec3.multiply(player.stuckSpeedMultiplier);
                }

                Vec3 vec32 = Collider.collide(player, Collider.maybeBackOffFromEdge(player, vec3));
                boolean horizontal = !MathUtil.equal(vec3.x, vec32.x) || !MathUtil.equal(vec3.z, vec32.z);
                boolean vertical = vec3.y != vec32.y;

                float distance = player.position.add(vec32).squaredDistanceTo(player.unvalidatedPosition);

                // Factor in vertical and horizontal collision, helps with cases where the travelled vel is the same.
                if (horizontal != player.getInputData().contains(PlayerAuthInputData.HORIZONTAL_COLLISION)) {
                    distance += 1.0E-6f;
                }
                if (vertical != player.getInputData().contains(PlayerAuthInputData.VERTICAL_COLLISION)) {
                    distance += 1.0E-6f;
                }

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
        return true;
    }
}