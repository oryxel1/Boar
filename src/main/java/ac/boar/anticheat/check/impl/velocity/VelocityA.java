package ac.boar.anticheat.check.impl.velocity;

import ac.boar.anticheat.check.api.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
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

        Iterator<Map.Entry<Long, Vec3f>> iterator = player.queuedVelocities.entrySet().iterator();

        Map.Entry<Long, Vec3f> entry;
        while (iterator.hasNext() && (entry = iterator.next()) != null) {
            if (player.lastReceivedId < entry.getKey()) {
                break;
            }
            iterator.remove();

            Vec3f vec3f = player.postPredictionVelocities.get(entry.getKey());
            if (vec3f == null) {
                continue;
            }

            double distance = vec3f.distanceTo(player.predictedVelocity);
            if (distance < 0.0001) {
                continue;
            }

            if (player.queuedVelocities.isEmpty()) {
                player.teleportUtil.rewind(vec3f);
            }
            this.fail("d=" + distance);
        }
    }
}
