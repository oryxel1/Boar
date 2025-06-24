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
        player.inBlockState = null;

        this.updateWaterState();
        // this.updateSubmergedInWaterState();
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

        Box box = player.boundingBox.expand(0, -0.4F, 0).contract(0.001F);

        int i = GenericMath.floor(box.minX);
        int j = GenericMath.floor(box.maxX + 1.0D);
        int k = GenericMath.floor(box.minY);
        int l = GenericMath.floor(box.maxY + 1.0D);
        int i1 = GenericMath.floor(box.minZ);
        int j1 = GenericMath.floor(box.maxZ + 1.0D);

        boolean found = false;
        Vec3 vec3 = new Vec3(0, 0, 0);
        float maxFluidHeight = 0;
        Mutable mutable = new Mutable();

        for (int k1 = i; k1 < j; ++k1) { for (int l1 = k; l1 < l; ++l1) { for (int i2 = i1; i2 < j1; ++i2) {
            mutable.set(k1, l1, i2);
            FluidState fluidState = player.compensatedWorld.getFluidState(mutable);

            if (fluidState.fluid() == tag) {
                float d0 = l1 + 1 - fluidState.height();
                maxFluidHeight = Math.max(maxFluidHeight, d0);

                if (l >= d0) {
                    found = true;
                    vec3 = fluidState.getFlow(player, Vector3i.from(k1, l1, i2));
                }
            }
        }}}

        if (vec3.lengthSquared() > 0.0D) {
            vec3 = vec3.normalize();
            player.velocity = player.velocity.add(vec3.multiply(speed));
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