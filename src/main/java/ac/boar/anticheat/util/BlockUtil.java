package ac.boar.anticheat.util;

import ac.boar.anticheat.collision.BedrockCollision;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.util.math.Vec3f;
import org.geysermc.geyser.level.block.BlockStateValues;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.util.BlockUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

import java.util.ArrayList;
import java.util.List;

public class BlockUtil {
    public static void onEntityCollision(final BoarPlayer player, BlockState state, Mutable pos) {
        if (state.is(Blocks.SWEET_BERRY_BUSH)) {
            player.movementMultiplier = new Vec3f(0.8F, 0.75F, 0.8F);
        } else if (state.is(Blocks.POWDER_SNOW)) {
            player.movementMultiplier = new Vec3f(0.9F, 1.5F, 0.9F);
        } else if (state.is(Blocks.COBWEB)) {
            player.movementMultiplier = new Vec3f(0.25F, 0.05F, 0.25F);
            if (player.hasStatusEffect(Effect.WEAVING)) {
                player.movementMultiplier = new Vec3f(0.5F, 0.25F, 0.5F);
            }
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

    public static float getVelocityMultiplier(BlockState state) {
        if (state.is(Blocks.SOUL_SAND) || state.is(Blocks.HONEY_BLOCK)) {
            return 0.4F;
        }

        return 1F;
    }

    public static float getJumpVelocityMultiplier(BlockState state) {
        if (state.is(Blocks.HONEY_BLOCK)) {
            return 0.6F;
        }

        return 1F;
    }

    public static float getBlockSlipperiness(BlockState state) {
        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.FROSTED_ICE)) {
            return 0.98F;
        } else if (state.is(Blocks.SLIME_BLOCK)) {
            return 0.8F;
        } else if (state.is(Blocks.BLUE_ICE)) {
            return 0.989F;
        }

        return 0.6F;
    }
}