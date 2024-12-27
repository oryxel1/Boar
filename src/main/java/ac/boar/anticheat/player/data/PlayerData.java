package ac.boar.anticheat.player.data;

import ac.boar.anticheat.util.LatencyUtil;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;

import java.util.HashSet;
import java.util.Set;

public class PlayerData {
    @Getter
    private final Set<PlayerAuthInputData> inputData = new HashSet<>();

    public long tick;

    // Position, rotation, other.
    public float prevX, x, prevY, y, prevZ, z;
    public float prevYaw, yaw, prevPitch, pitch;

    // Sprinting, sneaking, swimming and other status.
    public boolean wasSprinting, sprinting, wasSneaking, sneaking, wasGliding, gliding;
    public boolean flying;

    // Information about this tick.
    public boolean lastTickWasTeleport;

    // Movement related, (movement input, player EOT, ...)
    public Vector2f movementInput = Vector2f.ZERO;

    // "Transaction" related.
    public long lastReceivedId, lastSentId, lastResponseTime = System.currentTimeMillis();
    public final LatencyUtil latencyUtil = new LatencyUtil(this);
}
