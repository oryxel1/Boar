package ac.boar.anticheat.compensated;

import ac.boar.anticheat.compensated.cache.ChunkCache;
import ac.boar.anticheat.compensated.cache.entity.EntityCache;
import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.util.block.BlockUtil;
import ac.boar.anticheat.util.math.Mutable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.player.BoarPlayer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.level.JavaDimension;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;

import java.util.HashMap;
import java.util.Map;

// https://github.com/GeyserMC/Geyser/blob/master/core/src/main/java/org/geysermc/geyser/session/cache/ChunkCache.java
@RequiredArgsConstructor
@Getter
public final class CompensatedWorld {
    private final BoarPlayer player;
    private final Long2ObjectMap<ChunkCache> chunks = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<EntityCache> entities = new Long2ObjectOpenHashMap<>();
    private final Map<Long, Long> uniqueIdToRuntimeId = new HashMap<>();

    // Entity related

    public EntityCache addToCache(final BoarPlayer player, final long runtimeId, final long uniqueId) {
        final Entity entity = player.getSession().getEntityCache().getEntityByGeyserId(runtimeId);
        if (entity == null || entity.getDefinition() == null || runtimeId == player.runtimeEntityId) {
            return null;
        }

        final EntityDefinition<?> definition = entity.getDefinition();
        final EntityDimensions dimensions = EntityDimensions.fixed(definition.width(), definition.height());

        player.sendTransaction();
        final EntityCache cache = new EntityCache(player, definition.entityType(), definition, dimensions, player.lastSentId, runtimeId);
        this.entities.put(runtimeId, cache);
        this.uniqueIdToRuntimeId.put(uniqueId, runtimeId);

        return cache;
    }

    public void removeEntity(final long uniqueId) {
        final long key = this.uniqueIdToRuntimeId.getOrDefault(uniqueId, -1L);
        if (key == -1L) {
            return;
        }

        this.entities.remove(key);
    }

    public EntityCache getEntity(long id) {
        return this.entities.get(id);
    }

    // Chunk/World related

    @Getter
    private int minY;
    private int heightY;

    public void addToCache(int x, int z, DataPalette[] chunks, long id) {
        long chunkPosition = MathUtils.chunkPositionToLong(x, z);
        ChunkCache geyserChunk = new ChunkCache(chunks, id);
        this.chunks.put(chunkPosition, geyserChunk);
    }

    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        final ChunkCache chunk = this.getChunk(chunkX >> 4, chunkZ >> 4);
        return chunk != null && chunk.transactionId() <= player.lastReceivedId;
    }

    private ChunkCache getChunk(int chunkX, int chunkZ) {
        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        return chunks.getOrDefault(chunkPosition, null);
    }

    public void updateBlock(final Vector3i vector3i, int block) {
        this.updateBlock(vector3i.getX(), vector3i.getY(), vector3i.getZ(), block);
    }

    public void updateBlock(int x, int y, int z, int block) {
        final ChunkCache chunk = this.getChunk(x >> 4, z >> 4);
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
        for (int q = minX; q <= maxX; q++) {
            for (int r = minZ; r <= maxZ; r++) {
                if (!this.isChunkLoaded(q, r)) {
                    return false;
                }
            }
        }

        return true;
    }

    public FluidState getFluidState(final Vector3i vec3) {
        return this.getFluidState(vec3.getX(), vec3.getY(), vec3.getZ());
    }

    public FluidState getFluidState(final Mutable mutable) {
        return this.getFluidState(mutable.getX(), mutable.getY(), mutable.getZ());
    }

    public FluidState getFluidState(int x, int y, int z) {
        final int blockId = getBlockAt(x, y, z);
        float waterHeight = BlockUtil.getWorldFluidHeight(Fluid.WATER, blockId);
        float lavaHeight = BlockUtil.getWorldFluidHeight(Fluid.LAVA, blockId);

        if (waterHeight <= 0 && lavaHeight <= 0) {
            return new FluidState(Fluid.EMPTY, 0);
        }

        if (waterHeight > 0) {
            return new FluidState(Fluid.WATER, waterHeight);
        }

        return new FluidState(Fluid.LAVA, lavaHeight);
    }

    public BlockState getBlockState(final Mutable mutable) {
        return this.getBlockState(mutable.getX(), mutable.getY(), mutable.getZ());
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
        ChunkCache column = this.getChunk(x >> 4, z >> 4);
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

    public void loadDimension(boolean synced) {
        final JavaDimension dimension = player.getSession().getDimensionType();
        if (!synced) {
            this.minY = dimension.minY();
            this.heightY = dimension.maxY();
            return;
        }

        player.sendTransaction(true);
        player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
            this.minY = dimension.minY();
            this.heightY = dimension.maxY();
        });
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