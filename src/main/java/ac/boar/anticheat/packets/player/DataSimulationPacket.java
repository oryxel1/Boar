package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.data.PlayerAttributeData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import ac.boar.protocol.listener.MCPLPacketListener;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.attribute.AttributeModifierData;
import org.cloudburstmc.protocol.bedrock.packet.SetEntityDataPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAttributesPacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;

public class DataSimulationPacket implements CloudburstPacketListener, MCPLPacketListener {
    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
        final BoarPlayer player = event.getPlayer();
        if (event.getPacket() instanceof UpdateAbilitiesPacket packet) {
            if (packet.getUniqueEntityId() != player.runtimeEntityId) {
                return;
            }

            event.getPostTasks().add(() -> player.sendTransaction(immediate));
            player.latencyUtil.addTransactionToQueue(player.lastSentId + 1, () -> {
                player.abilities.clear();
                for (AbilityLayer layer : packet.getAbilityLayers()) {
                    if (layer.getLayerType() == AbilityLayer.Type.BASE) {
                        player.attributes.get(GeyserAttributeType.MOVEMENT_SPEED.getBedrockIdentifier()).setBaseValue(layer.getWalkSpeed());
                    }

                    player.abilities.addAll(layer.getAbilityValues());
                }

                player.wasFlying = player.flying;
                player.flying = player.abilities.contains(Ability.FLYING) || player.abilities.contains(Ability.MAY_FLY) && player.flying;
            });
        }

        if (event.getPacket() instanceof SetEntityDataPacket packet) {
            if (packet.getRuntimeEntityId() != player.runtimeEntityId) {
                return;
            }

            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
                System.out.println(packet);
            });
        }

        if (event.getPacket() instanceof UpdateAttributesPacket packet) {
            if (packet.getRuntimeEntityId() != player.runtimeEntityId) {
                return;
            }

            player.sendTransaction(immediate);
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
                for (final AttributeData data : packet.getAttributes()) {
                    final PlayerAttributeData attribute = player.attributes.get(data.getName());
                    if (attribute == null) {
                        return;
                    }

                    attribute.clearModifiers();
                    attribute.setBaseValue(data.getDefaultValue());
                    attribute.setValue(data.getValue());

                    for (AttributeModifierData lv5 : data.getModifiers()) {
                        attribute.addTemporaryModifier(lv5);
                    }
                }
            });
        }
    }
}
