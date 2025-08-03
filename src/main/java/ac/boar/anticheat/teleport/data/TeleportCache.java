package ac.boar.anticheat.teleport.data;

import ac.boar.anticheat.util.math.Vec3;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;

@RequiredArgsConstructor
@ToString
@Getter
public class TeleportCache {
    private final long stackId;
    private final Vec3 position;

    @Getter
    public static class Normal extends TeleportCache {
        public Normal(long stackId, Vec3 position) {
            super(stackId, position);
        }
    }

    public static class DimensionSwitch extends TeleportCache {
        public DimensionSwitch(long stackId, Vec3 position) {
            super(stackId, position);
        }
    }

    @ToString
    @Getter
    public static class Rewind extends TeleportCache {
        private final long tick;
        private final Vec3 tickEnd;
        private final boolean onGround;

        public Rewind(long stackId, long tick, Vec3 position, Vec3 tickEnd, boolean onGround) {
            super(stackId, position);
            this.tick = tick;
            this.tickEnd = tickEnd;
            this.onGround = onGround;
        }
    }
}
