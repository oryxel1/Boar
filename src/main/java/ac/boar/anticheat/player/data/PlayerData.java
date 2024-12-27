package ac.boar.anticheat.player.data;

import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.data.EntityPose;
import ac.boar.anticheat.util.LatencyUtil;
import ac.boar.anticheat.util.math.Box;
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

    // "Transaction" related.
    public long lastReceivedId, lastSentId, lastResponseTime = System.currentTimeMillis();
    public final LatencyUtil latencyUtil = new LatencyUtil(this);

    // Movement related, (movement input, player EOT, ...)
    public Vector2f movementInput = Vector2f.ZERO;

    // Prediction related
    public EntityPose pose = EntityPose.STANDING;
    public EntityDimensions dimensions = EntityDimensions.POSE_DIMENSIONS.get(EntityPose.STANDING);
    public Box boundingBox = new Box(0, 0, 0, 0, 0, 0);

    public final void updateBoundingBox(float x, float y, float z) {
        this.boundingBox = calculateBoundingBox(x, y, z);
    }

    public final Box calculateBoundingBox(float x, float y, float z) {
        return this.dimensions.getBoxAt(x, y, z);
    }

    public void setPose(EntityPose pose) {
        this.pose = pose;
        this.dimensions = EntityDimensions.POSE_DIMENSIONS.get(pose);
    }
}
