package ac.boar.anticheat.util.block;

import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.BlockState;

public class BlockUtil {
    public static boolean determineCanBreak(final BoarPlayer player, final BlockState state) {
        if (state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR) || state.is(Blocks.LAVA) || state.is(Blocks.WATER)) {
            return false;
        }

        float destroyTime = state.block().destroyTime();
        return destroyTime != -1 || player.gameType == GameType.CREATIVE;
    }

    public static float getWorldFluidHeight(Fluid fluidType, int blockId) {
        return (float) switch (fluidType) {
            case WATER -> BlockStateValues.getWaterHeight(blockId);
            case LAVA -> BlockStateValues.getLavaHeight(blockId);
            case EMPTY -> -1;
        };
    }
}