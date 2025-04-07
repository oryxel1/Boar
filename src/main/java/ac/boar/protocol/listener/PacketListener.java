package ac.boar.protocol.listener;

import ac.boar.protocol.event.CloudburstPacketEvent;

public interface PacketListener {
    default void onPacketSend(final CloudburstPacketEvent event, final boolean immediate) {
    }

    default void onPacketReceived(final CloudburstPacketEvent event) {
    }
}