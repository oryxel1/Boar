package ac.boar.anticheat.data;

import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.session.cache.tags.BlockTag;

public record FluidState(Fluid fluid, float height, int level) {
    public float getHeight(final BoarPlayer player, final Mutable pos) {
        return isFluidAboveEqual(player, pos) ? 1.0F : this.height();
    }

    private boolean isFluidAboveEqual(BoarPlayer player, Mutable pos) {
        return fluid == player.compensatedWorld.getFluidState(pos.getX(), pos.getY() + 1, pos.getZ()).fluid();
    }

    private boolean affectsFlow(FluidState state) {
        return state.fluid == Fluid.EMPTY || state.fluid.equals(this.fluid);
    }

    public Vec3 getFlow(final BoarPlayer player, final Vector3i vector3i, final FluidState fluidState) {
        float d = 0.0F;
        float e = 0.0F;

        Mutable mutableBlockPos = new Mutable();
        for (Direction direction : Direction.HORIZONTAL) {
            mutableBlockPos.set(vector3i, direction.getUnitVector());
            FluidState fluidState2 = player.compensatedWorld.getFluidState(mutableBlockPos);
            if (!this.affectsFlow(fluidState2)) continue;
            float f = fluidState2.height();
            float g = 0.0f;
            if (f == 0.0f) {
                Vector3i blockPos2 = Vector3i.from(mutableBlockPos.getX(), mutableBlockPos.getY() - 1, mutableBlockPos.getZ());
                FluidState fluidState3;
                if (!player.compensatedWorld.getBlockState(mutableBlockPos, 0).blocksMotion(player)
                        && this.affectsFlow(fluidState3 = player.compensatedWorld.getFluidState(blockPos2)) && (f = fluidState3.height()) > 0.0f) {
                    g = fluidState.height() - (f - 0.8888889f);
                }
            } else if (f > 0.0f) {
                g = fluidState.height() - f;
            }
            if (g == 0.0f) continue;
            d += (direction.getUnitVector().getX() * g);
            e += (direction.getUnitVector().getZ() * g);
        }
        Vec3 vec3 = new Vec3(d, 0, e);

        if (level == 8) {
            for (Direction direction2 : Direction.HORIZONTAL) {
                mutableBlockPos.set(vector3i, direction2.getUnitVector());
                if (!this.isSolidFace(player, mutableBlockPos, direction2) && !this.isSolidFace(player, mutableBlockPos.add(0, 1, 0), direction2)) continue;
                vec3 = vec3.normalize().add(0, -6, 0);
                break;
            }
        }
        return vec3.normalize();
    }

    private boolean isSolidFace(BoarPlayer player, Mutable blockPos, Direction direction) {
        BoarBlockState blockState = player.compensatedWorld.getBlockState(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 0);
        FluidState fluidState = player.compensatedWorld.getFluidState(blockPos);
//        if (fluidState.getType().isSame(this)) {
//            return false;
//        }
        if (direction == Direction.UP) {
            return true;
        }
        if (player.getSession().getTagCache().is(BlockTag.ICE, blockState.getState().block())) {
            return false;
        }
        return blockState.isFaceSturdy(player);
    }
}