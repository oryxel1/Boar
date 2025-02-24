package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.check.api.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import org.bukkit.Bukkit;

@CheckInfo(name = "DebugOffset")
public class DebugOffsetA extends OffsetHandlerCheck {
    public DebugOffsetA(BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(double offset) {
        final double maxOffset = player.getMaxOffset();
        double eotOffset = player.claimedEOT.distanceTo(player.eotVelocity);

        Vec3 predicted = player.predictedPosition.subtract(player.prevPosition);
        Vec3 actual = player.position.subtract(player.prevPosition);
        if (player.position.subtract(player.prevPosition).length() > 1e-5 || offset > maxOffset || eotOffset > maxOffset) {
            Bukkit.broadcastMessage((offset > maxOffset ? "§c" : "§a") + "O:" + offset + ", T: " + player.closetVector.getType() + ", P: " + predicted.x + "," + predicted.y + "," + predicted.z + ", MO=" + maxOffset);

            Bukkit.broadcastMessage("§7A: " + actual.x + "," + actual.y + "," + actual.z + ", " +
                    "SPRINTING=" + player.sprinting + ", SNEAKING=" + player.sneaking + ", JUMPING=" + player.closetVector.isJumping() +
                    ", ENGINE=" + player.engine.getClass().getSimpleName() + ", sinceTeleport=" + player.sinceTeleport);

            Bukkit.broadcastMessage("A EOT: " + player.eotVelocity.toVector3f().toString());
            Bukkit.broadcastMessage("EOT O: " + (eotOffset > 1e-4 ? "§b" : "§a") + eotOffset + "," + player.claimedEOT.toVector3f().toString());
        }
    }
}
