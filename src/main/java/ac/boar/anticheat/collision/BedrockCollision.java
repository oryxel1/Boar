package ac.boar.anticheat.collision;

import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Box;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.item.Items;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.property.ChestType;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.*;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.geyser.util.BlockUtils;

import java.util.ArrayList;
import java.util.List;

// Patch collision in bedrock that is different from java, or block with dynamic collision (ex: scaffolding)
public class BedrockCollision {
    private final static List<Box> EMPTY_SHAPE = List.of();
    private final static List<Box> SOLID_SHAPE = List.of(new Box(0, 0, 0, 1, 1, 1));
    
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

    private final static List<Box> LANTERN_SHAPE = List.of(new Box(0.3125F, 0, 0.3125F, 0.6875F, 0.5F, 0.6875F));

    private final static List<Box> ANVIL_X_SHAPE = List.of(new Box(0.0F, 0.0F, 0.125F, 1.0F, 1.0F, 0.875F));
    private final static List<Box> ANVIL_OTHER_SHAPE = List.of(new Box(0.125F, 0.0F, 0.0F, 0.875F, 1.0F, 1.0F));

    private final static List<Box> FALLING_POWDER_SNOW_SNOW = List.of(new Box(0.0F, 0.0F, 0.0F, 0.0625F, 0.05625F, 0.0625F));

    private final static List<Box> END_PORTAL_FRAME_SHAPE = List.of(new Box(0, 0, 0, 1, 0.8125F, 1));

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
    
    public static List<Box> getCollisionBox(final BoarPlayer player, final Box box, final Vector3i vector3i, final BlockState state) {
        if (vector3i.getY() == player.compensatedWorld.getDimension().minY() - 41) {
            return SOLID_SHAPE;
        }

        if (state.is(Blocks.BELL) && state.getValue(Properties.BELL_ATTACHMENT).equals("floor")) {
            final List<Box> collisions = new ArrayList<>();
            for (BoundingBox boundingBox : BlockUtils.getCollision(state.javaId()).getBoundingBoxes()) {
                collisions.add(new Box(
                        (float) boundingBox.getMin(Axis.X),
                        (float) boundingBox.getMin(Axis.Y),
                        (float) boundingBox.getMin(Axis.Z),
                        (float) boundingBox.getMax(Axis.X),
                        (float) boundingBox.getMax(Axis.Y) - 0.1875F,
                        (float) boundingBox.getMax(Axis.Z)
                ));
            }
            return collisions;
        }

        if (state.is(Blocks.BAMBOO)) {
//            Box baseShape = state.getValue(Properties.BAMBOO_LEAVES).equals("large") ? new Box(0, 0, 0, 0.1875F, 1, 0.1875F) : new Box(0, 0, 0, 0.125F, 1, 0.125F);

            // Couldn't they just keep the bamboo offsetting pre 1.21.80 but nope they changed it, and now I have no idea how it works?
            // Let's try to hack around this, if player is not colliding or only colliding horizontally then look at
            // UncertainRunner#extraOffsetNonTickEnd and EntityTicker#doSelfMove, for VERTICAL_COLLISION we can do a bit tricky hack to ensure accurate motion.
            if (!player.getInputData().contains(PlayerAuthInputData.VERTICAL_COLLISION) || box == null) {
                return EMPTY_SHAPE;
            }

            // Workaround.... we can still ensure player won't be having weird y motion on bamboo using the VERTICAL_COLLISION input.
            Box solidOffset = SOLID_SHAPE.get(0).offset(vector3i.getX(), vector3i.getY(), vector3i.getZ());

            // If player claimed to have y collision and their feet/head does hit something then we can be sure it's correct.
            // Also, this bamboo should not collide with player horizontal collision, only vertical so we can handle it properly.
            boolean likelyYCollision = solidOffset.calculateMaxDistance(Axis.Y, player.boundingBox, player.velocity.y) != player.velocity.y;
            return likelyYCollision && solidOffset.intersects(box) ? SOLID_SHAPE : EMPTY_SHAPE;
        }

        if (state.is(Blocks.END_PORTAL_FRAME)) {
            return END_PORTAL_FRAME_SHAPE;
        }

        if (state.is(Blocks.POWDER_SNOW)) {
            boolean leatherBoostOn = player.compensatedInventory.translate(player.compensatedInventory.armorContainer.get(3).getData()).getId() == Items.LEATHER_BOOTS.javaId();
            if (leatherBoostOn && player.position.y > vector3i.getY() + 1 - 1.0E-5f && !(player.getInputData().contains(PlayerAuthInputData.SNEAKING)|| player.getInputData().contains(PlayerAuthInputData.DESCEND_BLOCK))) {
                return SOLID_SHAPE;
            }
        }

        if (state.is(Blocks.ANVIL) || state.is(Blocks.DAMAGED_ANVIL) || state.is(Blocks.CHIPPED_ANVIL)) {
            Direction direction = state.getValue(Properties.HORIZONTAL_FACING);
            if (direction.getAxis() == Axis.X) {
                return ANVIL_X_SHAPE;
            } else {
                return ANVIL_OTHER_SHAPE;
            }
        }

        if (state.is(Blocks.LANTERN) || state.is(Blocks.SOUL_LANTERN)) {
            return LANTERN_SHAPE;
        }

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

        if (state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON) || state.is(Blocks.LAVA_CAULDRON) || state.is(Blocks.POWDER_SNOW_CAULDRON)) {
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
            if (above && !player.getInputData().contains(PlayerAuthInputData.WANT_DOWN)) {
                return SCAFFOLDING_NORMAL_SHAPE;
            } else {
                return EMPTY_SHAPE;
            }
        }

        return null;
    }
}
