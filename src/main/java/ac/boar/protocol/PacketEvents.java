package ac.boar.protocol;

import lombok.Getter;

import ac.boar.protocol.listener.*;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PacketEvents {
    @Getter
    private final static PacketEvents api = new PacketEvents();
    private final Cloudburst cloudburst = new Cloudburst();
    private final MCPL mcpl = new MCPL();

    public void terminate() {
        this.cloudburst.terminate();
        this.mcpl.terminate();
    }

    @Getter
    public static class Cloudburst {
        private final List<CloudburstPacketListener> listeners = new ArrayList<>();

        public void register(final CloudburstPacketListener listener) {
            this.listeners.add(listener);
        }

        private void terminate() {
            this.listeners.clear();
        }
    }

    @Getter
    public static class MCPL {
        private final List<MCPLPacketListener> listeners = new ArrayList<>();

        public void register(final MCPLPacketListener listener) {
            this.listeners.add(listener);
        }

        private void terminate() {
            this.listeners.clear();
        }
    }
}
