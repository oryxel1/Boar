package ac.boar.anticheat.packets.player;

import ac.boar.anticheat.data.StatusEffect;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.EntityUtil;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;
import org.cloudburstmc.protocol.bedrock.packet.MobEffectPacket;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public class PlayerEffectPacket implements CloudburstPacketListener {
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

            player.sendTransaction();

            if (packet.getEvent() == MobEffectPacket.Event.ADD) {
                player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> player.activeEffects.put(effect, new StatusEffect(effect, packet.getAmplifier(), packet.getDuration())));
            } else if (packet.getEvent() == MobEffectPacket.Event.REMOVE) {
                player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> player.activeEffects.remove(effect));
            }
        }
    }
}