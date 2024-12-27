package ac.boar.anticheat.player.data;

import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.data.EntityPose;
import ac.boar.anticheat.data.StatusEffect;
import ac.boar.anticheat.util.LatencyUtil;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Vec3f;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerData {
    public static final float STEP_HEIGHT = 0.6F;
    public static final float GRAVITY = 0.08F;

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

    // Effect status related
    public final Map<Effect, StatusEffect> statusEffects = new ConcurrentHashMap<>();

    public boolean hasStatusEffect(final Effect effect) {
        return this.statusEffects.containsKey(effect);
    }

    // Movement related, (movement input, player EOT, ...)
    public Vec3f movementInput = Vec3f.ZERO;
    public Vec3f claimedEOT = Vec3f.ZERO, actualVelocity = Vec3f.ZERO;
    public final Map<Long, Vec3f> queuedVelocities = new ConcurrentHashMap<>();

    // Attribute related
    public float movementSpeed = 0.1f;

    // Prediction related
    public EntityPose pose = EntityPose.STANDING;
    public EntityDimensions dimensions = EntityDimensions.POSE_DIMENSIONS.get(EntityPose.STANDING);
    public Box boundingBox = new Box(0, 0, 0, 0, 0, 0);
    public Vec3f eotVelocity = Vec3f.ZERO, predictedVelocity = Vec3f.ZERO;
    public boolean onGround, wasGround;
    public Vector3i supportingBlockPos = null;
    public Vec3f movementMultiplier = Vec3f.ZERO;

    // Prediction related method
    public final float getEffectiveGravity(final Vec3f vec3f) {
        return vec3f.y < 0.0 && this.hasStatusEffect(Effect.SLOW_FALLING) ? Math.min(GRAVITY, 0.01F) : GRAVITY;
    }

    public float getMovementSpeed(float slipperiness) {
        if (onGround) {
            return (this.movementSpeed * (sprinting ? 1.3F : 1)) * (0.21600002F / (slipperiness * slipperiness * slipperiness));
        }

        return sprinting ? 0.025999999F : 0.02F;
    }

    // Others (methods)
    public final void updateBoundingBox(float x, float y, float z) {
        this.boundingBox = calculateBoundingBox(x, y, z);
    }

    public final Box calculateBoundingBox(float x, float y, float z) {
        return this.dimensions.getBoxAt(x, y, z);
    }

    public final void setPose(EntityPose pose) {
        this.pose = pose;
        this.dimensions = EntityDimensions.POSE_DIMENSIONS.get(pose);
    }
}
