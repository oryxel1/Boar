package ac.boar.anticheat.validator;

import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.cloudburstmc.protocol.bedrock.packet.ItemStackRequestPacket;

@RequiredArgsConstructor
public final class ItemTransactionValidator {
    private final BoarPlayer player;

    public void handle(final InventoryTransactionPacket packet) {
        System.out.println(packet);
    }

    public void handle(final ItemStackRequestPacket packet) {
        final CompensatedInventory inventory = player.compensatedInventory;
        if (inventory.openContainer == null) {
            return;
        }

        System.out.println(packet);
    }
}