package ac.boar.anticheat.util.block;

import ac.boar.anticheat.collision.BedrockCollision;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.session.cache.TagCache;
import org.geysermc.geyser.session.cache.tags.BlockTag;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

import java.util.ArrayList;
import java.util.List;

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

    public static void onEntityLand(final boolean living, final BoarPlayer player, final BlockState state) {
        final TagCache cache = player.getSession().getTagCache();

        if (cache.is(BlockTag.BEDS, state.block()) && player.velocity.y < 0.0 && !player.sneaking) {
            final float d = living ? 1.0F : 0.8F;
            player.velocity.y = -player.velocity.y * 0.75F * d;
            if (player.velocity.y > 0.75) {
                player.velocity.y = 0.75F;
            }

            return;
        }

        player.velocity.y = 0;
    }

    public static void onSteppedOn(final BoarPlayer player, final BlockState state, final Vector3i vector3i) {
        if (state.is(Blocks.HONEY_BLOCK)) {
            // Yes lol.
            float d = Math.abs(player.velocity.y);
            if (d < 0.1 && !player.sneaking) {
                float e = 0.4F + d * 0.2F;
                player.velocity = player.velocity.multiply(e, 1, e);
            }
        }
    }

    public static void onEntityCollision(final BoarPlayer player, BlockState state, Mutable pos) {
        if (state.is(Blocks.BUBBLE_COLUMN)) {
            boolean drag = state.getValue(Properties.DRAG);

            final Vec3 lv = player.velocity;
            if (player.compensatedWorld.getBlockAt(pos.getX(), pos.getY() + 1, pos.getZ()) == Block.JAVA_AIR_ID) {
                if (drag) {
                    lv.y = Math.max(-0.9F, lv.y - 0.03F);
                } else {
                    lv.y = Math.min(1.8F, lv.y + 0.1F);
                }
            } else {
                if (drag) {
                    lv.y = Math.max(-0.3F, lv.y - 0.03F);
                } else {
                    lv.y = Math.min(0.7F, lv.y + 0.06F);
                }
            }
        }

        Vec3 movementMultiplier = Vec3.ZERO;
        if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            movementMultiplier = new Vec3(0.8F, 0.75F, 0.8F);
        } else if (state.is(Blocks.POWDER_SNOW)) {
            movementMultiplier = new Vec3(0.9F, 1.5F, 0.9F);
        } else if (state.is(Blocks.COBWEB)) {
            movementMultiplier = new Vec3(0.25F, 0.05F, 0.25F);
            if (player.hasEffect(Effect.WEAVING)) {
                movementMultiplier = new Vec3(0.5F, 0.25F, 0.5F);
            }
        }

        if (movementMultiplier.equals(Vec3.ZERO)) {
            return;
        }

        final boolean xLargerThanThreshold = Math.abs(player.stuckSpeedMultiplier.x) >= 1.0E-7;
        final boolean yLargerThanThreshold = Math.abs(player.stuckSpeedMultiplier.y) >= 1.0E-7;
        final boolean zLargerThanThreshold = Math.abs(player.stuckSpeedMultiplier.z) >= 1.0E-7;
        if (xLargerThanThreshold || yLargerThanThreshold || zLargerThanThreshold) {
            player.stuckSpeedMultiplier.x = Math.min(player.stuckSpeedMultiplier.x, movementMultiplier.x);
            player.stuckSpeedMultiplier.y = Math.min(player.stuckSpeedMultiplier.y, movementMultiplier.y);
            player.stuckSpeedMultiplier.z = Math.min(player.stuckSpeedMultiplier.z, movementMultiplier.z);
        } else {
            player.stuckSpeedMultiplier = movementMultiplier;
        }
    }

    public static boolean blocksMovement(BoarPlayer player, Mutable vector3i, Fluid fluid, BlockState state) {
        if (state.is(Blocks.ICE)) {
            return false;
        }

        if (BlockStateValues.getFluid(state.javaId()) == fluid) {
            return false;
        }

        return !state.is(Blocks.COBWEB) && !state.is(Blocks.BAMBOO_SAPLING) && isSolid(player, state, vector3i);
    }

    public static boolean isSolid(BoarPlayer player, BlockState state, Mutable vector3i) {
        List<Box> boxes = getBlockBoundingBoxes(player, state, vector3i);
        if (boxes.isEmpty()) {
            return false;
        } else {
            Box box = new Box(0, 0, 0, 0, 0, 0);
            for (Box box1 : boxes) {
                box = box1.union(box);
            }

            return box.getAverageSideLength() >= 0.7291666666666666 || box.getLengthY() >= 1.0;
        }
    }

    public static List<Box> getBlockBoundingBoxes(BoarPlayer player, BlockState state, Mutable vector3i) {
        List<Box> boxes = BedrockCollision.getCollisionBox(player, vector3i, state);
        if (boxes != null) {
            return boxes;
        }

        List<Box> boxes1 = new ArrayList<>();
        BlockCollision collision = BlockUtils.getCollision(state.javaId());
        if (collision == null) {
            return List.of();
        }

        for (final BoundingBox geyserBox : collision.getBoundingBoxes()) {
            boxes1.add(new Box(geyserBox));
        }

        return boxes1;
    }

    public static float getBlockJumpFactor(BlockState state) {
        if (state.is(Blocks.HONEY_BLOCK)) {
            return 0.6F;
        }

        return 1F;
    }

    public static float getBlockSlipperiness(final BlockState state) {
        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.FROSTED_ICE)) {
            return 0.98F;
        } else if (state.is(Blocks.SLIME_BLOCK) || state.is(Blocks.HONEY_BLOCK)) {
            return 0.8F;
        } else if (state.is(Blocks.BLUE_ICE)) {
            return 0.989F;
        }

        return 0.6F;
    }
}