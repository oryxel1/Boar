package ac.boar.anticheat.teleport.data;

import ac.boar.anticheat.util.math.Vec3;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TeleportCache {
    private final long stackId;
    private final Vec3 position;

    @Getter
    public static class Normal extends TeleportCache {
        private final boolean keepVelocity;

        public Normal(long stackId, Vec3 position, boolean keepVelocity) {
            super(stackId, position);
            this.keepVelocity = keepVelocity;
        }
    }

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
