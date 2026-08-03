package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.check.api.BaseCheck;
import ac.boar.anticheat.check.api.Check;
import ac.boar.api.anticheat.annotations.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.data.VectorType;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;

import java.util.HashMap;
import java.util.Map;

@CheckInfo(name = "Prediction")
public class Prediction extends BaseCheck implements OffsetHandlerCheck {
    private final Map<String, Check> checks = new HashMap<>();

    public Prediction(BoarPlayer player) {
        super(player);

        this.checks.put("Phase", new BaseCheck(player, "Phase", "", false));
        this.checks.put("Velocity", new BaseCheck(player, "Velocity", "", false));

        this.checks.put("Strafe", new BaseCheck(player, "Strafe", "", false));
        this.checks.put("Speed", new BaseCheck(player, "Speed", "", false));
        this.checks.put("Flight", new BaseCheck(player, "Flight", "", false));

        this.checks.put("Collisions", new BaseCheck(player, "Collisions", "", false));
    }

    @Override
    public void onPredictionComplete(float posDiff) {
        if (player.tick < 10 || !this.shouldDoFail()) {
            return;
        }
        if (posDiff < player.getPosAcceptanceThreshold()) {
            player.position = player.unvalidatedPosition.clone();
            return;
        }

        Boar.debug("[movement-debug] prediction posDiff tick=" + player.tick + " posDiff=" + posDiff + " acceptance/max=" + player.getPosAcceptanceThreshold() + " alert=" + Boar.getConfig().alertThreshold() + " type=" + player.bestPossibility.getType() + " predictedPos=" + player.position + " actualPos=" + player.unvalidatedPosition + " predictedDelta=" + player.velocity + " actualDelta=" + player.unvalidatedTickEnd, Boar.DebugMessage.WARNING);
        boolean isPosDiffExcessive = posDiff >= Boar.getConfig().alertThreshold();
        if (!player.disableMitigations()) {
            if (!isPosDiffExcessive) {
                this.driftTowardsClient();
            } else {
                Boar.debug("[movement-debug] correction reason=prediction-soft tick=" + player.tick + " posDiff=" + posDiff, Boar.DebugMessage.WARNING);
                player.getTeleportUtil().correct();
            }
            return;
        } else if (!isPosDiffExcessive) {
            // Mitigations are disabled and pos difference is above acceptance threshold but isn't above flagging threshold
            return;
        }

        Boar.debug("[movement-debug] correction reason=prediction-fail tick=" + player.tick + " posDiff=" + posDiff, Boar.DebugMessage.WARNING);
        player.getTeleportUtil().correct();

        boolean claimedHorizontal = player.getInputData().contains(PlayerAuthInputData.HORIZONTAL_COLLISION);
        boolean claimedVertical = player.getInputData().contains(PlayerAuthInputData.VERTICAL_COLLISION);
        if (claimedVertical != player.verticalCollision || claimedHorizontal != player.horizontalCollision) {
            fail("Phase", "o: " + posDiff + ", expect: (" + player.horizontalCollision + "," + player.verticalCollision + "), actual: (" + claimedHorizontal + "," + claimedVertical + ")");
        }

        if (player.bestPossibility.getType() == VectorType.VELOCITY) {
            fail("Velocity", "o: " + posDiff);
            return;
        }

        if (player.unvalidatedTickEnd.distanceTo(player.velocity) < player.getPosAcceptanceThreshold()) {
            fail("Collisions", "o: " + posDiff);
        }

        Vec3 actual = player.unvalidatedPosition.subtract(player.prevUnvalidatedPosition);
        Vec3 predicted = player.position.subtract(player.prevUnvalidatedPosition);
        if (!MathUtil.sameDirectionHorizontal(actual, predicted)) {
            fail("Strafe", "o: " + posDiff + ", expected direction: " + MathUtil.signAll(predicted).horizontalToString() + ", actual direction: " + MathUtil.signAll(actual).horizontalToString());
        }

        float squaredActual = actual.horizontalLengthSquared(), squaredPredicted = predicted.horizontalLengthSquared();
        if (actual.horizontalLengthSquared() > predicted.horizontalLengthSquared()) {
            fail("Speed", "o: " + posDiff + ", expected: " + squaredPredicted + ", actual: " + squaredActual);
        }

        if (Math.abs(player.position.y - player.unvalidatedPosition.y) > player.getPosAcceptanceThreshold()) {
            fail("Flight", "o: " + posDiff);
        }
    }

    /**
     * Moves the server position a small step towards the client position by a small amount.
     *
     * <p>The client and the server can stay out of sync by a small amount for many ticks if the correction threshold set is high enough.
     * The difference can grow until Boar must send a correction, which the player sees as a sudden rubberband. This
     * drift removes the difference slowly, so no correction is necessary.
     *
     * <p>The drift applies only to the X and Z axes. The Y axis keeps the value that Boar predicts, because a vertical
     * drift can help assist with flight cheats.
     */
    private void driftTowardsClient() {
        final float maxDrift = Boar.getConfig().positionDriftAmount();
        if (maxDrift <= 0) {
            return;
        }

        final Vec3 diff = player.unvalidatedPosition.subtract(player.position);
        player.setPos(player.position.add(
                MathUtil.clamp(diff.x, -maxDrift, maxDrift),
                0,
                MathUtil.clamp(diff.z, -maxDrift, maxDrift)
        ), false);

        Boar.debug("[movement-debug] drifted server position tick=" + player.tick + " newPos=" + player.position + " remaining=" + player.position.subtract(player.unvalidatedPosition), Boar.DebugMessage.INFO);
    }

    public boolean shouldDoFail() {
        return this.canFlagMovement();
    }

    private boolean canFlagMovement() {
        return player.tickSinceBlockResync <= 0
                && !player.insideUnloadedChunk
                && !player.getTeleportUtil().isTeleporting()
                && !player.getTeleportUtil().hasPendingCorrection()
                && !player.getTeleportUtil().isCorrectionCooldown()
                && !player.isMovementExempted()
                && !player.inLoadingScreen
                && player.sinceLoadingScreen > 5
                && player.compensatedWorld.isChunkLoadedAt(player.position.x, player.position.z)
                && player.compensatedWorld.isChunkLoadedAt(player.unvalidatedPosition.x, player.unvalidatedPosition.z);
    }

    public void fail(String name, String verbose) {
        if (Boar.getConfig().disabledChecks().contains(name)) {
            return;
        }

        this.checks.get(name).fail(verbose);
    }
}
