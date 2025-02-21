package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.compensated.cache.container.impl.TradeContainerCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.CreativeItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.*;
import org.cloudburstmc.protocol.bedrock.packet.*;

import java.util.Objects;

public class InventorySimulationPacket implements CloudburstPacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        final CompensatedInventory inventory = player.compensatedInventory;

        if (event.getPacket() instanceof InventoryTransactionPacket packet) {
            event.setCancelled(!player.transactionValidator.handle(packet));
        }

        if (event.getPacket() instanceof ItemStackRequestPacket packet) {
            player.transactionValidator.handle(packet);
        }

        if (event.getPacket() instanceof InteractPacket packet) {
            if (player.runtimeEntityId != packet.getRuntimeEntityId()) {
                return;
            }

            if (packet.getAction() == InteractPacket.Action.OPEN_INVENTORY) {
                player.compensatedInventory.openContainer = player.compensatedInventory.inventoryContainer;
            }
        }

        if (event.getPacket() instanceof ContainerClosePacket packet) {
            if (inventory.openContainer == null) {
                return;
            }

            if (packet.getId() != inventory.openContainer.getId()) {
                return;
            }

            inventory.openContainer = null;
        }

        if (event.getPacket() instanceof MobEquipmentPacket packet) {
            final int newSlot = packet.getHotbarSlot();
            if (player.runtimeEntityId != packet.getRuntimeEntityId()) {
                return;
            }

            if (newSlot < 0 || newSlot > 8 || packet.getContainerId() != ContainerId.INVENTORY || inventory.heldItemSlot == newSlot) {
                return;
            }

            inventory.heldItemSlot = newSlot;
        }
    }

    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
        final BoarPlayer player = event.getPlayer();
        final CompensatedInventory inventory = player.compensatedInventory;

        if (event.getPacket() instanceof CreativeContentPacket packet) {
            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
                inventory.getCreativeData().clear();

                for (final CreativeItemData data : packet.getContents()) {
                    inventory.getCreativeData().put(data.getNetId(), data.getItem());
                }
            });
        }

        if (event.getPacket() instanceof CraftingDataPacket packet) {
            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
                inventory.getCraftingData().clear();

                for (final RecipeData data : packet.getCraftingData()) {
                    switch (data.getType()) {
                        case MULTI -> {
                            final MultiRecipeData recipe = (MultiRecipeData) data;
                            inventory.getCraftingData().put(recipe.getNetId(), recipe);
                        }

                        case SHAPED -> {
                            final ShapedRecipeData recipe = (ShapedRecipeData) data;
                            inventory.getCraftingData().put(recipe.getNetId(), recipe);
                        }

                        case SHAPELESS -> {
                            final ShapelessRecipeData recipe = (ShapelessRecipeData) data;
                            inventory.getCraftingData().put(recipe.getNetId(), recipe);
                        }

                        case SMITHING_TRANSFORM -> {
                            final SmithingTransformRecipeData recipe = (SmithingTransformRecipeData) data;
                            inventory.getCraftingData().put(recipe.getNetId(), recipe);
                        }

                        case SMITHING_TRIM -> {
                            final SmithingTrimRecipeData recipe = (SmithingTrimRecipeData) data;
                            inventory.getCraftingData().put(recipe.getNetId(), recipe);
                        }
                    }
                }
                inventory.setPotionMixData(packet.getPotionMixData());
            });
        }

        if (event.getPacket() instanceof InventorySlotPacket packet) {
            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
                final ContainerCache container = inventory.getContainer((byte) packet.getContainerId());
                if (container == null) {
                    return;
                }

                if (packet.getSlot() < 0 || packet.getSlot() >= container.getContainerSize()) {
                    return;
                }

                container.set(packet.getSlot(), packet.getItem());
            });
        }

        if (event.getPacket() instanceof ContainerOpenPacket packet) {
            System.out.println(packet);
            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
                final ContainerCache container = inventory.getContainer(packet.getId());
                inventory.openContainer = Objects.requireNonNullElseGet(container, () -> new ContainerCache(packet.getId(), packet.getType(), packet.getBlockPosition(), packet.getUniqueEntityId()));
            });
        }
//
        if (event.getPacket() instanceof UpdateEquipPacket packet) {
//            System.out.println(packet);
//            player.sendTransaction(immediate);
//            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> { try {
//                inventory.openContainer = new ContainerCache((byte) packet.getWindowId(),
//                        ContainerType.from(packet.getWindowType()), Vector3i.ZERO, packet.getUniqueEntityId());
//            } catch (Exception ignored) {}});
        }
//
        if (event.getPacket() instanceof UpdateTradePacket packet) {
            if (packet.getPlayerUniqueEntityId() != player.runtimeEntityId || packet.getContainerType() != ContainerType.TRADE) {
                return;
            }

            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> { try {
                inventory.openContainer = new TradeContainerCache(packet.getOffers(),
                        (byte) packet.getContainerId(), packet.getContainerType(), Vector3i.ZERO, packet.getTraderUniqueEntityId());
            } catch (Exception ignored) {}});
        }

        if (event.getPacket() instanceof InventoryContentPacket packet) {
            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
                final ContainerCache container = inventory.getContainer((byte) packet.getContainerId());
                if (container == null) {
                    return;
                }

                for (int i = 0; i < packet.getContents().size(); i++) {
                    container.set(i, packet.getContents().get(i));
                }
            });
        }

        if (event.getPacket() instanceof PlayerHotbarPacket packet) {
            if (packet.getContainerId() != inventory.inventoryContainer.getId() || !packet.isSelectHotbarSlot()) {
                return;
            }

            final int slot = packet.getSelectedHotbarSlot();
            if (slot >= 0 && slot < 9) {
                player.sendTransaction();
                player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> inventory.heldItemSlot = slot);
            }
        }
    }
}
