package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.data.AttributeData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.event.MCPLPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import ac.boar.protocol.listener.MCPLPacketListener;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.Attribute;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeModifier;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundUpdateAttributesPacket;

public class AttributeSimulationPacket implements CloudburstPacketListener, MCPLPacketListener {
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
                        player.attributes.get(AttributeType.Builtin.MOVEMENT_SPEED.getId()).setBaseValue(layer.getWalkSpeed());
                    }

                    player.abilities.addAll(layer.getAbilityValues());
                }

                player.wasFlying = player.flying;
                player.flying = player.abilities.contains(Ability.FLYING) || player.abilities.contains(Ability.MAY_FLY) && player.flying;
            });
        }
    }

    @Override
    public void onPacketSend(final MCPLPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof ClientboundUpdateAttributesPacket attributesPacket) || attributesPacket.getEntityId() != player.javaEntityId) {
            return;
        }

        for (final Attribute attribute : attributesPacket.getAttributes()) {
            final AttributeData data = player.attributes.get(attribute.getType().getId());

            player.sendTransaction();
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> {
                data.getModifiers().clear();
                data.setBaseValue((float) attribute.getValue());
                player.hasSprintingAttribute = false;
                for (final AttributeModifier modifier : attribute.getModifiers()) {
                    final String id = modifier.toString().split(",")[0].replace("AttributeModifier(id=", "");
                    if (id.equals("minecraft:sprinting")) {
                        player.hasSprintingAttribute = true;
                    } else {
                        data.getModifiers().put(id, new ac.boar.anticheat.data.AttributeModifier(id, modifier.getAmount(), modifier.getOperation()));
                    }
                }
            });
        }
    }
}
