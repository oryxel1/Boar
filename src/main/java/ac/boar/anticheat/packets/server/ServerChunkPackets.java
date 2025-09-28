package ac.boar.anticheat.packets.server;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.compensated.world.base.CompensatedWorld;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.DimensionUtil;
import ac.boar.anticheat.util.geyser.BlockStorage;
import ac.boar.anticheat.util.geyser.BoarChunk;
import ac.boar.anticheat.util.geyser.BoarChunkSection;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NBTInputStream;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;
import org.cloudburstmc.protocol.bedrock.data.ServerboundLoadingScreenPacketType;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.chunk.bitarray.BitArray;
import org.geysermc.geyser.level.chunk.bitarray.BitArrayVersion;
import org.geysermc.geyser.level.chunk.bitarray.SingletonBitArray;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerChunkPackets implements PacketListener {
    @Override
    public void onPacketSend(CloudburstPacketEvent event, boolean immediate) {
        final BoarPlayer player = event.getPlayer();
        final CompensatedWorld world = player.compensatedWorld;

        // Based off GeyserMC and ViaBedrock code, should be correct!
        if (event.getPacket() instanceof LevelChunkPacket packet) {
            int sectionCount = packet.getSubChunksLength();

            // Bedrock ignores this. TODO: Should we remove this chunk then? maybe I will test this later.
            if (sectionCount == -2) {
                return;
            }

            // Unless the player is seriously lagging then this shouldn't false, we should only perfectly compensate if the chunk is close enough and can actually affect player.
            int chunkX = packet.getChunkX() << 4, chunkZ = packet.getChunkZ() << 4;
            boolean send = Math.abs(player.position.x - chunkX) <= 16 || Math.abs(player.position.z - chunkZ) <= 16;

            if (send) {
                player.sendLatencyStack(immediate);
            }

            if (sectionCount == 0) {
                player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> world.removeFromCache(packet.getChunkX(), packet.getChunkZ()));
                return;
            }

            int dimensionId = packet.getDimension();
            if (dimensionId < 0 || dimensionId > 2) {
                // Is this even possible.
                return;
            }

            final BedrockDimension dimension = DimensionUtil.dimensionFromId(dimensionId);

            final BoarChunkSection[] sections = new BoarChunkSection[dimension.height() >> 4];

            int yOffset = world.getMinY() >> 4, chunkSize = world.getHeightY() >> 4;
            int dimensionOffset = dimension.minY() >> 4;

            final List<BlockEntityInfo> blockEntities = new ArrayList<>();

            final ByteBuf buffer = packet.getData().retainedDuplicate();
            try {
                // Read chunk sections.
                for (int i = 0; i < sectionCount; i++) {
                    buffer.readByte(); // Version.

                    short storageLength = buffer.readUnsignedByte();
                    short subChunkIndex = buffer.readUnsignedByte();

                    // Default size should be at-least 2, so I can do waterlogged block.
                    // Also, server (atleast GeyserMC) seems to be sending the second layer regardless if that layer have a second block storage.
                    BlockStorage[] storages = new BlockStorage[Math.max(storageLength, 2)];
                    for (int n = 0; n < storageLength; n++) {
                        storages[n] = readStorage(buffer, player.BEDROCK_AIR);
                    }

                    for (int n = 0; n < storages.length; n++) {
                        if (storages[n] == null) {
                            if (n == 1) {
                                final IntArrayList list = new IntArrayList(1);
                                list.add(player.BEDROCK_AIR);
                                storages[n] = new BlockStorage(BitArrayVersion.V0.createArray(4096, null), list);
                            } else {
                                storages[n] = new BlockStorage(player.BEDROCK_AIR, BitArrayVersion.V2);
                            }
                        }
                    }

                    sections[i] = new BoarChunkSection(storages, subChunkIndex);
                }

                // As of 1.18.30, the amount of biomes read is dependent on how high Bedrock thinks the dimension is
                int biomeCount = dimension.height() >> 4;
                // I don't really care about biomes, just ignore whatever we were able to read.
                for (int i = 0; i < biomeCount; i++) {
                    int biomeYOffset = dimensionOffset + i;
                    if (biomeYOffset < yOffset) {
                        buffer.skipBytes(1);
                        continue;
                    }
                    if (biomeYOffset >= (chunkSize + yOffset)) {
                        buffer.skipBytes(1);
                        continue;
                    }

                    readStorageAndSkip(buffer);
                }

                // Border blocks.
                buffer.skipBytes(1);

                final NBTInputStream nbtStream = NbtUtils.createNetworkReader(new ByteBufInputStream(buffer));
                while (buffer.isReadable()) {
                    Object tag = nbtStream.readTag();
                    if (tag instanceof NbtMap nbtTag) {
                        int x = nbtTag.getInt("x", 0);
                        int y = nbtTag.getInt("y", 0);
                        int z = nbtTag.getInt("z", 0);

                        blockEntities.add(new BlockEntityInfo(x, y, z, null, nbtTag));
                    }
                }
            } catch (Exception ignored) {
                // Ignore and just use whatever we were able to read, bedrock client do the same thing I think?
                // ignored.printStackTrace();
            } finally {
                buffer.release();
            }

            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> {
                if (dimension != world.getDimension()) {
                    return;
                }

                world.addToCache(packet.getChunkX(), packet.getChunkZ(), sections, blockEntities);
            });
        }

        if (event.getPacket() instanceof UpdateBlockPacket packet) {
            // Ugly hack.
            if (packet.getDataLayer() == 0 && Boar.getConfig().ignoreGhostBlock() && !player.inLoadingScreen && player.sinceLoadingScreen >= 2) {
                boolean newBlockIsAir = player.AIR_IDS.contains(packet.getDefinition().getRuntimeId());
                boolean oldBlockIsAir = player.AIR_IDS.contains(player.compensatedWorld.getRawBlockAt(packet.getBlockPosition().getX(), packet.getBlockPosition().getY(), packet.getBlockPosition().getZ(), 0));

                if (newBlockIsAir && !oldBlockIsAir) {
                    int distance = Math.abs(packet.getBlockPosition().getY() - GenericMath.floor(player.position.y - 1));
                    if (distance <= 1) {
                        player.tickSinceBlockResync = 5;
                        world.updateBlock(packet.getBlockPosition(), packet.getDataLayer(), packet.getDefinition().getRuntimeId());
                    }
                }
            }

            // Unless the player is seriously lagging then this shouldn't false, we should only perfectly compensate if the block is close enough and can actually affect player.
            boolean send = player.position.distanceTo(new Vec3(packet.getBlockPosition())) <= 16;
            if (send) {
                player.sendLatencyStack(immediate);
            }

            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> world.updateBlock(packet.getBlockPosition(), packet.getDataLayer(), packet.getDefinition().getRuntimeId()));
        }

        if (event.getPacket() instanceof BlockEntityDataPacket packet) {
            player.sendLatencyStack();
            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> {
                final BoarChunk chunk = player.compensatedWorld.getChunk(packet.getBlockPosition().getX() >> 4, packet.getBlockPosition().getZ() >> 4);
                if (chunk == null) {
                    return;
                }

                final Vector3i pos = packet.getBlockPosition();
                chunk.blockEntities().removeIf(block -> block.getX() == pos.getX() && block.getY() == pos.getY() && block.getZ() == pos.getZ());
                chunk.blockEntities().add(new BlockEntityInfo(pos.getX(), pos.getY(), pos.getZ(), null, packet.getData()));
            });
        }
    }

    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof ServerboundLoadingScreenPacket packet && packet.getType() == ServerboundLoadingScreenPacketType.END_LOADING_SCREEN) {
            if (Objects.equals(player.currentLoadingScreen, packet.getLoadingScreenId()) && player.inLoadingScreen) {
                player.currentLoadingScreen = null;
                player.inLoadingScreen = false;
                player.sinceLoadingScreen = 0;
            }
        }
    }

    private void readStorageAndSkip(final ByteBuf buffer) {
        final short header = buffer.readUnsignedByte();
        final int bitArrayVersion = header >> 1;
        if (bitArrayVersion == 127) {
            return;
        }

        BitArrayVersion version = BitArrayVersion.get(bitArrayVersion, true);

        int words = MathUtils.ceil(4096F / Cache.values()[version.ordinal()].entriesPerWord);
        if (version != BitArrayVersion.V0) {
            for (int i = 0; i < words; i++) {
                buffer.readIntLE();
            }
        }

        final int size = version == BitArrayVersion.V0 ? 1 : VarInts.readInt(buffer);
        for (int i = 0; i < size; i++) {
            VarInts.readInt(buffer);
        }
    }

    // According to ViaBedrock.
    private BlockStorage readStorage(final ByteBuf buffer, int airId) {
        final short header = buffer.readUnsignedByte();
        final int bitArrayVersion = header >> 1;

        if (bitArrayVersion == 127) { // 127 = Same values as previous palette
            return null;
        }

        final BitArray bitArray;
        if (bitArrayVersion == 0) {
            bitArray = BitArrayVersion.get(bitArrayVersion, true).createArray(4096, null);
        } else {
            bitArray = BitArrayVersion.get(bitArrayVersion, true).createArray(4096);
        }

        if (!(bitArray instanceof SingletonBitArray)) {
            for (int i = 0; i < bitArray.getWords().length; i++) {
                bitArray.getWords()[i] = buffer.readIntLE();
            }
        }

        final int size = bitArray instanceof SingletonBitArray ? 1 : VarInts.readInt(buffer);

        final IntList palette = new IntArrayList(size);
        for (int i = 0; i < size; i++) {
            palette.add(VarInts.readInt(buffer));
        }

        if (palette.isEmpty()) {
            palette.add(airId);
        }

        return new BlockStorage(bitArray, palette);
    }

    private enum Cache {
        V16(16, 2),
        V8(8, 4),
        V6(6, 5), // 2 bit padding
        V5(5, 6), // 2 bit padding
        V4(4, 8),
        V3(3, 10), // 2 bit padding
        V2(2, 16),
        V1(1, 32),
        V0(0, 0);

        final byte bits;
        final byte entriesPerWord;
        final int maxEntryValue;

        Cache(int bits, int entriesPerWord) {
            this.bits = (byte) bits;
            this.entriesPerWord = (byte) entriesPerWord;
            this.maxEntryValue = (1 << this.bits) - 1;
        }
    }
}