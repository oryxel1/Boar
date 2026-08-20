package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.check.api.BaseCheck;
import ac.boar.anticheat.check.api.Check;
import ac.boar.api.anticheat.annotations.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;

@CheckInfo(name = "Prediction")
public class Prediction extends BaseCheck implements OffsetHandlerCheck {
    private final Check correction;

    private long lastFlagTick = Long.MIN_VALUE;
    private int suppressedFails;
    private float suppressedMaxPosDiff;

    public Prediction(BoarPlayer player) {
        super(player);

        this.correction = new BaseCheck(player, "MovementCorrection", "", false);
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
        if (posDiff < Boar.getConfig().alertThreshold()) {
            // The difference is above the acceptance threshold but below the alert threshold.
            if (!player.disableMitigations()) {
                this.driftTowardsClient();
            }
            return;
        }

        Boar.debug("[movement-debug] correction reason=prediction-fail tick=" + player.tick + " posDiff=" + posDiff, Boar.DebugMessage.WARNING);

        // Dump the retained movement trace so the failure can be re-created and inspected.
        final boolean claimedHorizontal = player.getInputData().contains(PlayerAuthInputData.HORIZONTAL_COLLISION);
        final boolean claimedVertical = player.getInputData().contains(PlayerAuthInputData.VERTICAL_COLLISION);
        final String failureInfo = "prediction failure tick=" + player.tick
                + " posDiff=" + posDiff + " alertThreshold=" + Boar.getConfig().alertThreshold()
                + " acceptance=" + player.getPosAcceptanceThreshold()
                + " type=" + player.bestPossibility.getType()
                + " predictedPos=" + player.position + " actualPos=" + player.unvalidatedPosition
                + " predictedDelta=" + player.velocity + " actualDelta=" + player.unvalidatedTickEnd
                + " serverCollision=(h=" + player.horizontalCollision + ",v=" + player.verticalCollision + ")"
                + " claimedCollision=(h=" + claimedHorizontal + ",v=" + claimedVertical + ")";

        player.getTeleportUtil().correct();

        final int cooldown = Boar.getConfig().correctionFlagCooldownTicks();
        if (this.lastFlagTick != Long.MIN_VALUE && player.tick - this.lastFlagTick < cooldown) {
            this.suppressedFails++;
            if (posDiff > this.suppressedMaxPosDiff) {
                this.suppressedMaxPosDiff = posDiff;
            }
            Boar.debug("[movement-debug] correction flag on cooldown tick=" + player.tick
                    + " suppressed=" + this.suppressedFails
                    + " maxPosDiff=" + this.suppressedMaxPosDiff, Boar.DebugMessage.WARNING);
            return;
        }

        String suppressedNote = "";
        if (this.suppressedFails > 0) {
            suppressedNote = " +" + this.suppressedFails + " extra fails since last flag, maxPosDiff="
                    + this.suppressedMaxPosDiff + ")";
            this.suppressedFails = 0;
            this.suppressedMaxPosDiff = 0;
        }
        this.lastFlagTick = player.tick;

        final boolean checkEnabled = !Boar.getConfig().disabledChecks().contains("Correction");
        final String verbose = "o: " + posDiff + suppressedNote;
        if (player.disableMitigations() && checkEnabled) {
            if (Boar.getConfig().debugMode()) {
                this.correction.fail(verbose + "\n" + player.getMovementTrace().dump(failureInfo + suppressedNote));
            } else {
                this.correction.fail(verbose);
            }
            return;
        }

        if (Boar.getConfig().debugMode()) {
            player.getMovementTrace().flush(failureInfo + suppressedNote);
        }
        if (checkEnabled) {
            this.correction.fail(verbose);
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
}
