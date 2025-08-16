package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.check.api.annotations.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.geyser.GeyserBoar;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

@CheckInfo(name = "DebugOffset")
public class DebugOffsetA extends OffsetHandlerCheck {
    public DebugOffsetA(BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(float offset) {
        if (!player.isDebugMode()) {
            return;
        }

        final float maxOffset = player.getMaxOffset();
        float eotOffset = player.unvalidatedTickEnd.distanceTo(player.velocity);

        Vec3 predicted = player.position.subtract(player.prevUnvalidatedPosition);
        Vec3 actual = player.unvalidatedPosition.subtract(player.prevUnvalidatedPosition);
        if (actual.length() > 1e-5 || offset > maxOffset || eotOffset > maxOffset) {
            String colorOffset = offset > maxOffset ? "§c" : offset > 1.0E-5 ? "§6" : "§a";

            String predDebug = colorOffset + "O:" + offset + ", T: " + player.bestPossibility.getType() + ", P: " + predicted.x + "," + predicted.y + "," + predicted.z + ", pos=" + player.position;
            if (offset > 1.0E-5) {
                if (offset > 1.0E-4) {
                    GeyserBoar.getLogger().severe(predDebug);
                } else {
                    GeyserBoar.getLogger().warning(predDebug);
                }
            } else {
                GeyserBoar.getLogger().info(predDebug);
            }

            GeyserBoar.getLogger().info("§7A: " + actual.x + "," + actual.y + "," + actual.z + ", " + "SPRINTING=" + player.getFlagTracker().has(EntityFlag.SPRINTING) + ", SNEAKING=" + player.getFlagTracker().has(EntityFlag.SNEAKING) + ", water=" + player.touchingWater);

            GeyserBoar.getLogger().info("A EOT: " + player.velocity.toVector3f().toString());
            GeyserBoar.getLogger().info("EOT O: " + (eotOffset > 1e-4 ? "§b" : "§a") + eotOffset + "," + player.unvalidatedTickEnd.toVector3f().toString());
        }
    }
}
