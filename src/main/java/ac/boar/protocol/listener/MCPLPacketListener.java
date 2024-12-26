package ac.boar.protocol.listener;

import ac.boar.protocol.event.MCPLPacketEvent;

public interface MCPLPacketListener {
    default void onPacketSend(final MCPLPacketEvent event) {
    }
}
