package ac.boar.anticheat.data;

import ac.boar.anticheat.player.BoarPlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.item.Items;

@RequiredArgsConstructor
@Getter
@Setter
public class ItemUseTracker {
    private final BoarPlayer player;

    private ItemData usedItem = ItemData.AIR;
    private int javaItemId = -1;
    private DirtyUsing dirtyUsing = DirtyUsing.NONE;
    private int useDuration;
    public enum DirtyUsing {
        METADATA, INVENTORY_TRANSACTION, NONE
    }

    public void preTick() {
        if (!player.getFlagTracker().has(EntityFlag.USING_ITEM)) {
            return;
        }

        if (this.javaItemId == Items.TRIDENT.javaId()) {
            player.sinceTridentUse++;
        }
    }

    public void postTick() {
//        if (-- this.useDuration == 0) {
//            System.out.println("Done duration!");
//        }

        if (!player.getFlagTracker().has(EntityFlag.USING_ITEM)) {
            if (this.usedItem != ItemData.AIR || this.javaItemId != -1) {
                this.release();
            }

            return;
        }

        if (this.usedItem == ItemData.AIR || this.javaItemId == -1) {
            this.release();
            return;
        }

        if (!player.compensatedInventory.inventoryContainer.getHeldItemData().equals(this.usedItem, false, false, false)) {
            this.release();
        }
    }

    public void release() {
//        System.out.println("Release!");
        this.usedItem = ItemData.AIR;
        this.javaItemId = -1;
        player.sinceTridentUse = 0;
        player.getFlagTracker().set(EntityFlag.USING_ITEM, false);
    }

    public void use(final ItemData usedItem, int itemId, boolean skip) {
        if (!canBeUse(usedItem, itemId) && !skip) {
//            System.out.println("Skip!");
            return;
        }

        this.usedItem = usedItem;
        this.javaItemId = itemId;
        this.dirtyUsing = DirtyUsing.INVENTORY_TRANSACTION;

//        this.useDuration = getMaxDuration(this.usedItem, this.javaItemId);

        player.sinceTridentUse = 0;
    }

//    private int getMaxDuration(final ItemData usedItem, int itemId) {
//        try {
//            final NbtMap map = usedItem.getDefinition().getComponentData();
//            if (map != null) {
//                NbtMap components = map.getCompound("components");
//                if (components != null) {
//                    if (components.containsKey("minecraft:use_duration")) {
//                        return components.getInt("minecraft:use_duration");
//                    } else {
//                        NbtMap itemProperties = components.getCompound("item_properties");
//                        if (itemProperties.containsKey("use_duration")) {
//                            return components.getInt("use_duration");
//                        }
//                    }
//                }
//            }
//        } catch (Exception ignored) {}
//
//        if (itemId == Items.BOW.javaId() || itemId == Items.CROSSBOW.javaId() || itemId == Items.TRIDENT.javaId()) {
//            return 72000;
//        } else if (itemId == Items.SPYGLASS.javaId()) {
//            return 1200;
//        }
//
//        return 1;
//    }

    private boolean canBeUse(final ItemData usedItem, int itemId) {
        // This way we can support custom item use duration too, also wrap this since I don't trust myself enough.
        try {
            final NbtMap map = usedItem.getDefinition().getComponentData();
            if (map != null) {
                NbtMap components = map.getCompound("components");
                if (components == null) {
                    return true;
                }

                if (components.containsKey("minecraft:use_duration")) {
//                    System.out.println(components.get("minecraft:use_duration"));
                    return true;
                } else {
                    NbtMap itemProperties = components.getCompound("item_properties");
                    if (itemProperties.containsKey("use_duration")) {
//                        System.out.println(components.get("use_duration"));
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}

        return itemId == Items.BOW.javaId() || itemId == Items.CROSSBOW.javaId() ||
                itemId == Items.TRIDENT.javaId() || itemId == Items.ENDER_EYE.javaId() ||
                itemId == Items.SPYGLASS.javaId();
    }
}
