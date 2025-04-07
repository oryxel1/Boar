package ac.boar.anticheat.compensated.world;

import ac.boar.anticheat.collision.util.CuboidBlockIterator;
import ac.boar.anticheat.compensated.world.base.CompensatedWorld;
import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.block.BlockUtil;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Mutable;
import com.google.common.collect.ImmutableList;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;

import java.util.ArrayList;
import java.util.List;

public class CompensatedWorldImpl extends CompensatedWorld {
    public CompensatedWorldImpl(BoarPlayer player) {
        super(player);
    }

    public boolean noCollision(Box aabb) {
        return this.collectColliders(this.getEntityCollisions(aabb), aabb).isEmpty();
    }

    public FluidState getFluidState(final Vector3i vec3) {
        return this.getFluidState(vec3.getX(), vec3.getY(), vec3.getZ());
    }

    public FluidState getFluidState(final Mutable mutable) {
        return this.getFluidState(mutable.getX(), mutable.getY(), mutable.getZ());
    }

    public FluidState getFluidState(int x, int y, int z) {
        final int blockIdLayer0 = this.toJavaId(getBlockAt(x, y, z, 0));
        final int blockIdLayer1 = this.toJavaId(getBlockAt(x, y, z, 1));
        float waterHeight = BlockUtil.getWorldFluidHeight(Fluid.WATER, blockIdLayer0);
        float lavaHeight = BlockUtil.getWorldFluidHeight(Fluid.LAVA, blockIdLayer0);

        boolean waterlogged = blockIdLayer1 == Blocks.WATER.javaId();
        if (waterlogged) {
            return new FluidState(Fluid.WATER, 1);
        }

        if (waterHeight <= 0 && lavaHeight <= 0) {
            return new FluidState(Fluid.EMPTY, 0);
        }

        if (waterHeight > 0) {
            return new FluidState(Fluid.WATER, waterHeight);
        }

        return new FluidState(Fluid.LAVA, lavaHeight);
    }

    public List<Box> collectColliders(List<Box> list, Box aABB) {
        ImmutableList.Builder<Box> builder = ImmutableList.builderWithExpectedSize(list.size() + 1);
        if (!list.isEmpty()) {
            builder.addAll(list);
        }

        // Bedrock void limit.
//        if (aABB.minY - 1 == this.getDimension().minY() - 40) {
//            builder.add(new AABB(this.getPlayer().position.x, this.getPlayer().position.y - 1, this.getPlayer().position.z, this.getPlayer().position.x + 1, this.getPlayer().position.y, this.getPlayer().position.z + 1));
//        }

        final CuboidBlockIterator iterator = CuboidBlockIterator.iterator(aABB);
        while (iterator.step()) {
            int x = iterator.getX(), y = iterator.getY(), z = iterator.getZ();
            if (this.isChunkLoaded(x, z)) {
                builder.addAll(this.getBlockState(x, y, z, 0).findCollision(this.getPlayer(), Vector3i.from(x, y, z), aABB, true));

                // This is geyser so this line is not needed for normal bedrock server Ig feel free to uncomment this line.
                // builder.addAll(this.getBlockState(x, y, z, 1).findCollision(this.getPlayer(), Vector3i.from(x, y, z), aABB));
            }
        }
        return builder.build();
    }

    // TODO: Implement this.
    public List<Box> getEntityCollisions(Box aABB) {
        return new ArrayList<>();

//        if (aABB.getSize() < 1.0E-7) {
//            return List.of();
//        }
//        Predicate<Entity> predicate = entity == null ? EntitySelector.CAN_BE_COLLIDED_WITH : EntitySelector.NO_SPECTATORS.and(entity::canCollideWith);
//        List<Entity> list = this.getEntities(entity, aABB.inflate(1.0E-7), predicate);
//        if (list.isEmpty()) {
//            return List.of();
//        }
//        ImmutableList.Builder builder = ImmutableList.builderWithExpectedSize(list.size());
//        for (Entity entity2 : list) {
//            builder.add(Shapes.create(entity2.getBoundingBox()));
//        }
//        return builder.build();
    }

    public boolean hasChunksAt(int i, int j, int k, int l) {
        for (int q = i; q <= k; ++q) {
            for (int r = j; r <= l; ++r) {
                if (this.isChunkLoaded(q, r)) continue;
                return false;
            }
        }
        return true;
    }
}