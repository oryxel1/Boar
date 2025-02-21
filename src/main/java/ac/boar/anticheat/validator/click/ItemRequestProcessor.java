package ac.boar.anticheat.validator.click;

import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.validator.ItemTransactionValidator;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.RecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapedRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.crafting.recipe.ShapelessRecipeData;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.DefaultDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.InvalidDescriptor;
import org.cloudburstmc.protocol.bedrock.data.inventory.descriptor.ItemDescriptorWithCount;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequest;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.ItemStackRequestSlotData;
import org.cloudburstmc.protocol.bedrock.data.inventory.itemstack.request.action.*;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class ItemRequestProcessor {
    private final BoarPlayer player;

    private final List<ItemData> queuedItems = new ArrayList<>();

    public boolean processAll(final ItemStackRequest request) {
        for (final ItemStackRequestAction action : request.getActions()) {
            System.out.println(action);
            if (!this.handle(action)) {
                return false;
            }
        }

        this.queuedItems.clear();

        return true;
    }

    public boolean handle(final ItemStackRequestAction action) {
        final CompensatedInventory inventory = player.compensatedInventory;

        final ItemStackRequestActionType type = action.getType();
        final ContainerCache cache = inventory.openContainer;

        switch (type) {
            case CRAFT_CREATIVE -> {
                if (player.gameType != GameType.CREATIVE) {
                    return false;
                }

                final CraftCreativeAction creativeAction = (CraftCreativeAction) action;
                final ItemData item = inventory.getCreativeData().get(creativeAction.getCreativeItemNetworkId());
                if (item == null) {
                    return false;
                }

                // Creative item yay! Also, we have to grab the item definition we stored instead of
                // the one player send to prevent they send some weird shit item to try anything funny.
                this.queuedItems.add(item);
            }

            case CRAFT_RECIPE -> {
                final CraftRecipeAction craftAction = (CraftRecipeAction) action;
                if (cache.getType() == ContainerType.WORKBENCH) {
                    final RecipeData rawRecipe = inventory.getCraftingData().get(craftAction.getRecipeNetworkId());
                    if (rawRecipe == null) {
                        System.out.println("No recipe found!");
                        break;
                    }

                    final List<ItemData> ingredients = List.of(
                            cache.get(32), cache.get(33), cache.get(34), cache.get(35), cache.get(36), cache.get(37), cache.get(38),
                            cache.get(39), cache.get(40));

                    List<ItemData> results = null;

                    // Simple silly crafting validation.
                    if (rawRecipe instanceof ShapelessRecipeData shapeless) {
                        for (final ItemDescriptorWithCount descriptor : shapeless.getIngredients()) {
                            if (descriptor.getDescriptor() instanceof DefaultDescriptor defaultDescriptor) {
                                boolean valid = false;
                                for (final ItemData item : ingredients) {
                                    if (ItemTransactionValidator.validate(item.getDefinition(), defaultDescriptor.getItemId())) {
                                        valid = true;
                                    }
                                }

                                if (!valid) {
                                    System.out.println("INVALID CRAFTING - SHAPELESS - INGREDIENTS!");
                                    return false;
                                }
                            }
                        }

                        results = shapeless.getResults();
                    } else if (rawRecipe instanceof ShapedRecipeData shaped) {
                        final List<ItemDefinition> predictedIngredients = new ArrayList<>();

                        for (final ItemDescriptorWithCount descriptor : shaped.getIngredients()) {
                            if (descriptor.getDescriptor() instanceof DefaultDescriptor defaultDescriptor) {
                                predictedIngredients.add(defaultDescriptor.getItemId());
                            } else if (descriptor.getDescriptor() instanceof InvalidDescriptor) {
                                predictedIngredients.add(ItemDefinition.AIR);
                            }
                        }

                        for (int i = 0; i < predictedIngredients.size(); i++) {
                            final ItemDefinition predicted = predictedIngredients.get(i);
                            final ItemDefinition claimed = ingredients.get(i).getDefinition();

                            if (!ItemTransactionValidator.validate(predicted, claimed)) {
                                System.out.println("INVALID CRAFTING - SHAPED - INGREDIENTS!");
                                return false;
                            }
                        }

                        results = shaped.getResults();
                    }

                    System.out.println("Valid crafting yay!");
                    if (results != null) {
                        this.queuedItems.addAll(results);
                    }
                }
            }

            case CRAFT_RESULTS_DEPRECATED -> {
                if (this.queuedItems.isEmpty()) {
                    return false;
                }

                final CraftResultsDeprecatedAction craftResult = (CraftResultsDeprecatedAction) action;

                for (final ItemData item : craftResult.getResultItems()) {
                    boolean valid = false;
                    for (final ItemData predicted : this.queuedItems) {
                        if (item.isNull()) {
                            continue;
                        }

                        if (ItemTransactionValidator.validate(item, predicted) && (item.getCount() == predicted.getCount() || player.gameType == GameType.CREATIVE)) {
                            valid = true;
                        }
                    }

                    if (!valid) {
                        return false;
                    }
                }

                System.out.println("Valid crafting yay! (2)");
            }

            case TAKE, PLACE -> {
                final TransferItemStackRequestAction transferAction = (TransferItemStackRequestAction) action;
                // TODO: bundle lol.

                final ItemStackRequestSlotData source = transferAction.getSource();
                final ItemStackRequestSlotData destination = transferAction.getDestination();

                final ContainerCache sourceContainer = this.findContainer(source.getContainer());
                final ContainerCache destinationContainer = this.findContainer(destination.getContainer());

                final int sourceSlot = source.getSlot();
                final int destinationSlot = destination.getSlot();

                // From creative menu, crafting or other actions.
                final boolean create = !this.queuedItems.isEmpty() && sourceSlot == 50 && source.getContainer() == ContainerSlotType.CREATED_OUTPUT;

                if (sourceSlot < 0 || destinationSlot < 0 || (sourceSlot >= sourceContainer.getContainerSize() && !create) || destinationSlot >= destinationContainer.getContainerSize()) {
                    return false;
                }

                final ItemData sourceData = create ? this.queuedItems.getFirst() : sourceContainer.get(sourceSlot);
                final ItemData destinationData = destinationContainer.get(destinationSlot);

                // Player try to move this item to an already occupied destination, and is sending TAKE/PLACE instead of SWAP.
                // This is not the same item too, so not possible...
                if (!destinationData.isNull() && !ItemTransactionValidator.validate(sourceData, destinationData)) {
                    // for debugging in case I fucked up.
                    System.out.println("INVALID DESTINATION!");
                    System.out.println(sourceData);
                    System.out.println(destinationSlot);
                    return false;
                }

                int count = transferAction.getCount();
                // Source data is air, or count is invalid.
                // Exempt this if player is grabbing from creative menu....
                if (!(create && player.gameType == GameType.CREATIVE) && (sourceData.isNull() || count <= 0 || count > sourceData.getCount())) {
                    System.out.println("INVALID COUNT!"); // for debugging in case I fucked up.
                    System.out.println("First condition: " + sourceData.isNull());
                    System.out.println("Count: " + count);
                    System.out.println("Source Data: " + sourceData);
                    return false;
                }

                count = Math.max(0, count);

                // Now simply move, lol.
                if (!create) {
                    this.remove(sourceContainer, sourceSlot, sourceData, count);
                }

                if (destinationData.isNull()) {
                    final ItemData.Builder builder = sourceData.toBuilder();
                    builder.count(count);

                    destinationContainer.set(destinationSlot, builder.build());
                } else {
                    this.add(destinationContainer, destinationSlot, destinationData, count);
                }
            }

            case SWAP -> {
                final SwapAction swapAction = (SwapAction) action;

                final ItemStackRequestSlotData source = swapAction.getSource();
                final ItemStackRequestSlotData destination = swapAction.getDestination();

                final ContainerCache sourceContainer = this.findContainer(source.getContainer());
                final ContainerCache destinationContainer = this.findContainer(destination.getContainer());

                final int sourceSlot = source.getSlot();
                final int destinationSlot = destination.getSlot();

                if (sourceSlot < 0 || destinationSlot < 0 || sourceSlot >= sourceContainer.getContainerSize() || destinationSlot >= destinationContainer.getContainerSize()) {
                    return false;
                }

                final ItemData sourceData = sourceContainer.get(sourceSlot);
                final ItemData destinationData = destinationContainer.get(destinationSlot);

                // Source/Destination slot is empty! Player is supposed to send TAKE/PLACE instead of SWAP!
                if (sourceData.isNull() || destinationData.isNull()) {
                    System.out.println("INVALID SWAP!"); // for debugging in case I fucked up.
                    return false;
                }

                // Now simply swap :D
                sourceContainer.set(sourceSlot, destinationData);
                destinationContainer.set(destinationSlot, sourceData);
            }

            case DROP -> {
                final DropAction dropAction = (DropAction) action;
                final int slot = dropAction.getSource().getSlot();
                if (slot < 0 || slot >= cache.getContainerSize()) {
                    return false;
                }

                final ItemStackRequestSlotData source = dropAction.getSource();

                // Player is clicking outside the window to drop.
                if (source.getContainer() == ContainerSlotType.CURSOR) {
                    final ItemData cursor = inventory.hudContainer.get(0);
                    if (!cursor.isValid() || slot != 0) { // Slot 0 is cursor slot.
                        return false;
                    }

                    this.remove(inventory.hudContainer, 0, cursor, dropAction.getCount());
                    System.out.println("drop cursor!");
                } else { // Dropping by pressing Q?
                    final ItemData data = cache.get(slot);
                    this.remove(cache, slot, data, dropAction.getCount());
                    System.out.println("drop count!");
                }
            }

            case DESTROY -> {
                final DestroyAction destroyAction = (DestroyAction) action;
                final ItemStackRequestSlotData source = destroyAction.getSource();
                final ContainerCache sourceContainer = this.findContainer(source.getContainer());

                final int slot = source.getSlot();
                if (slot < 0 || slot > sourceContainer.getContainerSize()) {
                    return false;
                }

                final ItemData itemData = sourceContainer.get(slot);

                if (destroyAction.getCount() > itemData.getCount()) {
                    return false;
                }

                this.remove(sourceContainer, slot, itemData, destroyAction.getCount());
            }

            case CONSUME -> {
                // TODO: predict this.
                final ConsumeAction consumeAction = (ConsumeAction) action;
                final ItemStackRequestSlotData source = consumeAction.getSource();
                final ContainerCache sourceContainer = this.findContainer(source.getContainer());

                final int slot = source.getSlot();
                if (slot < 0 || slot > sourceContainer.getContainerSize()) {
                    return false;
                }

                final ItemData itemData = sourceContainer.get(slot);

                if (consumeAction.getCount() > itemData.getCount()) {
                    return false;
                }

                this.remove(sourceContainer, slot, itemData, consumeAction.getCount());
            }
        }

        return true;
    }

    public void add(final ContainerCache cache, final int slot, final ItemData data, final int counts) {
        final ItemData.Builder builder = data.toBuilder();
        builder.count(data.getCount() + counts);
        cache.set(slot, builder.build());
    }

    private void remove(final ContainerCache cache, final int slot, final ItemData data, final int counts) {
        if (counts >= data.getCount()) {
            cache.set(slot, ItemData.AIR);
        } else {
            if (data.getCount() > 0) {
                final ItemData.Builder builder = data.toBuilder();
                builder.count(data.getCount() - counts);

                final ItemData newData = builder.build();
                if (newData.getCount() <= 0) {
                    cache.set(slot, ItemData.AIR);
                } else {
                    cache.set(slot, builder.build());
                }
            }
        }
    }

    private ContainerCache findContainer(final ContainerSlotType type) {
        final CompensatedInventory inventory = player.compensatedInventory;

        ContainerCache cache;
        switch (type) {
            case CURSOR -> cache = inventory.hudContainer;
            case ARMOR -> cache = inventory.armorContainer;
            case OFFHAND -> cache = inventory.offhandContainer;
            case INVENTORY, HOTBAR, HOTBAR_AND_INVENTORY -> cache = inventory.inventoryContainer;
            default -> cache = inventory.openContainer;
        }

        return cache;
    }
}
