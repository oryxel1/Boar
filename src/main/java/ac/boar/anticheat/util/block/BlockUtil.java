package ac.boar.anticheat.util.block;

import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.mappings.BlockMappings;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.registry.BlockRegistries;

import java.util.Locale;

import static org.geysermc.geyser.level.block.property.Properties.*;

public class BlockUtil {
    public static BlockState getPlacementState(BoarPlayer player, Block block, Vector3i position) {
        return block.defaultBlockState();
    }

    public static boolean determineCanBreak(final BoarPlayer player, final BlockState state) {
        if (state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR) || state.is(Blocks.LAVA) || state.is(Blocks.WATER)) {
            return false;
        }

        float destroyTime = state.block().destroyTime();
        return destroyTime != -1 || player.gameType == GameType.CREATIVE;
    }

    public static BlockState findFenceBlockState(BoarPlayer player, BlockState main, Vector3i position) {
        BoarBlockState blockState = player.compensatedWorld.getBlockState(position.north(), 0);
        BoarBlockState blockState2 = player.compensatedWorld.getBlockState(position.east(), 0);
        BoarBlockState blockState3 = player.compensatedWorld.getBlockState(position.south(), 0);
        BoarBlockState blockState4 = player.compensatedWorld.getBlockState(position.west(), 0);

        boolean north = connectsTo(main, blockState.getState(), blockState.isFaceSturdy(player), Direction.SOUTH);
        boolean east = connectsTo(main, blockState2.getState(), blockState2.isFaceSturdy(player), Direction.WEST);
        boolean south = connectsTo(main, blockState3.getState(), blockState3.isFaceSturdy(player), Direction.NORTH);
        boolean west = connectsTo(main, blockState4.getState(), blockState4.isFaceSturdy(player), Direction.EAST);

        // A bit hacky but works, Geyser withValue implementation seems to be broken.
        String identifier = main.block().defaultBlockState().toString().intern();
        identifier = identifier.replace("north=true", "north=" + north);
        identifier = identifier.replace("east=true", "east=" + east);
        identifier = identifier.replace("south=true", "south=" + south);
        identifier = identifier.replace("west=true", "west=" + west);
        identifier = identifier.replace("waterlogged=true", "waterlogged=false");

        return BlockState.of(BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.getOrDefault(identifier, main.javaId()));
        //return main.block().defaultBlockState().withValue(EAST, east).withValue(NORTH, north).withValue(SOUTH, south).withValue(WATERLOGGED,false).withValue(WEST, west); this is broken, geyser fault I think?
    }

    public static BlockState findIronBarsBlockState(BoarPlayer player, BlockState state, Vector3i position) {
        BoarBlockState blockState = player.compensatedWorld.getBlockState(position.north(), 0);
        BoarBlockState blockState2 = player.compensatedWorld.getBlockState(position.south(), 0);
        BoarBlockState blockState3 = player.compensatedWorld.getBlockState(position.west(), 0);
        BoarBlockState blockState4 = player.compensatedWorld.getBlockState(position.east(), 0);

        boolean north = attachsTo(blockState.getState(), blockState.isFaceSturdy(player));
        boolean south = attachsTo(blockState2.getState(), blockState2.isFaceSturdy(player));
        boolean west = attachsTo(blockState3.getState(), blockState3.isFaceSturdy(player));
        boolean east = attachsTo(blockState4.getState(), blockState4.isFaceSturdy(player));

        // A bit hacky but works, Geyser withValue implementation seems to be broken.
        String identifier = state.block().defaultBlockState().toString().intern();
        identifier = identifier.replace("north=true", "north=" + north);
        identifier = identifier.replace("east=true", "east=" + east);
        identifier = identifier.replace("south=true", "south=" + south);
        identifier = identifier.replace("west=true", "west=" + west);
        identifier = identifier.replace("waterlogged=true", "waterlogged=false");

        return BlockState.of(BlockRegistries.JAVA_BLOCK_STATE_IDENTIFIER_TO_ID.getOrDefault(identifier, state.javaId()));
    }

    private static boolean connectsTo(BlockState blockState, BlockState neighbour, boolean bl, Direction direction) {
        return !isExceptionForConnection(neighbour) && bl || isSameFence(neighbour, blockState) || connectsToDirection(neighbour, direction);
    }

    private static boolean attachsTo(BlockState blockState, boolean bl) {
        boolean walls = BlockMappings.getWallBlocks().contains(blockState.block());
        return !isExceptionForConnection(blockState) && bl || blockState.is(Blocks.IRON_BARS) || blockState.toString().toLowerCase(Locale.ROOT).contains("glass_pane") || walls;
    }

    private static boolean isSameFence(BlockState blockState, BlockState currentBlockState) {
        return BlockMappings.getFenceBlocks().contains(blockState.block()) && blockState.is(Blocks.NETHER_BRICK_FENCE) == currentBlockState.is(Blocks.NETHER_BRICK_FENCE);
    }

    public static boolean connectsToDirection(BlockState blockState, Direction direction) {
        if (!BlockMappings.getFenceGateBlocks().contains(blockState.block())) {
            return false;
        }

        return blockState.getValue(HORIZONTAL_FACING).getAxis() == getClockWise(direction).getAxis();
    }

    public static boolean isExceptionForConnection(BlockState blockState) {
        return BlockMappings.getLeavesBlocks().contains(blockState.block()) || blockState.is(Blocks.BARRIER) ||
                blockState.is(Blocks.CARVED_PUMPKIN) || blockState.is(Blocks.JACK_O_LANTERN) || blockState.is(Blocks.MELON) || blockState.is(Blocks.PUMPKIN)
                || BlockMappings.getShulkerBlocks().contains(blockState.block());
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