package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.check.api.annotations.CheckInfo;
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
        double eotOffset = player.unvalidatedTickEnd.distanceTo(player.velocity);

        Vec3 predicted = player.position.subtract(player.prevUnvalidatedPosition);
        Vec3 actual = player.unvalidatedPosition.subtract(player.prevUnvalidatedPosition);
        if (actual.length() > 1e-5 || offset > maxOffset || eotOffset > maxOffset) {
            String colorOffset = offset > maxOffset ? "§c" : offset > 1.0E-5 ? "§6" : "§a";

            Bukkit.broadcastMessage(colorOffset + "O:" + offset + ", T: " + player.bestPossibility.getType() + ", P: " + predicted.x + "," + predicted.y + "," + predicted.z + ", MO=" + maxOffset);

            Bukkit.broadcastMessage("§7A: " + actual.x + "," + actual.y + "," + actual.z + ", " + "SPRINTING=" + player.sprinting + ", SNEAKING=" + player.sneaking + ", sinceTeleport=" + player.sinceTeleport);

            Bukkit.broadcastMessage("A EOT: " + player.velocity.toVector3f().toString());
            Bukkit.broadcastMessage("EOT O: " + (eotOffset > 1e-4 ? "§b" : "§a") + eotOffset + "," + player.unvalidatedTickEnd.toVector3f().toString());
        }
    }
}
