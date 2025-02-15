package ac.boar.anticheat.validator;

import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;

@RequiredArgsConstructor
public final class ItemTransactionValidator {
    private final BoarPlayer player;

    public void handle(final InventoryTransactionPacket transaction) {

    }
}