package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.data.vanilla.StatusEffect;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.EntityUtil;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.PacketListener;
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class PlayerEffectPackets implements PacketListener {
    @Override
    public void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof MobEffectPacket packet) {
            if (packet.getRuntimeEntityId() != player.runtimeEntityId) {
                return;
            }

            Effect effect = EntityUtil.toJavaEffect(packet.getEffectId());
            if (effect == null) {
                return;
            }

            player.sendLatencyStack(immediate);

            if (packet.getEvent() == MobEffectPacket.Event.ADD || packet.getEvent() == MobEffectPacket.Event.MODIFY) {
                player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> player.getActiveEffects().put(effect, new StatusEffect(effect, packet.getAmplifier(), packet.getDuration() + 1)));
            } else if (packet.getEvent() == MobEffectPacket.Event.REMOVE) {
                player.getLatencyUtil().addTaskToQueue(player.sentStackId.get(), () -> player.getActiveEffects().remove(effect));
            }
        }
    }
}