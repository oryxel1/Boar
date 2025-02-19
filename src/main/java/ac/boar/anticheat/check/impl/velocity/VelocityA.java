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

    public boolean check() {
        if (player.closetVector.getType() == VectorType.VELOCITY || player.sinceSpawnIn < 5) {
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

            double distance = data.afterCollision().distanceTo(player.predictedVelocity);
            if (distance < 0.0001) {
                continue;
            }

            if (player.sinceTeleport > 5) {
                this.fail("d=" + distance);
            }

            if (player.queuedVelocities.isEmpty()) {
                player.teleportUtil.rewind(entry.getValue().tick(), data.beforeCollision(), data.afterCollision());
            }

            return true;
        }

        return false;
    }
}
