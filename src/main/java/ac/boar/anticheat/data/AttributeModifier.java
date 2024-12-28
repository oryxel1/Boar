package ac.boar.anticheat.data;

import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;

public record AttributeModifier(String id, double amount, ModifierOperation operation) {
}