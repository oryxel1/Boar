package ac.boar.anticheat.data;

import ac.boar.anticheat.data.cache.UseDurationCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Vec3;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.MovementEffectType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.MovementEffectPacket;
import org.geysermc.geyser.item.Items;

@RequiredArgsConstructor
public class UseItemCache {
    private final BoarPlayer player;

    private int useItemJavaId = -1;
    private ItemData useItem = ItemData.AIR;
    private int useItemRemaining;

    private int goatHornCooldown;

    public void tick() {
        if (this.goatHornCooldown > 0) {
            this.goatHornCooldown--;
        }

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
        if (this.useItemJavaId == Items.TRIDENT.javaId()) {
            player.setDirtyRiptide(72000 - this.useItemRemaining, this.useItem);
        }

        this.useItem = ItemData.AIR;
        this.useItemJavaId = -1;
        this.useItemRemaining = 0;

        player.getFlagTracker().set(EntityFlag.USING_ITEM, false);
    }

    public boolean isUsingItem() {
        return player.getFlagTracker().has(EntityFlag.USING_ITEM);
    }

    public void use(final ItemData useItem) {
        int itemId = player.compensatedInventory.translate(useItem).getId();
        int useDuration = UseDurationCache.getUseDuration(itemId);

        if (itemId == Items.FIREWORK_ROCKET.javaId() && player.getFlagTracker().has(EntityFlag.GLIDING)) {
            NbtMap map = useItem.getTag();

            int flightBoost = 1;
            if (map != null && map.containsKey("Fireworks")) {
                NbtMap fireworks = map.getCompound("Fireworks");
                if (fireworks != null && fireworks.containsKey("Flight")) {
                    flightBoost = fireworks.getByte("Flight");
                }
            }

            MovementEffectPacket effectPacket = new MovementEffectPacket();
            effectPacket.setEffectType(MovementEffectType.GLIDE_BOOST);
            effectPacket.setTick(player.tick);
            effectPacket.setEntityRuntimeId(player.runtimeEntityId);
            effectPacket.setDuration(0);
            player.cloudburstDownstream.sendPacketImmediately(effectPacket);
            player.sendLatencyStack(true);
            player.latencyUtil.addTaskToQueue(player.sentStackId.get(), () -> {
                player.tryingToElytraBoost = false;
            });

            player.tryingToElytraBoost = true;
            player.sinceElytraBoost = 5;

            // TODO: We should only do this when player actually spawned a firework rocket.
            player.velocity = new Vec3(
                    -TrigMath.sin(player.yaw * MathUtil.DEGREE_TO_RAD) * TrigMath.cos(player.pitch * MathUtil.DEGREE_TO_RAD) * flightBoost,
                    -TrigMath.sin(player.pitch * MathUtil.DEGREE_TO_RAD) * flightBoost,
                    TrigMath.cos(player.yaw * MathUtil.DEGREE_TO_RAD) * TrigMath.cos(player.pitch * MathUtil.DEGREE_TO_RAD) * flightBoost);
        }

        if (useDuration == -1) {
            return;
        }

        if (itemId == Items.GOAT_HORN.javaId()) {
            if (goatHornCooldown > 0) {
                return;
            }

            this.goatHornCooldown = useDuration;
        }

        this.useItemJavaId = itemId;

        this.useItem = useItem;
        this.useItemRemaining = useDuration;

        player.getFlagTracker().set(EntityFlag.USING_ITEM, true);
    }
}
