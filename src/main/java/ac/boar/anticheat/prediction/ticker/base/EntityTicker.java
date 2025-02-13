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
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.BlockState;

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
        final FluidState lv4 = player.compensatedWorld.getFluidState(mutable);
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

        final Box box = player.boundingBox.expand(0, -0.3F, 0).contract(0.001F);
        final BlockPositionIterator iterator = BlockPositionIterator.fromMinMax(
                GenericMath.floor(box.minX), GenericMath.floor(box.minY), GenericMath.floor(box.minZ),
                GenericMath.floor(box.maxX), GenericMath.floor(box.maxY), GenericMath.floor(box.maxZ));

        final Mutable mutable = new Mutable();
        for (iterator.reset(); iterator.hasNext(); iterator.next()) {
            mutable.set(iterator.getX(), iterator.getY(), iterator.getZ());
            final FluidState state = player.compensatedWorld.getFluidState(mutable);

            if (state.fluid() != tag) {
                continue;
            }

            float height = mutable.getY() + state.getHeight(player, mutable);
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

        player.eotVelocity = player.eotVelocity.add(velocity.multiply(speed));
        player.fluidHeight.put(tag, maxFluidHeight);

        return found;
    }

    protected void tickBlockCollision() {
        if (player.onGround) {
            final Vector3i lv = player.getPosWithYOffset(false, 0.2F);
            final BlockState lv2 = player.compensatedWorld.getBlockState(lv);
            BlockUtil.onSteppedOn(player, lv2, lv);
        }

        final Box box = player.boundingBox;
        final BlockPositionIterator iterator = BlockPositionIterator.fromMinMax(
                GenericMath.floor(box.minX + 0.001), GenericMath.floor(box.minY + 0.001), GenericMath.floor(box.minZ + 0.001),
                GenericMath.floor(box.maxX - 0.001), GenericMath.floor(box.maxY - 0.001), GenericMath.floor(box.maxZ - 0.001));

        final Mutable mutable = new Mutable();
        for (iterator.reset(); iterator.hasNext(); iterator.next()) {
            mutable.set(iterator.getX(), iterator.getY(), iterator.getZ());
            BlockUtil.onEntityCollision(player, player.compensatedWorld.getBlockState(mutable), mutable);
        }
    }
}
