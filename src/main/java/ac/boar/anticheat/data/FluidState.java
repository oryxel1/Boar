package ac.boar.anticheat.data;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.util.math.Vec3;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.physics.Direction;

public record FluidState(Fluid fluid, float height) {
    public float getHeight(final BoarPlayer player, final Mutable pos) {
        return isFluidAboveEqual(player, pos) ? 1.0F : this.height();
    }

    private boolean isFluidAboveEqual(BoarPlayer player, Mutable pos) {
        return fluid == player.compensatedWorld.getFluidState(pos.getX(), pos.getY() + 1, pos.getZ()).fluid();
    }

    private boolean isEmptyOrThis(FluidState state) {
        return state.fluid == Fluid.EMPTY || state.fluid.equals(this.fluid);
    }

    public Vec3 getVelocity(final BoarPlayer player, final Mutable vector3i, final FluidState state) {
        Vec3 lv6 = Vec3.ZERO;
        final Mutable mutable = new Mutable();
        for (final Direction lv2 : Direction.HORIZONTAL) {
            mutable.set(vector3i.getX(), vector3i.getY(), vector3i.getZ()).add(lv2.getUnitVector());
            FluidState lv3 = player.compensatedWorld.getFluidState(mutable);
            if (!this.isEmptyOrThis(lv3)) {
                continue;
            }

            float f = lv3.height();
            float g = 0.0F;
            if (f == 0.0F) {
                if (player.compensatedWorld.getBlockState(mutable.getX(), mutable.getY(), mutable.getZ(), 0).blocksMovement(
                        player, mutable, fluid)) {
                    FluidState lv5 = player.compensatedWorld.getFluidState(mutable.getX(), mutable.getY() - 1, mutable.getZ());
                    if (this.isEmptyOrThis(lv5) && lv5.height() > 0.0F) {
                        g = state.height() - (f - 0.8888889F);
                    }
                }
            } else if (f > 0.0F) {
                g = state.height() - f;
            }

            if (g != 0.0F) {
                lv6 = lv6.add(lv2.getUnitVector().getX() * g, 0, lv2.getUnitVector().getZ() * g);
            }
        }

        return lv6.length() > 0 ? lv6.normalize() : lv6;
    }
}