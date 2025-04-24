package ac.boar.anticheat.prediction.ticker.base;

import ac.boar.anticheat.collision.Collider;
import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.util.math.Vec3;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.level.block.Fluid;

@RequiredArgsConstructor
public class EntityTicker {
    protected final BoarPlayer player;

    public void tick() {
        this.baseTick();
    }

    public void baseTick() {
        player.inBlockState = null;

        player.wasInPowderSnow = player.inPowderSnow;
        player.inPowderSnow = false;
        this.updateWaterState();
        this.updateSubmergedInWaterState();
        this.updateSwimming();
    }

    private void updateSwimming() {
        if (player.getFlagTracker().has(EntityFlag.SWIMMING)) {
            player.getFlagTracker().set(EntityFlag.SWIMMING, player.touchingWater && player.vehicleData == null);
        }
    }

    private void updateWaterState() {
        player.fluidHeight.clear();
        this.checkWaterState();

        // TODO: lava prediction.
        this.updateFluidHeightAndDoFluidPushing(0F, Fluid.LAVA);
    }

    private void updateSubmergedInWaterState() {
        player.submergedInWater = player.submergedFluidTag.contains(Fluid.WATER);
        player.submergedFluidTag.clear();
//        if (this.getVehicle() instanceof AbstractBoatEntity lv2 && !lv2.isSubmergedInWater() && lv2.getBoundingBox().maxY >= d && lv2.getBoundingBox().minY <= d) {
//            return;
//        }

        final float eyePosition = player.position.y + player.dimensions.eyeHeight();
        final Mutable mutable = new Mutable(player.position.x, eyePosition, player.position.z);
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

        player.touchingWater = this.updateFluidHeightAndDoFluidPushing(0.014F, Fluid.WATER);
    }

    private boolean updateFluidHeightAndDoFluidPushing(final float speed, final Fluid tag) {
        if (player.isRegionUnloaded()) {
            return false;
        }

        boolean found = false;
        Vec3 velocity = Vec3.ZERO;
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

            Vec3 lv5 = state.getVelocity(player, mutable, state);
//            if (maxFluidHeight < 0.4) {
//                lv5 = lv5.multiply(maxFluidHeight);
//            }
            velocity = velocity.add(lv5);

            fluidCount++;
        }

        if (velocity.length() > 0.0 && fluidCount > 0) {
            velocity = velocity.multiply(1.0f / fluidCount);
        }

        // Fluid pushing is broken as shit.
        // player.velocity = player.velocity.add(velocity.multiply(speed));
        player.fluidHeight.put(tag, maxFluidHeight);

        return found;
    }

    protected void applyEffectsFromBlocks() {
        if (player.onGround) {
            final Vector3i lv = player.getOnPos(0.2F);
            player.compensatedWorld.getBlockState(lv, 0).onSteppedOn(player, lv);
        }

        final BlockPositionIterator iterator = getBlockPositionIterator();

        final Mutable mutable = new Mutable();
        for (iterator.reset(); iterator.hasNext(); iterator.next()) {
            mutable.set(iterator.getX(), iterator.getY(), iterator.getZ());
            player.compensatedWorld.getBlockState(iterator.getX(), iterator.getY(), iterator.getZ(), 0).entityInside(player, mutable);
        }
    }

    public final void doSelfMove(Vec3 vec3) {
        if (player.stuckSpeedMultiplier.lengthSquared() > 1.0E-7) {
            vec3 = vec3.multiply(player.stuckSpeedMultiplier);
            player.stuckSpeedMultiplier = Vec3.ZERO;
            player.velocity = Vec3.ZERO.clone();
        }

        Vec3 vec32 = Collider.collide(player, vec3 = Collider.maybeBackOffFromEdge(player, vec3));
        player.setPos(player.prevUnvalidatedPosition.add(vec32));

        boolean bl = !MathUtil.equal(vec3.x, vec32.x);
        boolean bl2 = !MathUtil.equal(vec3.z, vec32.z);
        player.horizontalCollision = bl || bl2;
        player.verticalCollision = vec3.y != vec32.y;
        player.onGround = player.verticalCollision && vec3.y < 0.0;
        // this.minorHorizontalCollision = this.horizontalCollision ? this.isHorizontalCollisionMinor(vec32) : false;
        Vector3i blockPos = player.getOnPos(0.2F + 1.0E-5F);
        BoarBlockState blockState = player.compensatedWorld.getBlockState(blockPos, 0);
//        if (this.isLocalInstanceAuthoritative()) {
//            this.checkFallDamage(vec32.y, this.onGround(), blockState, blockPos);
//        }
        if (player.horizontalCollision) {
            player.velocity = new Vec3(bl ? 0 : player.velocity.x, player.velocity.y, bl2 ? 0 : player.velocity.z);
        }

        if (player.verticalCollision) {
            blockState.updateEntityMovementAfterFallOn(player, true);
        }

        float f = player.getBlockSpeedFactor();
        player.velocity = player.velocity.multiply(f, 1, f);

        player.beforeCollision = vec3.clone();
        player.afterCollision = vec32.clone();
    }

    private BlockPositionIterator getBlockPositionIterator() {
        final Box box = player.boundingBox;
        return BlockPositionIterator.fromMinMax(
                Math.min(GenericMath.floor(box.minX + 0.001), GenericMath.floor(box.minX - 0.001)),
                Math.min(GenericMath.floor(box.minY + 0.001), GenericMath.floor(box.minY - 0.001)),
                Math.min(GenericMath.floor(box.minZ + 0.001), GenericMath.floor(box.minZ - 0.001)),
                GenericMath.floor(box.maxX - 0.001), GenericMath.floor(box.maxY - 0.001), GenericMath.floor(box.maxZ - 0.001));
    }
}