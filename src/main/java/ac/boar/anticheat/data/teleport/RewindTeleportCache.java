package ac.boar.anticheat.data.teleport;

import ac.boar.anticheat.util.math.Vec3;
import lombok.Getter;

@Getter
public class RewindTeleportCache extends TeleportCache {
    private final long tick;
    private final Vec3 lastPosition;
    private final Vec3 velocity;
    private final RewindData data;
    private final boolean onGround;

    public RewindTeleportCache(long tick, RewindData data, Vec3 lastPosition, Vec3 position, Vec3 velocity, boolean onGround, long transactionId) {
        super(position, transactionId);

        this.lastPosition = lastPosition;

        this.tick = tick;
        this.velocity = velocity;

        this.onGround = onGround;

        this.data = data;
    }
}
