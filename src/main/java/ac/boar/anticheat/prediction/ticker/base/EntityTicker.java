package ac.boar.anticheat.prediction.ticker.base;

import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.BlockUtil;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.util.math.Vec3f;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.Fluid;

@RequiredArgsConstructor
public class EntityTicker {
    protected final BoarPlayer player;

    public void tick() {
        this.baseTick();
    }

    public void baseTick() {
        player.wasInPowderSnow = player.inPowderSnow;
        player.inPowderSnow = false;
        this.updateWaterState();
        this.updateSubmergedInWaterState();
        // this.updateSwimming();
    }

    private void updateWaterState() {
        player.fluidHeight.clear();
        this.checkWaterState();
        this.updateMovementInFluid(Fluid.LAVA);
    }

    private void updateSubmergedInWaterState() {
        player.submergedInWater = player.submergedFluidTag.contains(Fluid.WATER);
        player.submergedFluidTag.clear();
//        if (this.getVehicle() instanceof AbstractBoatEntity lv2 && !lv2.isSubmergedInWater() && lv2.getBoundingBox().maxY >= d && lv2.getBoundingBox().minY <= d) {
//            return;
//        }

        final float eyePosition = player.y + player.dimensions.eyeHeight();
        final Mutable mutable = new Mutable(player.x, eyePosition, player.z);
        final FluidState lv4 = player.compensatedWorld.getFluidState(mutable.x, mutable.y, mutable.z);
        float e = mutable.getY() + lv4.getHeight(player, mutable);
        if (e > eyePosition) {
            player.submergedFluidTag.add(lv4.fluid());
        }
    }

    void checkWaterState() {
//        if (this.getVehicle() instanceof AbstractBoatEntity lv && !lv.isSubmergedInWater()) {
//            this.touchingWater = false;
//            return;
//        }

        player.touchingWater = this.updateMovementInFluid(Fluid.WATER);
    }

    private boolean updateMovementInFluid(final Fluid tag) {
        if (player.isRegionUnloaded()) {
            return false;
        }

        final Box lv = player.prevBoundingBox.contract(0.001F);
        int i = GenericMath.floor(lv.minX);
        int j = GenericMath.ceil(lv.maxX);
        int k = GenericMath.floor(lv.minY);
        int l = GenericMath.ceil(lv.maxY);
        int m = GenericMath.floor(lv.minZ);
        int n = GenericMath.ceil(lv.maxZ);
        float e = 0;
        boolean bl2 = false;
        Vec3f lv2 = Vec3f.ZERO;
        int o = 0;
        Mutable lv3 = new Mutable();

        for (int p = i; p < j; p++) {
            for (int q = k; q < l; q++) {
                for (int r = m; r < n; r++) {
                    lv3.set(p, q, r);
                    FluidState lv4 = player.compensatedWorld.getFluidState(lv3.x, lv3.y, lv3.z);
                    if (lv4.fluid() != tag) {
                        continue;
                    }

                    float f = q + lv4.getHeight(player, lv3);
                    if (f < lv.minY) {
                        continue;
                    }

                    bl2 = true;
                    e = Math.max(f - lv.minY, e);
                    Vec3f lv5 = lv4.getVelocity(player, lv3, lv4);
                    if (e < 0.4) {
                        lv5 = lv5.multiply(e);
                    }

                    lv2 = lv2.add(lv5);
                    o++;
                }
            }
        }

        if (lv2.length() > 0.0) {
            if (o > 0) {
                lv2 = lv2.multiply(1.0f / o);
            }

            player.fluidPushingVelocity = lv2.clone();
        } else {
            player.fluidPushingVelocity = Vec3f.ZERO;
        }

        player.fluidHeight.put(tag, e);
        return bl2;
    }

    protected void checkBlockCollision() {
        final Vector3i vector3i = Vector3i.from(player.boundingBox.minX + 0.001D, player.boundingBox.minY + 0.001D, player.boundingBox.minZ + 0.001D);
        final Vector3i vector31i = Vector3i.from(player.boundingBox.maxX - 0.001D, player.boundingBox.maxY - 0.001D, player.boundingBox.maxZ - 0.001D);

        final Mutable mutable = new Mutable(0, 0, 0);
        for (int i = vector3i.getX(); i <= vector31i.getX(); ++i) {
            for (int j = vector3i.getY(); j <= vector31i.getY(); ++j) {
                for (int k = vector3i.getZ(); k <= vector31i.getZ(); ++k) {
                    mutable.set(i, j, k);
                    BlockUtil.onEntityCollision(player, player.compensatedWorld.getBlockState(mutable.x, mutable.y, mutable.z), mutable);
                }
            }
        }
    }
}
