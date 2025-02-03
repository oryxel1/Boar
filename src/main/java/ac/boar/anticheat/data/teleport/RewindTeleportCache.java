package ac.boar.anticheat.data.teleport;

import ac.boar.anticheat.util.math.Vec3f;
import lombok.Getter;

@Getter
public class RewindTeleportCache extends TeleportCache {
    private final long tick;
    private final Vec3f lastPosition;
    private final Vec3f velocity;
    private final boolean onGround;

    public RewindTeleportCache(long tick, Vec3f lastPosition, Vec3f position, Vec3f velocity, boolean onGround, long transactionId) {
        super(position, transactionId);

        this.lastPosition = lastPosition;

        this.tick = tick;
        this.velocity = velocity;

        this.onGround = onGround;
    }
}
