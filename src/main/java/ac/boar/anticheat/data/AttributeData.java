package ac.boar.anticheat.data;

import lombok.Getter;
import lombok.Setter;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;

import java.util.HashMap;
import java.util.Map;

@Getter
public class AttributeData {
    @Setter
    private float baseValue;
    private boolean dirty = true;
    private float value;

    private final Map<String, AttributeModifier> modifiers = new HashMap<>();

    public AttributeData(float baseValue) {
        this.baseValue = baseValue;
    }

    public void tick() {
        this.dirty = true;
    }

    public float getValue() {
        if (this.dirty) {
            this.value = this.computeValue();
            this.dirty = false;
        }

        return this.value;
    }

    private float computeValue() {
        float base = this.getBaseValue();
        for (final Map.Entry<String, AttributeModifier> entry : this.modifiers.entrySet()) {
            final AttributeModifier modifier = entry.getValue();
            if (modifier.operation() == ModifierOperation.ADD) {
                base += (float) modifier.amount();
            }
        }
        float value = base;
        for (final Map.Entry<String, AttributeModifier> entry : this.modifiers.entrySet()) {
            final AttributeModifier modifier = entry.getValue();
            if (modifier.operation() == ModifierOperation.ADD_MULTIPLIED_BASE) {
                value += (float) (base * modifier.amount());
            }
        }
        for (final Map.Entry<String, AttributeModifier> entry : this.modifiers.entrySet()) {
            final AttributeModifier modifier = entry.getValue();
            if (modifier.operation() == ModifierOperation.ADD_MULTIPLIED_TOTAL) {
                value *= (float) (1.0F + modifier.amount());
            }
        }

        return value;
    }
}