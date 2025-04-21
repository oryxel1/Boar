package ac.boar.anticheat.data;

import ac.boar.anticheat.data.cache.UseDurationCache;
import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;

@RequiredArgsConstructor
public class UseItemCache {
    private final BoarPlayer player;

    private ItemData useItem = ItemData.AIR;
    private int useItemRemaining;

    public void tick() {
        if (useItemRemaining <= 0 || useItem == ItemData.AIR) {
            return;
        }

        if (player.compensatedInventory.inventoryContainer.getHeldItemData().equals(useItem, true, false, false)) {
            if (--this.useItemRemaining == 0) {
                this.release();
            }
        } else {
            this.release();
        }
    }

    public void release() {
        this.useItem = ItemData.AIR;
        this.useItemRemaining = 0;

        player.getFlagTracker().set(EntityFlag.USING_ITEM, false);
    }

    public boolean isUsingItem() {
        return player.getFlagTracker().has(EntityFlag.USING_ITEM);
    }

    public void use(final ItemData useItem) {
        int useDuration = UseDurationCache.getUseDuration(player.compensatedInventory.translate(useItem).getId());
        if (useDuration == -1) {
            return;
        }

        this.useItem = useItem;
        this.useItemRemaining = useDuration;

        player.getFlagTracker().set(EntityFlag.USING_ITEM, true);
    }
}
