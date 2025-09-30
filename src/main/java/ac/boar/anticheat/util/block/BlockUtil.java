package ac.boar.anticheat.util.block;

import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.DirectionUtil;
import ac.boar.mappings.BlockMappings;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.ChestType;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.block.type.SkullBlock;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.registry.BlockRegistries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.SkullCache;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;

import java.util.Locale;
import java.util.Objects;

import static org.geysermc.geyser.level.block.property.Properties.*;

public class BlockUtil {
    public static void restoreCorrectBlock(GeyserSession session, Vector3i vector, BlockState blockState) {
        BlockDefinition bedrockBlock = session.getBlockMappings().getBedrockBlock(blockState);

        if (blockState.block() instanceof SkullBlock skullBlock && skullBlock.skullType() == SkullBlock.Type.PLAYER) {
            // The changed block was a player skull so check if a custom block was defined for this skull
            SkullCache.Skull skull = session.getSkullCache().getSkulls().get(vector);
            if (skull != null && skull.getBlockDefinition() != null) {
                bedrockBlock = skull.getBlockDefinition();
            }
        }

        UpdateBlockPacket updateBlockPacket = new UpdateBlockPacket();
        updateBlockPacket.setDataLayer(0);
        updateBlockPacket.setBlockPosition(vector);
        updateBlockPacket.setDefinition(bedrockBlock);
        updateBlockPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(updateBlockPacket);

        UpdateBlockPacket updateWaterPacket = new UpdateBlockPacket();
        updateWaterPacket.setDataLayer(1);
        updateWaterPacket.setBlockPosition(vector);
        updateWaterPacket.setDefinition(BlockRegistries.WATERLOGGED.get().get(blockState.javaId()) ? session.getBlockMappings().getBedrockWater() : session.getBlockMappings().getBedrockAir());
        updateWaterPacket.getFlags().addAll(UpdateBlockPacket.FLAG_ALL_PRIORITY);
        session.sendUpstreamPacket(updateWaterPacket);

        // Reset the item in hand to prevent "missing" blocks
        session.getPlayerInventoryHolder().updateSlot(session.getPlayerInventory().getHeldItemSlot()); // TODO test
    }

    public static Vector3i getBlockPosition(Vector3i blockPos, int face) {
        return switch (face) {
            case 0 -> blockPos.sub(0, 1, 0);
            case 1 -> blockPos.add(0, 1, 0);
            case 2 -> blockPos.sub(0, 0, 1);
            case 3 -> blockPos.add(0, 0, 1);
            case 4 -> blockPos.sub(1, 0, 0);
            case 5 -> blockPos.add(1, 0, 0);
            default -> blockPos;
        };
    }

    public static void restoreCorrectBlock(GeyserSession session, Vector3i blockPos) {
        restoreCorrectBlock(session, blockPos, session.getGeyser().getWorldManager().blockAt(session, blockPos));
    }

    public static BlockState getPlacementState(BoarPlayer player, Block block, Vector3i position) {
        return block.defaultBlockState();
    }

    // This is no longer accurate as of 1.21.110 because Mojang decide to break chest state even more yay!
    // There is so many FUCKING PROBLEMS WITH CHEST WTFFFFF WHY THE FUCK CHEST IS SO BROKEN AND NOW EVEN MORE BROKEN
    // AS OF 1.21.110 MOJANG????
    public static BlockState findChestState(final BoarPlayer player, final BlockState state, final Vector3i vector3i) {
        final BlockEntityInfo blockEntity = player.compensatedWorld.getBlockEntity(vector3i.getX(), vector3i.getY(), vector3i.getZ());
        if (blockEntity == null || blockEntity.getNbt() == null) {
            return state.withValue(CHEST_TYPE, ChestType.SINGLE);
        }

        int pairX = blockEntity.getNbt().getInt("pairx", 0);
        int pairZ = blockEntity.getNbt().getInt("pairz", 0);
        if (Math.abs(vector3i.getX() - pairX) > 1 || Math.abs(vector3i.getZ() - pairZ) > 1) {
            return state.withValue(CHEST_TYPE, ChestType.SINGLE);
        }
        final Vector3i attachedPos = Vector3i.from(pairX, vector3i.getY(), pairZ);

        final int id = player.compensatedWorld.getBlockAt(attachedPos.getX(), attachedPos.getY(), attachedPos.getZ(), 0);
        if (BlockState.of(id).block().javaId() != state.block().javaId()) {
            return state.withValue(CHEST_TYPE, ChestType.SINGLE);
        }

        int pairLead = blockEntity.getNbt().getInt("pairlead", 0);
        return state.withValue(CHEST_TYPE, pairLead == 1 ? ChestType.RIGHT : ChestType.LEFT);
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

        return blockState.getValue(HORIZONTAL_FACING).getAxis() == DirectionUtil.getClockWise(direction).getAxis();
    }

    public static boolean isExceptionForConnection(BlockState blockState) {
        return BlockMappings.getLeavesBlocks().contains(blockState.block()) || blockState.is(Blocks.BARRIER) ||
                blockState.is(Blocks.CARVED_PUMPKIN) || blockState.is(Blocks.JACK_O_LANTERN) || blockState.is(Blocks.MELON) || blockState.is(Blocks.PUMPKIN)
                || BlockMappings.getShulkerBlocks().contains(blockState.block());
    }

    public static String getStairShape(BoarPlayer player, BlockState state, Vector3i pos) {
        Direction direction = state.getValue(HORIZONTAL_FACING);
        BlockState blockState = player.compensatedWorld.getBlockState(pos.add(direction.getUnitVector()), 0).getState();
        if (isStairs(blockState) && Objects.equals(state.getValue(HALF), blockState.getValue(HALF))) {
            Direction direction2 = blockState.getValue(HORIZONTAL_FACING);
            if (direction2.getAxis() != state.getValue(HORIZONTAL_FACING).getAxis() && isDifferentOrientation(player, state, pos, direction2.reversed())) {
                if (direction2 == DirectionUtil.rotateYCounterclockwise(direction)) {
                    return "outer_left";
                }

                return "outer_right";
            }
        }

        BlockState blockState2 = player.compensatedWorld.getBlockState(pos.add(direction.reversed().getUnitVector()), 0).getState();
        if (isStairs(blockState2) && Objects.equals(state.getValue(HALF), blockState2.getValue(HALF))) {
            Direction direction3 = blockState2.getValue(HORIZONTAL_FACING);
            if (direction3.getAxis() != state.getValue(HORIZONTAL_FACING).getAxis() && isDifferentOrientation(player, state, pos, direction3)) {
                if (direction3 == DirectionUtil.rotateYCounterclockwise(direction)) {
                    return "inner_left";
                }

                return "inner_right";
            }
        }

        return "straight";
    }

    private static boolean isDifferentOrientation(BoarPlayer player, BlockState state, Vector3i pos, Direction dir) {
        BlockState blockState = player.compensatedWorld.getBlockState(pos.add(dir.getUnitVector()), 0).getState();
        return !isStairs(blockState) || blockState.getValue(HORIZONTAL_FACING) != state.getValue(HORIZONTAL_FACING) || !Objects.equals(blockState.getValue(HALF), state.getValue(HALF));
    }

    public static boolean isStairs(BlockState state) {
        return BlockMappings.getStairsBlocks().contains(state.block());
    }
}