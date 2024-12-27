package ac.boar.anticheat.compensated;

import ac.boar.anticheat.compensated.cache.BoarChunk;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;

// https://github.com/GeyserMC/Geyser/blob/master/core/src/main/java/org/geysermc/geyser/session/cache/ChunkCache.java
@RequiredArgsConstructor
public class CompensatedWorld {
    private final BoarPlayer player;
    private final Long2ObjectMap<BoarChunk> chunks = new Long2ObjectOpenHashMap<>();

    @Getter
    private int minY;
    private int heightY;

    public void addToCache(int x, int z, DataPalette[] chunks, long id) {
        long chunkPosition = MathUtils.chunkPositionToLong(x, z);
        BoarChunk geyserChunk = new BoarChunk(chunks, id);
        this.chunks.put(chunkPosition, geyserChunk);
    }

    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        final BoarChunk chunk = this.getChunk(chunkX >> 4, chunkZ >> 4);
        return chunk != null && chunk.id() <= player.lastReceivedId;
    }

    private BoarChunk getChunk(int chunkX, int chunkZ) {
        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public void updateBlock(final Vector3i vector3i, int block) {
        this.updateBlock(vector3i.getX(), vector3i.getY(), vector3i.getZ(), block);
    }

    public void updateBlock(int x, int y, int z, int block) {
        final BoarChunk chunk = this.getChunk(x >> 4, z >> 4);
        if (chunk == null) {
            return;
        }

        if (y < minY || ((y - minY) >> 4) > chunk.sections().length - 1) {
            // Y likely goes above or below the height limit of this world
            return;
        }

        DataPalette palette = chunk.sections()[(y - minY) >> 4];
        if (palette == null) {
            if (block != Block.JAVA_AIR_ID) {
                // A previously empty chunk, which is no longer empty as a block has been added to it
                palette = DataPalette.createForChunk();
                // Fixes the chunk assuming that all blocks is the `block` variable we are updating. /shrug
                palette.getPalette().stateToId(Block.JAVA_AIR_ID);
                chunk.sections()[(y - minY) >> 4] = palette;
            } else {
                // Nothing to update
                return;
            }
        }

        palette.set(x & 0xF, y & 0xF, z & 0xF, block);
    }

    public boolean isRegionLoaded(int minX, int minZ, int maxX, int maxZ) {
        int m = minX >> 4;
        int n = maxX >> 4;
        int o = minZ >> 4;
        int p = maxZ >> 4;

        for (int q = m; q <= n; q++) {
            for (int r = o; r <= p; r++) {
                if (!this.isChunkLoaded(q, r)) {
                    return false;
                }
            }
        }

        return true;
    }

    public FluidState getFluidState(Vector3i vector3i) {
        return getFluidState(vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }

    public FluidState getFluidState(int x, int y, int z) {
        final BlockState state = getBlockState(x, y, z);
        boolean lava = state.is(Blocks.LAVA), water = state.is(Blocks.WATER), waterlogged = state.getValue(Properties.WATERLOGGED) != null && state.getValue(Properties.WATERLOGGED);
        if (!lava && !water && !waterlogged) {
            return new FluidState(Fluid.EMPTY, 0);
        }
        if (lava || water) {
            return new FluidState(lava ? Fluid.LAVA : Fluid.WATER, Math.max(8 - state.getValue(Properties.LEVEL), 0) / 9.0F);
        }

        return new FluidState(Fluid.WATER, 8 / 9.0F);
    }

    public BlockState getBlockState(Vector3i vector3i) {
        return getBlockState(vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }

    public BlockState getBlockState(int x, int y, int z) {
        return BlockState.of(getBlockAt(x, y, z));
    }

    public BlockState getBlockState(double x, double y, double z) {
        return BlockState.of(getBlockAt(GenericMath.floor(x), GenericMath.floor(y), GenericMath.floor(z)));
    }

    public int getBlockAt(int x, int y, int z) {
        BoarChunk column = this.getChunk(x >> 4, z >> 4);
        if (column == null) {
            return Block.JAVA_AIR_ID;
        }

        if (y < minY || ((y - minY) >> 4) > column.sections().length - 1) {
            // Y likely goes above or below the height limit of this world
            return Block.JAVA_AIR_ID;
        }

        DataPalette chunk = column.sections()[(y - minY) >> 4];
        if (chunk != null) {
            return chunk.get(x & 0xF, y & 0xF, z & 0xF);
        }

        return Block.JAVA_AIR_ID;
    }

    public void removeChunk(int chunkX, int chunkZ) {
        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        chunks.remove(chunkPosition);
    }


    public void loadDimension() {
        JavaDimension dimension = player.getSession().getDimensionType();
        this.minY = dimension.minY();
        this.heightY = dimension.maxY();
    }

    /**
     * Manually clears all entries in the chunk cache.
     * The server is responsible for clearing chunk entries if out of render distance (for example) or switching dimensions,
     * but it is the client that must clear sections in the event of proxy switches.
     */
    public void clear() {
        chunks.clear();
    }

    public int getChunkMinY() {
        return minY >> 4;
    }

    public int getChunkHeightY() {
        return heightY >> 4;
    }
}