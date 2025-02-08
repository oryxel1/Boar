package ac.boar.anticheat.data;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.attribute.AttributeModifierData;
import org.cloudburstmc.protocol.bedrock.data.attribute.AttributeOperation;

import java.util.HashMap;
import java.util.Map;

@Getter
public class PlayerAttributeData {
    @Setter
    private float baseValue;
    @Setter
    private float value;
    private boolean dirty = true;

    private final Map<String, AttributeModifierData> modifiers = new HashMap<>();

    public PlayerAttributeData(float baseValue) {
        this.baseValue = baseValue;
    }

    public void onUpdate() {
        this.dirty = true;
    }

    public void clearModifiers() {
        this.modifiers.clear();
    }

    public void removeModifier(final String id) {
        final AttributeModifierData lv = this.modifiers.remove(id);
        if (lv != null) {
            this.onUpdate();
        }
    }

    public void addTemporaryModifier(AttributeModifierData modifier) {
        this.addModifier(modifier);
    }

    private void addModifier(AttributeModifierData modifier) {
        final AttributeModifierData lv = this.modifiers.putIfAbsent(modifier.getId(), modifier);
        if (lv == null) {
            this.onUpdate();
        }
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
        for (final Map.Entry<String, AttributeModifierData> entry : this.modifiers.entrySet()) {
            final AttributeModifierData modifier = entry.getValue();
            if (modifier.getOperation() == AttributeOperation.ADDITION) {
                base += modifier.getAmount();
            }
        }
        float value = base;
        for (final Map.Entry<String, AttributeModifierData> entry : this.modifiers.entrySet()) {
            final AttributeModifierData modifier = entry.getValue();
            if (modifier.getOperation() == AttributeOperation.MULTIPLY_BASE) {
                value += (base * modifier.getAmount());
            }
        }
        for (final Map.Entry<String, AttributeModifierData> entry : this.modifiers.entrySet()) {
            final AttributeModifierData modifier = entry.getValue();
            if (modifier.getOperation() == AttributeOperation.MULTIPLY_TOTAL) {
                value *= (1.0F + modifier.getAmount());
            }
        }

        return value;
    }
}