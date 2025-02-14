package ac.boar.anticheat.handler;

import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;

public final class BlockInteractHandler {
    public static void handleBlockClick(final BoarPlayer player, final Vector3i vector3i) {
        final BlockState state = player.compensatedWorld.getBlockState(vector3i);
        if (!state.is(Blocks.BARREL) && state.getValue(Properties.OPEN) != null) {
            int newId = state.withValue(Properties.OPEN, !state.getValue(Properties.OPEN)).javaId();
            player.compensatedWorld.updateBlock(vector3i, newId);
        }
    }

}