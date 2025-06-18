package ac.boar.anticheat.packets.world;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.compensated.world.base.CompensatedWorld;
import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
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
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.chunk.bitarray.BitArray;
import org.geysermc.geyser.level.chunk.bitarray.BitArrayVersion;
import org.geysermc.geyser.level.chunk.bitarray.SingletonBitArray;

import java.util.Objects;

public class ChunkWorldPackets implements PacketListener {
    @Override
    public void onPacketSend(CloudburstPacketEvent event, boolean immediate) {
        final BoarPlayer player = event.getPlayer();
        final CompensatedWorld world = player.compensatedWorld;

        if (event.getPacket() instanceof RespawnPacket packet && packet.getState() == RespawnPacket.State.SERVER_READY) {
            if (packet.getRuntimeEntityId() != 0) { // Vanilla behaviour according Geyser.
                return;
            }

            player.sendLatencyStack(immediate);
            player.latencyUtil.addTaskToQueue(player.sentStackId.get(), () -> {
                player.tick = Long.MIN_VALUE;

                player.prevUnvalidatedPosition = player.unvalidatedPosition = new Vec3(packet.getPosition()).subtract(0, player.getYOffset(), 0);
                player.setPos(player.unvalidatedPosition.clone());

                player.hasLeastRunPredictionOnce = false;
            });
        }

        if (event.getPacket() instanceof ChangeDimensionPacket packet) {
            int dimensionId = packet.getDimension();
            final BedrockDimension dimension = dimensionId == BedrockDimension.OVERWORLD_ID ? BedrockDimension.OVERWORLD
                    : dimensionId == BedrockDimension.BEDROCK_NETHER_ID ? BedrockDimension.THE_NETHER : BedrockDimension.THE_END;

            player.sendLatencyStack(immediate);
            player.latencyUtil.addTaskToQueue(player.sentStackId.get(), () -> {
                world.getChunks().clear();
                world.setDimension(dimension);

                player.currentLoadingScreen = packet.getLoadingScreenId();
                player.inLoadingScreen = true;

                player.getFlagTracker().clear();
                player.wasFlying = player.flying = false;
                player.getTeleportUtil().getQueuedTeleports().clear();

                player.tick = Long.MIN_VALUE;
            });
        }

        // Based off GeyserMC and ViaBedrock code, should be correct!
        if (event.getPacket() instanceof LevelChunkPacket packet) {
            int sectionCount = packet.getSubChunksLength();

            // Bedrock ignores this.
            if (sectionCount == -2) {
                return;
            }

            // Sub chunk?
            if (sectionCount == 0) {
                return;
            }

            int dimensionId = packet.getDimension();
            if (dimensionId < 0 || dimensionId > 2) {
                // Is this even possible.
                return;
            }

            int yOffset = world.getMinY() >> 4, chunkSize = world.getHeightY() >> 4;

            final BedrockDimension dimension = dimensionId == BedrockDimension.OVERWORLD_ID ? BedrockDimension.OVERWORLD
                    : dimensionId == BedrockDimension.BEDROCK_NETHER_ID ? BedrockDimension.THE_NETHER : BedrockDimension.THE_END;

            int dimensionOffset = dimension.minY() >> 4;

            final BoarChunkSection[] sections = new BoarChunkSection[dimension.height() >> 4];

            final ByteBuf buffer = packet.getData().copy();
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

                    readStorage(buffer, player.BEDROCK_AIR);
                }

                // Border blocks.
                buffer.skipBytes(1);

                // Just ignore the rest, I don't need those.
            } catch (Exception ignored) {
                // Ignore and just use whatever we were able to read.
                // ignored.printStackTrace();
            } finally {
                buffer.release();
            }

            // TODO: Should we send stack id all the time?
            player.sendLatencyStack(immediate);
            player.latencyUtil.addTaskToQueue(player.sentStackId.get(), () -> {
                if (dimension != world.getDimension()) {
                    return;
                }

                world.addToCache(packet.getChunkX(), packet.getChunkZ(), sections);
            });
        }

        if (event.getPacket() instanceof UpdateBlockPacket packet) {
            // Ugly hack.
            if (packet.getDataLayer() == 0 && Boar.getConfig().ignoreGhostBlock() && !player.inLoadingScreen && player.sinceLoadingScreen >= 2) {
                BlockState state = BlockState.of(player.bedrockBlockToJava.getOrDefault(packet.getDefinition().getRuntimeId(), Blocks.AIR.javaId()));
                if (state.is(Blocks.AIR) || state.is(Blocks.CAVE_AIR) || state.is(Blocks.VOID_AIR)) {
                    int distance = Math.abs(packet.getBlockPosition().getY() - GenericMath.floor(player.position.y - 1));
                    if (distance <= 1) {
                        player.tickSinceBlockResync = 5;
                        world.updateBlock(packet.getBlockPosition(), packet.getDataLayer(), packet.getDefinition().getRuntimeId());
                    }
                }
            }

            player.sendLatencyStack(immediate);
            player.latencyUtil.addTaskToQueue(player.sentStackId.get(), () -> world.updateBlock(packet.getBlockPosition(), packet.getDataLayer(), packet.getDefinition().getRuntimeId()));
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