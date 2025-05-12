package ac.boar.anticheat.util.block;

import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.cache.TagCache;
import org.geysermc.geyser.session.cache.tags.BlockTag;

import static org.geysermc.geyser.level.block.property.Properties.*;

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

    public static BlockState findFenceBlockState(BoarPlayer player, Vector3i position) {
        BlockState main = player.compensatedWorld.getBlockState(position, 0, false).getState();

        BoarBlockState blockState = player.compensatedWorld.getBlockState(position.north(), 0, false);
        BoarBlockState blockState2 = player.compensatedWorld.getBlockState(position.east(), 0, false);
        BoarBlockState blockState3 = player.compensatedWorld.getBlockState(position.south(), 0, false);
        BoarBlockState blockState4 = player.compensatedWorld.getBlockState(position.west(), 0, false);

        boolean north = connectsTo(player, main, blockState.getState(), blockState.isFaceSturdy(player), Direction.SOUTH);
        boolean east = connectsTo(player, main, blockState2.getState(), blockState2.isFaceSturdy(player), Direction.WEST);
        boolean south = connectsTo(player, main, blockState3.getState(), blockState3.isFaceSturdy(player), Direction.NORTH);
        boolean west = connectsTo(player, main, blockState4.getState(), blockState4.isFaceSturdy(player), Direction.EAST);

        // waterlogged value doesn't matter that much since we check for layer 1 instead
        return main.block().defaultBlockState().withValue(NORTH, north).withValue(EAST, east).withValue(SOUTH, south).withValue(WEST, west).withValue(WATERLOGGED, false);
    }

    private static boolean connectsTo(BoarPlayer player, BlockState blockState, BlockState neighbour, boolean bl, Direction direction) {
        final TagCache tagCache = player.getSession().getTagCache();

        boolean bl2 = isSameFence(tagCache, neighbour, blockState);
        boolean bl3 = tagCache.is(BlockTag.FENCE_GATES, neighbour.block()) && connectsToDirection(neighbour, direction);
        return !isExceptionForConnection(tagCache, neighbour) && bl || bl2 || bl3;
    }

    private static boolean isSameFence(TagCache tagCache, BlockState blockState, BlockState currentBlockState) {
        return tagCache.is(BlockTag.FENCES, blockState.block()) && tagCache.is(BlockTag.WOODEN_FENCES, blockState.block()) ==
                tagCache.is(BlockTag.WOODEN_FENCES, currentBlockState.block());
    }

    public static boolean connectsToDirection(BlockState blockState, Direction direction) {
        return blockState.getValue(FACING).getAxis() == getClockWise(direction).getAxis();
    }

    public static boolean isExceptionForConnection(TagCache cache, BlockState blockState) {
        return cache.is(BlockTag.LEAVES, blockState.block()) || blockState.is(Blocks.BARRIER) || blockState.is(Blocks.CARVED_PUMPKIN) || blockState.is(Blocks.JACK_O_LANTERN) || blockState.is(Blocks.MELON) || blockState.is(Blocks.PUMPKIN)
                || cache.is(BlockTag.SHULKER_BOXES, blockState.block());
    }

    private static Direction getClockWise(Direction direction) {
        return switch (direction.ordinal()) {
            case 2 -> Direction.EAST;
            case 5 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            case 4 -> Direction.NORTH;
            default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + direction);
        };
    }
}