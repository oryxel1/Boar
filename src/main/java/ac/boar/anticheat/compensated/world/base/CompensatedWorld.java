package ac.boar.anticheat.compensated.world.base;

import ac.boar.anticheat.compensated.cache.entity.EntityCache;
import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.data.block.impl.HoneyBlockState;
import ac.boar.anticheat.data.block.impl.SlimeBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import it.unimi.dsi.fastutil.longs.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.chunk.GeyserChunkSection;
import org.geysermc.geyser.util.MathUtils;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Setter
@Getter
public class CompensatedWorld {
    private final BoarPlayer player;
    private final Long2ObjectMap<GeyserChunkSection[]> chunks = new Long2ObjectOpenHashMap<>();

    private BedrockDimension dimension;

    private final Long2ObjectMap<EntityCache> entities = new Long2ObjectOpenHashMap<>();
    private final Map<Long, Long> uniqueIdToRuntimeId = new HashMap<>();

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

    public EntityCache addToCache(final BoarPlayer player, final long runtimeId, final long uniqueId) {
        final Entity entity = player.getSession().getEntityCache().getEntityByGeyserId(runtimeId);
        if (entity == null || entity.getDefinition() == null || runtimeId == player.runtimeEntityId) {
            return null;
        }

        final EntityDefinition<?> definition = entity.getDefinition();
        final EntityDimensions dimensions = EntityDimensions.fixed(definition.width(), definition.height());

        player.sendLatencyStack();
        final EntityCache cache = new EntityCache(player, definition.entityType(), definition, dimensions, player.sentStackId.get(), runtimeId);
        this.entities.put(runtimeId, cache);
        this.uniqueIdToRuntimeId.put(uniqueId, runtimeId);

        return cache;
    }

    public void addToCache(int x, int z, GeyserChunkSection[] chunks) {
        long chunkPosition = MathUtils.chunkPositionToLong(x, z);
        this.chunks.put(chunkPosition, chunks);
    }

    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return this.getChunk(chunkX >> 4, chunkZ >> 4) != null;
    }

    public void updateBlock(final Vector3i position, int layer, int block) {
        this.updateBlock(position.getX(), position.getY(), position.getZ(), layer, block);
    }

    public void updateBlock(int x, int y, int z, int layer, int block) {
        final GeyserChunkSection[] column = this.getChunk(x >> 4, z >> 4);
        if (column == null) {
            return;
        }

        if (y < getMinY() || ((y - getMinY()) >> 4) > column.length - 1) {
            // Y likely goes above or below the height limit of this world
            return;
        }

        GeyserChunkSection palette = column[(y - getMinY()) >> 4];
        if (palette == null) {
            if (block != 0) {
                // A previously empty chunk, which is no longer empty as a block has been added to it
                column[(y - getMinY()) >> 4] = palette = new GeyserChunkSection(this.player.BEDROCK_AIR, 0);
            } else {
                // Nothing to update
                return;
            }
        }

        palette.setFullBlock(x & 0xF, y & 0xF, z & 0xF, layer, block);
    }

    public BoarBlockState getBlockState(Vector3i vector3i, int layer) {
        return getBlockState(vector3i.getX(), vector3i.getY(), vector3i.getZ(), layer);
    }

    public BoarBlockState getBlockState(int x, int y, int z, int layer) {
        BlockState state = BlockState.of(getBlockAt(x, y, z, layer));
        if (state.is(Blocks.HONEY_BLOCK)) {
            return new HoneyBlockState(state);
        } else if (state.is(Blocks.SLIME_BLOCK)) {
            return new SlimeBlockState(state);
        }

        return new BoarBlockState(state);
    }

    public int getBlockAt(int x, int y, int z, int layer) {
        GeyserChunkSection[] column = this.getChunk(x >> 4, z >> 4);
        if (column == null) {
            return 0;
        }

        if (y < getMinY() || ((y - getMinY()) >> 4) > column.length - 1) {
            // Y likely goes above or below the height limit of this world
            return 0;
        }

        GeyserChunkSection chunk = column[(y - getMinY()) >> 4];
        if (chunk != null) {
            try {
                return player.bedrockBlockToJava.getOrDefault(chunk.getFullBlock(x & 0xF, y & 0xF, z & 0xF, layer), 0);
            } catch (Exception e) {
                return 0;
            }
        }

        return 0;
    }

    private GeyserChunkSection[] getChunk(int chunkX, int chunkZ) {
        long chunkPosition = MathUtils.chunkPositionToLong(chunkX, chunkZ);
        return this.chunks.getOrDefault(chunkPosition, null);
    }

    public int getMinY() {
        return this.dimension.minY();
    }

    public int getHeightY() {
        return this.dimension.maxY();
    }
}