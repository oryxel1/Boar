package ac.boar.anticheat.packets.server;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.compensated.world.base.CompensatedWorld;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.DimensionUtil;
import ac.boar.anticheat.util.geyser.BlockStorage;
import ac.boar.anticheat.util.geyser.BoarChunkSection;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.protocol.bedrock.data.ServerboundLoadingScreenPacketType;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.common.util.VarInts;
import org.geysermc.geyser.level.BedrockDimension;
import org.geysermc.geyser.level.chunk.bitarray.BitArray;
import org.geysermc.geyser.level.chunk.bitarray.BitArrayVersion;
import org.geysermc.geyser.level.chunk.bitarray.SingletonBitArray;

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

                // Just ignore the rest, I don't need those.
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

                world.addToCache(packet.getChunkX(), packet.getChunkZ(), sections);
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
    }

    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof ServerboundLoadingScreenPacket packet && packet.getType() == ServerboundLoadingScreenPacketType.END_LOADING_SCREEN) {
            if (Objects.equals(player.currentLoadingScreen, packet.getLoadingScreenId())) {
                player.currentLoadingScreen = null;
                player.inLoadingScreen = false;
                player.sinceLoadingScreen = 0;
            }
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
}