package ac.boar.anticheat.check.impl.velocity;

import ac.boar.anticheat.check.api.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.data.PredictionData;
import ac.boar.anticheat.data.VelocityData;
import ac.boar.anticheat.prediction.engine.data.VectorType;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3f;

import java.util.Iterator;
import java.util.Map;

@CheckInfo(name = "Velocity", type = "A")
public class VelocityA extends OffsetHandlerCheck {
    public VelocityA(BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(double offset) {
        if (System.currentTimeMillis() - player.joinedTime < 5000L) {
            return;
        }

        if (player.closetVector.getType() == VectorType.VELOCITY) {
            return;
        }

        Iterator<Map.Entry<Long, VelocityData>> iterator = player.queuedVelocities.entrySet().iterator();

        Map.Entry<Long, VelocityData> entry;
        while (iterator.hasNext() && (entry = iterator.next()) != null) {
            if (player.lastReceivedId < entry.getKey()) {
                break;
            }
            iterator.remove();

            final PredictionData data = player.postPredictionVelocities.get(entry.getKey());
            if (data == null) {
                continue;
            }

            double distance = data.afterCollision().distanceTo(player.predictedVelocity);
            if (distance < 0.0001) {
                continue;
            }

            this.fail("d=" + distance);
            if (player.queuedVelocities.isEmpty()) {
                player.teleportUtil.rewind(entry.getValue().tick(), data.beforeCollision(), data.afterCollision());
            }
        }
    }
}
