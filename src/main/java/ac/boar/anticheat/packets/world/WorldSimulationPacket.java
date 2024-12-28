package ac.boar.anticheat.packets.world;

import ac.boar.anticheat.handler.BlockInteractHandler;
import ac.boar.anticheat.player.BoarPlayer;

import ac.boar.protocol.event.*;
import ac.boar.protocol.listener.*;

import io.netty.buffer.*;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateBlockPacket;
import org.geysermc.geyser.level.block.property.Properties;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;

public class WorldSimulationPacket implements CloudburstPacketListener, MCPLPacketListener {
    @Override
    public void onPacketSend(MCPLPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (event.getPacket() instanceof ClientboundLevelChunkWithLightPacket packet) {
            final int chunkSize = player.compensatedWorld.getChunkHeightY();
            final DataPalette[] palette = new DataPalette[chunkSize];

            final ByteBuf in = Unpooled.wrappedBuffer(packet.getChunkData());
            for (int sectionY = 0; sectionY < chunkSize; sectionY++) {
                ChunkSection javaSection = player.getCodecHelper().readChunkSection(in);
                palette[sectionY] = javaSection.getChunkData();
            }

            // Send a transaction if player is inside (or near) that chunk.
            int chunkX = packet.getX() << 4, chunkZ = packet.getZ() << 4;
            boolean check = Math.abs(player.x - chunkX) <= 16 || Math.abs(player.z - chunkZ) <= 16;
            if (check) {
                event.getPostTasks().add(player::sendTransaction);
            }

            player.compensatedWorld.addToCache(packet.getX(), packet.getZ(), palette, player.lastSentId + (check ? 1 : 0));
        }

        if (event.getPacket() instanceof ClientboundForgetLevelChunkPacket packet) {
            // Send a transaction if player is inside (or near) that chunk.
            int chunkX = packet.getX() << 4, chunkZ = packet.getZ() << 4;
            boolean check = Math.abs(player.x - chunkX) <= 16 || Math.abs(player.z - chunkZ) <= 16;
            if (check) {
                event.getPostTasks().add(player::sendTransaction);
            }

            player.latencyUtil.addTransactionToQueue(player.lastSentId + (check ? 1 : 0),
                    () -> player.compensatedWorld.removeChunk(packet.getX(), packet.getZ()));
        }

        if (event.getPacket() instanceof ClientboundRespawnPacket) {
            event.getPostTasks().add(player.compensatedWorld::loadDimension);
        }
    }

    @Override
    public void onPacketSend(CloudburstPacketEvent event, boolean immediate) {
        final BoarPlayer player = event.getPlayer();
        if (event.getPacket() instanceof UpdateBlockPacket packet) {
            final Vector3i blockPosition = packet.getBlockPosition();
            // Waterlogged block.
            if (packet.getDataLayer() == 1) {
                final boolean waterlogged = packet.getDefinition() == player.getSession().getBlockMappings().getBedrockWater();

                event.getPostTasks().add(() -> player.sendTransaction(immediate));
                player.latencyUtil.addTransactionToQueue(player.lastSentId + 1, () -> {
                    final BlockState state = player.compensatedWorld.getBlockState(blockPosition);
                    if (state.getValue(Properties.WATERLOGGED) != null) {
                        player.compensatedWorld.updateBlock(blockPosition, state.withValue(Properties.WATERLOGGED, waterlogged).javaId());
                    }
                });
                return;
            }

            int javaId = player.bedrockToJavaBlockId(packet.getDefinition());
            if (javaId == -1) {
                player.getSession().getGeyser().getWorldManager().getBlockAt(
                        player.getSession(), blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
            }

            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> player.compensatedWorld.updateBlock(blockPosition, javaId));
        }
    }

    @Override
    public void onPacketReceived(CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof InventoryTransactionPacket packet)) {
            return;
        }

        if (packet.getTransactionType() == InventoryTransactionType.ITEM_USE && packet.getActionType() == 0) {
            BlockInteractHandler.handleBlockClick(player, packet.getBlockPosition());
        }
    }
}
