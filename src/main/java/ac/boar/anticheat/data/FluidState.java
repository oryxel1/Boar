package ac.boar.anticheat.data;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.BlockUtil;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.util.math.Vec3f;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.physics.Direction;

public record FluidState(Fluid fluid, float height) {
    public float getHeight(final BoarPlayer player, final Mutable pos) {
        return isFluidAboveEqual(player, pos) ? 1.0F : this.height();
    }

    private boolean isFluidAboveEqual(BoarPlayer player, Mutable pos) {
        return fluid == player.compensatedWorld.getFluidState(pos.x, pos.y + 1, pos.z).fluid();
    }

    private boolean isEmptyOrThis(FluidState state) {
        return state.fluid == Fluid.EMPTY || state.fluid.equals(this.fluid);
    }

    public Vec3f getVelocity(final BoarPlayer player, final Mutable vector3i, final FluidState state) {
        Vec3f lv6 = new Vec3f(0, 0, 0);
        final Mutable mutable = new Mutable(0, 0, 0);
        for (final Direction lv2 : Direction.HORIZONTAL) {
            mutable.set(vector3i.x, vector3i.y, vector3i.z).add(lv2.getUnitVector());
            FluidState lv3 = player.compensatedWorld.getFluidState(mutable.x, mutable.y, mutable.z);
            if (!this.isEmptyOrThis(lv3)) {
                continue;
            }

            float f = lv3.height();
            float g = 0.0F;
            if (f == 0.0F) {
                if (!BlockUtil.blocksMovement(player, mutable, fluid, player.compensatedWorld.getBlockState(mutable.x, mutable.y, mutable.z))) {
                    FluidState lv5 = player.compensatedWorld.getFluidState(mutable.x, mutable.y - 1, mutable.z);
                    if (this.isEmptyOrThis(lv5) && lv5.height() > 0.0F) {
                        g = state.height() - (f - 0.8888889F);
                    }
                }
            } else if (f > 0.0F) {
                g = state.height() - f;
            }

            if (g != 0.0F) {
                lv6.add(lv2.getUnitVector().getX() * g, 0, lv2.getUnitVector().getZ() * g);
            }
        }

        return lv6.length() > 0 ? lv6.normalize() : lv6;
    }
}