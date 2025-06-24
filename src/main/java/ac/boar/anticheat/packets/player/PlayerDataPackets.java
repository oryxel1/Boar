package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.data.vanilla.AttributeInstance;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer;
import org.cloudburstmc.protocol.bedrock.data.AttributeData;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.attribute.AttributeModifierData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;

import java.util.EnumSet;
import java.util.Set;

public class PlayerDataPackets implements PacketListener {
    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof SetPlayerGameTypePacket packet) {
            player.sendLatencyStack();
            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> player.gameType = GameType.from(packet.getGamemode()));
        }

        if (event.getPacket() instanceof UpdateAbilitiesPacket packet) {
            if (packet.getUniqueEntityId() != player.runtimeEntityId) {
                return;
            }

            event.getPostTasks().add(() -> player.sendLatencyStack(immediate));
            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get() + 1, () -> {
                player.abilities.clear();
                for (AbilityLayer layer : packet.getAbilityLayers()) {
                    // TODO: Figure this out? also "fly" speed doesn't seems to even be fly speed. too lazy to look at vanilla code brah
                    // also how can we be sure it's the same as java? well ignore this for now, attribute will handle this.
//                    if (layer.getLayerType() == AbilityLayer.Type.BASE) {
//                        player.attributes.get(GeyserAttributeType.MOVEMENT_SPEED.getBedrockIdentifier()).setBaseValue(layer.getWalkSpeed());
//                    }

                    player.abilities.addAll(layer.getAbilityValues());
                }

                player.getFlagTracker().setFlying(player.abilities.contains(Ability.FLYING) || player.abilities.contains(Ability.MAY_FLY) && player.getFlagTracker().isFlying());
            });
        }

        if (event.getPacket() instanceof SetEntityDataPacket packet) {
            if (player.vehicleData != null) {
                return;
            }

            if (packet.getRuntimeEntityId() != player.runtimeEntityId) {
                return;
            }

            Float height = packet.getMetadata().get(EntityDataTypes.HEIGHT);
            Float width = packet.getMetadata().get(EntityDataTypes.WIDTH);

            final EnumSet<EntityFlag> flags = packet.getMetadata().getFlags();
            if (flags == null && height == null && width == null) {
                return;
            }

            final Set<EntityFlag> flagsCopy;
            if (flags != null) {
                flagsCopy = EnumSet.noneOf(EntityFlag.class);
                flagsCopy.addAll(flags);
            } else {
                flagsCopy = null;
            }

            player.sendLatencyStack(immediate);
            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> {
                if (flagsCopy != null) {
                    flagsCopy.addAll(flags);
                }

                // Dimension seems to be controlled server-side as far as I know (tested with clumsy).

                if (width != null) {
                    player.dimensions = EntityDimensions.fixed(width, player.dimensions.height()).withEyeHeight(player.dimensions.eyeHeight());
                    player.boundingBox = player.dimensions.getBoxAt(player.position);
                    // System.out.println("Update width!");
                }

                if (height != null) {
                    float eyeHeight = 1.62F;
                    if (Math.abs(height - 0.2F) <= 1.0E-3) {
                        eyeHeight = 0.2F;
                    } else if (Math.abs(height - 0.6F) <= 1.0E-3) {
                        eyeHeight = 0.4F;
                    } else if (Math.abs(height - 1.5F) <= 1.0E-3) {
                        eyeHeight = 1.27F;
                    }

                    player.dimensions = EntityDimensions.fixed(player.dimensions.width(), height).withEyeHeight(eyeHeight);
                    player.boundingBox = player.dimensions.getBoxAt(player.position);
                    // System.out.println("Update height!");
                }
            });
        }

        if (event.getPacket() instanceof UpdateAttributesPacket packet) {
            if (packet.getRuntimeEntityId() != player.runtimeEntityId) {
                return;
            }

            player.sendLatencyStack(immediate);
            player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> {
                if (player.vehicleData != null) {
                    return;
                }

                for (final AttributeData data : packet.getAttributes()) {
                    final AttributeInstance attribute = player.attributes.get(data.getName());
                    if (attribute == null) {
                        return;
                    }

                    attribute.clearModifiers();
                    attribute.setBaseValue(data.getDefaultValue());
                    attribute.setValue(data.getValue());

                    // This is useless since there is no modifiers but still be here if Geyser decide to change this in the future.
                    for (AttributeModifierData lv5 : data.getModifiers()) {
                        attribute.addTemporaryModifier(lv5);
                    }
                }
            });
        }
    }
}
