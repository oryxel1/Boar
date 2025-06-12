package ac.boar.anticheat.util.block;

import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.cache.TagCache;
import org.geysermc.geyser.session.cache.tags.BlockTag;

import java.util.Locale;

import static org.geysermc.geyser.level.block.property.Properties.*;

public class BlockUtil {
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

        boolean north = connectsTo(player, main, blockState.getState(), blockState.isFaceSturdy(player), Direction.SOUTH);
        boolean east = connectsTo(player, main, blockState2.getState(), blockState2.isFaceSturdy(player), Direction.WEST);
        boolean south = connectsTo(player, main, blockState3.getState(), blockState3.isFaceSturdy(player), Direction.NORTH);
        boolean west = connectsTo(player, main, blockState4.getState(), blockState4.isFaceSturdy(player), Direction.EAST);

        // A bit hacky but works, Geyser withValue implementation seems to be broken.
        String identifier = main.block().defaultBlockState().toString().intern();
        identifier = identifier.replace("north=true", "north=" + north);
        identifier = identifier.replace("east=true", "east=" + east);
        identifier = identifier.replace("south=true", "south=" + south);
        identifier = identifier.replace("west=true", "west=" + west);
        identifier = identifier.replace("waterlogged=true", "waterlogged=false");

        return BlockState.of(BlockRegistries.JAVA_IDENTIFIER_TO_ID.getOrDefault(identifier, main.javaId()));
        //return main.block().defaultBlockState().withValue(EAST, east).withValue(NORTH, north).withValue(SOUTH, south).withValue(WATERLOGGED,false).withValue(WEST, west); this is broken, geyser fault I think?
    }

    public static BlockState findIronBarsBlockState(BoarPlayer player, BlockState state, Vector3i position) {
        BoarBlockState blockState = player.compensatedWorld.getBlockState(position.north(), 0);
        BoarBlockState blockState2 = player.compensatedWorld.getBlockState(position.south(), 0);
        BoarBlockState blockState3 = player.compensatedWorld.getBlockState(position.west(), 0);
        BoarBlockState blockState4 = player.compensatedWorld.getBlockState(position.east(), 0);

        boolean north = attachsTo(player, blockState.getState(), blockState.isFaceSturdy(player));
        boolean south = attachsTo(player, blockState2.getState(), blockState2.isFaceSturdy(player));
        boolean west = attachsTo(player, blockState3.getState(), blockState3.isFaceSturdy(player));
        boolean east = attachsTo(player, blockState4.getState(), blockState4.isFaceSturdy(player));

        // A bit hacky but works, Geyser withValue implementation seems to be broken.
        String identifier = state.block().defaultBlockState().toString().intern();
        identifier = identifier.replace("north=true", "north=" + north);
        identifier = identifier.replace("east=true", "east=" + east);
        identifier = identifier.replace("south=true", "south=" + south);
        identifier = identifier.replace("west=true", "west=" + west);
        identifier = identifier.replace("waterlogged=true", "waterlogged=false");

        return BlockState.of(BlockRegistries.JAVA_IDENTIFIER_TO_ID.getOrDefault(identifier, state.javaId()));
    }

    private static boolean connectsTo(BoarPlayer player, BlockState blockState, BlockState neighbour, boolean bl, Direction direction) {
        final TagCache tagCache = player.getSession().getTagCache();

        boolean bl2 = isSameFence(tagCache, neighbour, blockState);
        boolean bl3 = tagCache.is(BlockTag.FENCE_GATES, neighbour.block()) && connectsToDirection(neighbour, direction);
        return !isExceptionForConnection(tagCache, neighbour) && bl || bl2 || bl3;
    }

    private static boolean attachsTo(BoarPlayer player, BlockState blockState, boolean bl) {
        final TagCache tagCache = player.getSession().getTagCache();
        return !isExceptionForConnection(tagCache, blockState) && bl || blockState.is(Blocks.IRON_BARS) || blockState.toString().toLowerCase(Locale.ROOT).contains("glass_pane") || tagCache.is(BlockTag.WALLS, blockState.block());
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