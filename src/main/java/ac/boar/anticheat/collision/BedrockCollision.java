package ac.boar.anticheat.collision;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Box;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.ChestType;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.*;
import org.geysermc.geyser.level.physics.Direction;

import java.util.ArrayList;
import java.util.List;

// Patch collision in bedrock that is different from java, or block with dynamic collision (ex: scaffolding)
public class BedrockCollision {
    private final static List<Box> EMPTY_SHAPE = List.of();
    
    private final static List<Box> BED_SHAPE = List.of(new Box(0, 0, 0, 1, 0.5625F, 1));
    private final static List<Box> HONEY_SHAPE = List.of(new Box(0.0625F, 0, 0.0625F, 0.9375F, 1, 0.9375F));
    private final static List<Box> LECTERN_SHAPE = List.of(new Box(0, 0, 0, 1, 0.9F, 1));
    private final static List<Box> CONDUIT_SHAPE = List.of(new Box(0.25F, 0, 0.25F, 0.75f, 0.5F, 0.75f));
    private final static List<Box> CACTUS_SHAPE = List.of(new Box(0.0625F, 0, 0.0625F, 0.9375F, 1, 0.9375F));

    // Chest
    private final static List<Box> SINGLE_CHEST_SHAPE = List.of(new Box(0.025F, 0, 0.025F, 0.975F, 0.95F, 0.975F));
    private final static List<Box> NORTH_CHEST_SHAPE = List.of(new Box(0.025F, 0, 0, 0.975F, 0.95F, 0.975F));
    private final static List<Box> SOUTH_CHEST_SHAPE = List.of(new Box(0.025F, 0, 0.025F, 0.975F, 0.95F, 1));
    private final static List<Box> WEST_CHEST_SHAPE = List.of(new Box(0, 0, 0.025F, 0.975F, 0.95F, 0.975F));
    private final static List<Box> EAST_CHEST_SHAPE = List.of(new Box(0.025F, 0, 0.025F, 1, 0.95F, 0.975F));

    // Scaffolding
    private final static List<Box> SCAFFOLDING_NORMAL_SHAPE;
    private final static Box SCAFFOLDING_COLLISION_SHAPE = new Box(0, 0, 0, 1, 0.125F, 1);
    private final static Box SCAFFOLDING_OUTLINE_SHAPE = new Box(0, 0, 0, 1, 1, 1).offset(0, -1, 0);

    // Cauldron
    private final static List<Box> CAULDRON_SHAPE;

    // Trapdoor
    private final static List<Box> TRAPDOOR_EAST_SHAPE = List.of(new Box(0, 0, 0, 0.1825F, 1, 1));
    private final static List<Box> TRAPDOOR_WEST_SHAPE = List.of(new Box(0.8175F, 0, 0, 1, 1, 1));
    private final static List<Box> TRAPDOOR_SOUTH_SHAPE = List.of(new Box(0, 0, 0, 1, 1, 0.1825F));
    private final static List<Box> TRAPDOOR_NORTH_SHAPE = List.of(new Box(0, 0, 0.8175F, 1, 1, 1));
    private final static List<Box> TRAPDOOR_OPEN_BOTTOM_SHAPE = List.of(new Box(0, 0, 0, 1, 0.1825F, 1));
    private final static List<Box> TRAPDOOR_OPEN_TOP_SHAPE = List.of(new Box(0, 0.8175F, 0, 1, 1, 1));

    // Door
    private final static List<Box> DOOR_NORTH_SHAPE = List.of(new Box(0, 0, 0, 1, 1, 0.1825F));
    private final static List<Box> DOOR_SOUTH_SHAPE = List.of(new Box(0, 0, 0.8175F, 1, 1, 1));
    private final static List<Box> DOOR_EAST_SHAPE = List.of(new Box(0.8175F, 0, 0, 1, 1, 1));
    private final static List<Box> DOOR_WEST_SHAPE = List.of(new Box(0, 0, 0, 0.1825F, 1, 1));

    static {
        // Scaffolding
        {
            Box lv = new Box(0, 0.875f, 0, 1, 1, 1);
            Box lv2 = new Box(0, 0, 0, 0.125f, 1, 0.125f);
            Box lv3 = new Box(0.875f, 0, 0, 1, 1, 0.125f);
            Box lv4 = new Box(0, 0, 0.875f, 0.125f, 1, 1);
            Box lv5 = new Box(0.875f, 0, 0.875f, 1, 1, 1);
            SCAFFOLDING_NORMAL_SHAPE = List.of(lv, lv2, lv3, lv4, lv5);
        }

        // Cauldron
        {
            float f = 0.125F;
            List<Box> boxes = new ArrayList<>();
            boxes.add(new Box(0.0F, 0.0F, 0.0F, 1.0F, 0.3125F, 1.0F));
            boxes.add(new Box(0.0F, 0.0F, 0.0F, f, 1.0F, 1.0F));
            boxes.add(new Box(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, f));
            boxes.add(new Box(1.0F - f, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F));
            boxes.add(new Box(0.0F, 0.0F, 1.0F - f, 1.0F, 1.0F, 1.0F));
            CAULDRON_SHAPE = boxes;
        }
    }
    
    public static List<Box> getCollisionBox(final BoarPlayer player, final Vector3i vector3i, final BlockState state) {
        if (state.is(Blocks.ENDER_CHEST)) {
            return SINGLE_CHEST_SHAPE;
        }

        if (state.is(Blocks.SEA_PICKLE)) {
            return EMPTY_SHAPE;
        }

        if (state.block() instanceof BedBlock) {
            return BED_SHAPE;
        }

        if (state.is(Blocks.HONEY_BLOCK)) {
            return HONEY_SHAPE;
        }

        if (state.is(Blocks.LECTERN)) {
            return LECTERN_SHAPE;
        }

        if (state.is(Blocks.CAULDRON)) {
            return CAULDRON_SHAPE;
        }

        if (state.is(Blocks.CONDUIT)) {
            return CONDUIT_SHAPE;
        }

        if (state.is(Blocks.CACTUS)) {
            return CACTUS_SHAPE;
        }

        if (state.block() instanceof ChestBlock) {
            final ChestType type = state.getValue(Properties.CHEST_TYPE);
            Direction facing = state.getValue(Properties.HORIZONTAL_FACING);
            if (type == ChestType.LEFT) {
                facing = switch (facing) {
                    case SOUTH -> Direction.WEST;
                    case WEST -> Direction.NORTH;
                    case EAST -> Direction.SOUTH;
                    default -> Direction.EAST;
                };
            } else {
                facing = switch (facing) {
                    case SOUTH -> Direction.EAST;
                    case WEST -> Direction.SOUTH;
                    case EAST -> Direction.NORTH;
                    default -> Direction.WEST;
                };
            }

            if (type == ChestType.SINGLE) {
                return SINGLE_CHEST_SHAPE;
            } else {
                switch (facing) {
                    case SOUTH -> {
                        return SOUTH_CHEST_SHAPE;
                    }
                    case WEST -> {
                        return WEST_CHEST_SHAPE;
                    }
                    case EAST -> {
                        return EAST_CHEST_SHAPE;
                    }
                    default -> {
                        return NORTH_CHEST_SHAPE;
                    }
                }
            }
        }

        if (state.block() instanceof TrapDoorBlock) {
            if (!state.getValue(Properties.OPEN)) {
                return state.getValue(Properties.HALF).equalsIgnoreCase("top") ? TRAPDOOR_OPEN_TOP_SHAPE : TRAPDOOR_OPEN_BOTTOM_SHAPE;
            } else {
                switch (state.getValue(Properties.HORIZONTAL_FACING)) {
                    case SOUTH -> {
                        return TRAPDOOR_SOUTH_SHAPE;
                    }
                    case WEST -> {
                        return TRAPDOOR_WEST_SHAPE;
                    }
                    case EAST -> {
                        return TRAPDOOR_EAST_SHAPE;
                    }
                    default -> {
                        return TRAPDOOR_NORTH_SHAPE;
                    }
                }
            }
        }

        if (state.block() instanceof DoorBlock) {
            Direction direction = state.getValue(Properties.HORIZONTAL_FACING);
            boolean bl = !state.getValue(Properties.OPEN);
            boolean bl2 = state.getValue(Properties.DOOR_HINGE).equalsIgnoreCase("right");

            switch (direction) {
                case SOUTH -> {
                    return bl ? DOOR_NORTH_SHAPE : (bl2 ? DOOR_WEST_SHAPE : DOOR_EAST_SHAPE);
                }
                case WEST -> {
                    return bl ? DOOR_EAST_SHAPE : (bl2 ? DOOR_NORTH_SHAPE : DOOR_SOUTH_SHAPE);
                }
                case NORTH -> {
                    return bl ? DOOR_SOUTH_SHAPE : (bl2 ? DOOR_EAST_SHAPE : DOOR_WEST_SHAPE);
                }
                default -> {
                    return bl ? DOOR_WEST_SHAPE : (bl2 ? DOOR_SOUTH_SHAPE : DOOR_NORTH_SHAPE);
                }
            }
        }

        if (state.is(Blocks.SCAFFOLDING)) {
            boolean above = player.boundingBox.minY > vector3i.getY() + 1 - 1.0E-5F;
            boolean aboveOutline = player.boundingBox.minY > vector3i.getY() + SCAFFOLDING_OUTLINE_SHAPE.maxY - 1.0E-5F;
            if (above && !player.getInputData().contains(PlayerAuthInputData.WANT_DOWN)) {
                return SCAFFOLDING_NORMAL_SHAPE;
            } else {
                return state.getValue(Properties.STABILITY_DISTANCE) != 0 && state.getValue(Properties.BOTTOM)
                        && aboveOutline ? List.of(SCAFFOLDING_COLLISION_SHAPE) : EMPTY_SHAPE;
            }
        }

        return null;
    }
}
