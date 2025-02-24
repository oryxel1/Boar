package ac.boar.anticheat.check.impl.velocity;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.CheckInfo;
import ac.boar.anticheat.data.PredictionData;
import ac.boar.anticheat.data.VelocityData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.data.VectorType;

import java.util.Iterator;
import java.util.Map;

@CheckInfo(name = "Velocity", type = "A")
public class VelocityA extends Check {
    public VelocityA(BoarPlayer player) {
        super(player);
    }

    public boolean check(final double offset) {
        if (player.sinceSpawnIn < 5) {
            return false;
        }

        if (player.closetVector.getType() == VectorType.VELOCITY) {
            if (offset > player.getMaxOffset()) {
                if (player.sinceTeleport > 5) {
                    this.fail("o=" + offset);
                }

                player.teleportUtil.rewind(player.velocityData == null ? player.tick - 1 : player.velocityData.tick(), player.predictedData.before(), player.predictedData.after());
                return true;
            }

            return false;
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

            double distance = data.after().distanceTo(player.predictedData.after());
            if (distance < 0.0001) {
                continue;
            }

            if (player.sinceTeleport > 5) {
                this.fail("d=" + distance);
            }

            if (player.queuedVelocities.isEmpty()) {
                player.teleportUtil.rewind(entry.getValue().tick(), data.before(), data.after());
            }

            return true;
        }

        return false;
    }
}
