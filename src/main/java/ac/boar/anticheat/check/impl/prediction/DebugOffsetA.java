package ac.boar.anticheat.check.impl.prediction;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.check.api.CheckInfo;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.player.BoarPlayer;
import org.bukkit.Bukkit;

@CheckInfo(name = "DebugOffset")
public class DebugOffsetA extends OffsetHandlerCheck {
    public DebugOffsetA(BoarPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(double offset) {
        final double maxOffset = player.getMaxOffset();
        if (player.actualVelocity.length() > 1e-5 || offset > maxOffset) {
            Bukkit.broadcastMessage((offset > maxOffset ? "§c" : "§a") + "O:" + offset + ", T: " + player.closetVector.getType() + ", P: " +
                    player.predictedVelocity.x + "," + player.predictedVelocity.y + "," + player.predictedVelocity.z + ", MO=" + maxOffset);

            Bukkit.broadcastMessage("§7A: " + player.actualVelocity.x + "," + player.actualVelocity.y + "," + player.actualVelocity.z + ", " +
                    "SPRINTING=" + player.sprinting + ", SNEAKING=" + player.sneaking + ", JUMPING=" + player.closetVector.isJumping() +
                    ", ENGINE=" + player.engine.getClass().getSimpleName());
        }

        if (offset > maxOffset && Boar.IS_IN_DEBUGGING) {
            player.updateBoundingBox(player.x, player.y, player.z);
        }

        Bukkit.broadcastMessage(player.claimedEOT.toVector3f().toString());
    }
}
