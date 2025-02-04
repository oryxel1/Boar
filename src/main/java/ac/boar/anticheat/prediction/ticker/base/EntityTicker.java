package ac.boar.anticheat.prediction.ticker.base;

import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.BlockUtil;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.util.math.Vec3f;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.erosion.util.BlockPositionIterator;
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

        // TODO: lava prediction.
        this.updateMovementInFluid(0F, Fluid.LAVA);
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

        player.touchingWater = this.updateMovementInFluid(0.014F, Fluid.WATER);
    }

    private boolean updateMovementInFluid(final float speed, final Fluid tag) {
        if (player.isRegionUnloaded()) {
            return false;
        }

        boolean found = false;
        Vec3f velocity = Vec3f.ZERO;
        float maxFluidHeight = 0;
        int fluidCount = 0;

        final Box box = player.prevBoundingBox.contract(0.001F);
        final BlockPositionIterator iterator = BlockPositionIterator.fromMinMax(
                GenericMath.floor(box.minX), GenericMath.floor(box.minY), GenericMath.floor(box.minZ),
                GenericMath.floor(box.maxX), GenericMath.floor(box.maxY), GenericMath.floor(box.maxZ));

        final Mutable mutable = new Mutable();
        for (iterator.reset(); iterator.hasNext(); iterator.next()) {
            mutable.set(iterator.getX(), iterator.getY(), iterator.getZ());
            final FluidState state = player.compensatedWorld.getFluidState(mutable.x, mutable.y, mutable.z);

            if (state.fluid() != tag) {
                continue;
            }

            float height = mutable.y + state.getHeight(player, mutable);
            if (height < box.minY) {
                continue;
            }

            found = true;
            maxFluidHeight = Math.max(height - box.minY, maxFluidHeight);

            Vec3f lv5 = state.getVelocity(player, mutable, state);
//            if (maxFluidHeight < 0.4) {
//                lv5 = lv5.multiply(maxFluidHeight);
//            }
            velocity = velocity.add(lv5);

            fluidCount++;
        }

        if (velocity.length() > 0.0 && fluidCount > 0) {
            velocity = velocity.multiply(1.0f / fluidCount);
        }

        if (tag == Fluid.WATER) {
            Bukkit.broadcastMessage("push: " + velocity.toVector3f());
        }

        player.eotVelocity = player.eotVelocity.add(velocity.multiply(speed));
        player.fluidHeight.put(tag, maxFluidHeight);

        return found;
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
