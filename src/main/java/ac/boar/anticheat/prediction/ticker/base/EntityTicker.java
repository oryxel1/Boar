package ac.boar.anticheat.prediction.ticker.base;

import ac.boar.anticheat.collision.Collider;
import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.ticker.impl.PlayerTicker;
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
        player.affectedByFluidPushing = false;
        player.guessedFluidPushingVelocity = Vec3.ZERO;

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

        final Box box = player.boundingBox.expand(0, -0.3F, 0).contract(0.001F);
        final BlockPositionIterator iterator = BlockPositionIterator.fromMinMax(
                GenericMath.floor(box.minX), GenericMath.floor(box.minY), GenericMath.floor(box.minZ),
                GenericMath.ceil(box.maxX), GenericMath.ceil(box.maxY), GenericMath.ceil(box.maxZ));

        float maxFluidHeight = 0.0F;
        boolean bl = /* this.isPushedByFluid(); */ true;
        boolean found = false;
        Vec3 fluidPushVelocity = Vec3.ZERO;
        int fluidCount = 0;

        Mutable mutable = new Mutable();
        for (iterator.reset(); iterator.hasNext(); iterator.next()) {
            mutable.set(iterator.getX(), iterator.getY(), iterator.getZ());

            float f;
            FluidState fluidState = player.compensatedWorld.getFluidState(iterator.getX(), iterator.getY(), iterator.getZ());
            if (fluidState.fluid() != (tag) || !((f = (float)iterator.getY() + fluidState.getHeight(player, mutable)) >= box.minY)) continue;
            found = true;
            maxFluidHeight = Math.max(f - box.minY, maxFluidHeight);
            if (!bl) continue;
            Vec3 vec32 = fluidState.getFlow(player, Vector3i.from(iterator.getX(), iterator.getY(), iterator.getZ()), fluidState);
//            if (maxFluidHeight < 0.4) {
//                vec32 = vec32.multiply(maxFluidHeight);
//            }

            player.affectedByFluidPushing = true;
            fluidPushVelocity = fluidPushVelocity.add(vec32);
            ++fluidCount;
        }

        if (fluidPushVelocity.length() > 0.0) {
            if (fluidCount > 0) {
                fluidPushVelocity = fluidPushVelocity.multiply(1.0F / fluidCount);
            }
            if (!(this instanceof PlayerTicker)) {
                fluidPushVelocity = fluidPushVelocity.normalize();
            }
            fluidPushVelocity = fluidPushVelocity.multiply(speed);
//            if (Math.abs(vec33.x) < 0.003 && Math.abs(vec33.z) < 0.003 && fluidPushVelocity.length() < 0.0045000000000000005) {
//                fluidPushVelocity = fluidPushVelocity.normalize().scale(0.0045000000000000005);
//            }

            // player.velocity = player.velocity.add(fluidPushVelocity); // broken lol...

            player.guessedFluidPushingVelocity = new Vec3(
                    Math.max(Math.abs(fluidPushVelocity.x), player.guessedFluidPushingVelocity.x),
                    Math.max(Math.abs(fluidPushVelocity.y), player.guessedFluidPushingVelocity.y),
                    Math.max(Math.abs(fluidPushVelocity.z), player.guessedFluidPushingVelocity.z));
        }
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
        player.setPos(player.position.add(vec32));

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