package ac.boar.anticheat.prediction.ticker.base;

import ac.boar.anticheat.collision.Collider;
import ac.boar.anticheat.collision.util.CuboidBlockIterator;
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
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.erosion.util.BlockPositionIterator;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.block.Blocks;
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

        player.soulSandBelow = player.compensatedWorld.getBlockState(player.getOnPos(1.0E-3F), 0).getState().is(Blocks.SOUL_SAND);
    }

    private void updateSwimming() {
        if (player.getFlagTracker().has(EntityFlag.SWIMMING)) {
            player.getFlagTracker().set(EntityFlag.SWIMMING, player.touchingWater && player.vehicleData == null);
        }
    }

    private void updateWaterState() {
        player.fluidHeight.clear();
        this.checkWaterState();
        this.updateFluidHeightAndDoFluidPushing(player.compensatedWorld.getDimension().bedrockId() ==
                BedrockDimension.DEFAULT_NETHER_ID ? 0.007F : 0.0023333333333333335F, Fluid.LAVA);
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

        player.submergedFluidTag.remove(Fluid.EMPTY); // not needed lol.
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

        float maxFluidHeight = 0.0F;
        boolean bl = /* this.isPushedByFluid(); */ true;
        boolean found = false;
        Vec3 fluidPushVelocity = Vec3.ZERO;
        int fluidCount = 0;

        Mutable mutable = new Mutable();

        final CuboidBlockIterator iterator = CuboidBlockIterator.iterator(player.boundingBox);
        while (iterator.step()) {
            int x = iterator.getX(), y = iterator.getY(), z = iterator.getZ();
            if (!player.compensatedWorld.isChunkLoaded(x, z)) {
                continue;
            }
            mutable.set(x, y, z);
            FluidState fluidState = player.compensatedWorld.getFluidState(mutable);
            if (fluidState.fluid() != tag) {
                continue;
            }

            float height = y + fluidState.getHeight(player, mutable);

            if (!player.boundingBox.intersects(x, y, z, x + 1, y + 1, z + 1)) {
                continue;
            }

            float distanceToLeg = height - player.boundingBox.minY;

            System.out.println("Info: " + fluidState.level() + ":" + distanceToLeg);
            // YES, I know, it's a "bedrock" prediction engine, what are you expecting.
            if (!player.getFlagTracker().has(EntityFlag.SWIMMING)) {
                switch (fluidState.level()) {
                    case 8, 0 -> {if (distanceToLeg < 0.29F) continue;}
                    case 7 -> {if (distanceToLeg < 0.2F - 5.0E-3) continue;}
                    case 6 -> {if (distanceToLeg < 0.07) continue;}
                    case 5 -> {if (distanceToLeg < 0 - 3.0E-3) continue;}
                    case 4 -> {if (distanceToLeg < -0.15) {continue;}}
                    case 3 -> {if (distanceToLeg < -0.25 - 3.0E-3) continue;}
                    case 2 -> {if (distanceToLeg < -0.35 - 3.0E-3) continue;}
                    case 1 -> {if (distanceToLeg < -0.45 - 3.0E-3) continue;}
                    default -> {}
                }
            }

            found = true;
            maxFluidHeight = Math.max(Math.abs(height - player.boundingBox.minY), maxFluidHeight);
            if (!bl) continue;
            Vec3 vec32 = fluidState.getFlow(player, Vector3i.from(x, y, z), fluidState);

            player.affectedByFluidPushing = vec32.lengthSquared() > 0;
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

            player.guessedFluidPushingVelocity = new Vec3(Math.max(Math.abs(fluidPushVelocity.x), player.guessedFluidPushingVelocity.x), Math.max(Math.abs(fluidPushVelocity.y), player.guessedFluidPushingVelocity.y), Math.max(Math.abs(fluidPushVelocity.z), player.guessedFluidPushingVelocity.z));
        }
        player.fluidHeight.put(tag, maxFluidHeight);

        return found;
    }

    private boolean affectedByFluid(Fluid tag) {
        Box box = player.boundingBox.contract(0.001F);

        Mutable mutable = new Mutable();

        int i = GenericMath.floor(box.minX);
        int j = GenericMath.ceil(box.maxX);
        int k = GenericMath.floor(box.minY);
        int l = GenericMath.ceil(box.maxY);
        int m = GenericMath.floor(box.minZ);
        int n = GenericMath.ceil(box.maxZ);
        for (int p = i; p < j; ++p) {
            for (int q = k; q < l; ++q) {
                for (int r = m; r < n; ++r) {
                    FluidState fluidState = player.compensatedWorld.getFluidState(p, q, r);
                    float f = (float) q + fluidState.getHeight(player, mutable);
                    if (fluidState.fluid() != (tag) || !(f >= box.minY)) continue;
                    Vec3 vec32 = fluidState.getFlow(player, Vector3i.from(p, q, r), fluidState);

                    if (vec32.lengthSquared() > 0) {
                        return true;
                    }
                }
            }
        }

        return false;
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

        if (player.horizontalCollision) {
            player.velocity = new Vec3(bl ? 0 : player.velocity.x, player.velocity.y, bl2 ? 0 : player.velocity.z);
        }

        // TODO: What the actual value actually? Player still able to bounce on slime despite being .375 block higher.
        Vector3i blockPos = Vector3i.from((int) player.position.x, GenericMath.floor(player.position.y - 0.378F), (int) player.position.z);
        BoarBlockState blockState = player.compensatedWorld.getBlockState(blockPos, 0);

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