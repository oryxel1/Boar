package ac.boar.anticheat.player.data;

import ac.boar.anticheat.GlobalSetting;
import ac.boar.anticheat.data.*;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.prediction.engine.data.Vector;
import ac.boar.anticheat.prediction.engine.data.VectorType;
import ac.boar.anticheat.util.LatencyUtil;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Vec3;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.attribute.AttributeModifierData;
import org.cloudburstmc.protocol.bedrock.data.attribute.AttributeOperation;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerData {
    private final static AttributeModifierData SPRINTING_SPEED_BOOST = new AttributeModifierData("D208FC00-42AA-4AAD-9276-D5446530DE43",
            "Sprinting speed boost",
            0.3F, AttributeOperation.MULTIPLY_TOTAL, 2, false);

    public final static float JUMP_HEIGHT = 0.42F;
    public final static float STEP_HEIGHT = 0.6F;
    public final static float GRAVITY = 0.08F;

    @Getter
    @Setter
    private Set<PlayerAuthInputData> inputData = new HashSet<>();

    public long tick = Long.MIN_VALUE;
    public boolean hasSpawnedIn = false;
    public long sinceSpawnIn;

    public GameType gameType = GameType.DEFAULT;

    // Position, rotation, other.
    public Vec3 unvalidatedPosition = Vec3.ZERO, prevUnvalidatedPosition = Vec3.ZERO;

    public Vec3 position = Vec3.ZERO;
    public float prevYaw, yaw, prevPitch, pitch;
    public Vector3f bedrockRotation = Vector3f.ZERO, cameraOrientation = Vector3f.ZERO;

    // Sprinting, sneaking, swimming and other status.
    public boolean wasSprinting, sprinting, wasSneaking, sneaking, wasGliding, gliding, wasSwimming, swimming;
    public boolean wasFlying, flying;
    public int sinceSprinting, sinceTeleport;

    // Information about this tick.
    public boolean lastTickWasTeleport, lastTickWasRewind;

    // "Transaction" related.
    public long lastReceivedId, lastSentId, lastResponseTime = System.currentTimeMillis();
    public final LatencyUtil latencyUtil = new LatencyUtil(this);

    // Effect status related
    public final Map<Effect, StatusEffect> statusEffects = new ConcurrentHashMap<>();

    public boolean hasStatusEffect(final Effect effect) {
        return this.statusEffects.containsKey(effect);
    }

    // Movement related, (movement input, player EOT, ...)
    public Vec3 movementInput = Vec3.ZERO;
    public Vec3 claimedEOT = Vec3.ZERO;
    public final Map<Long, VelocityData> queuedVelocities = new ConcurrentHashMap<>();

    public VelocityData getSupposedVelocity() {
        final Iterator<Map.Entry<Long, VelocityData>> iterator = this.queuedVelocities.entrySet().iterator();

        VelocityData velocity = null;
        Map.Entry<Long, VelocityData> entry;
        while (iterator.hasNext() && (entry = iterator.next()) != null) {
            if (this.lastReceivedId < entry.getKey()) {
                break;
            }
            iterator.remove();
            velocity = entry.getValue();
        }

        this.velocityData = velocity;
        return velocity;
    }

    // Attribute related, abilities
    public final Map<String, PlayerAttributeData> attributes = new HashMap<>();
    public final Set<Ability> abilities = new HashSet<>();

    // Prediction related
    public EntityPose pose = EntityPose.STANDING, prevPose = EntityPose.STANDING;
    public EntityDimensions dimensions = EntityDimensions.POSE_DIMENSIONS.get(EntityPose.STANDING);
    public Box boundingBox = Box.EMPTY;
    public Vec3 prevEotVelocity = Vec3.ZERO, eotVelocity = Vec3.ZERO;
    public PredictionData predictedData = new PredictionData(Vector.NONE, Vec3.ZERO, Vec3.ZERO);
    public Vector closetVector = new Vector(Vec3.ZERO, VectorType.NORMAL);
    public VelocityData velocityData;
    public boolean onGround, wasGround;
    public Vec3 movementMultiplier = Vec3.ZERO;

    public final Map<Long, PredictionData> postPredictionVelocities = new HashMap<>();
    public final Map<Long, PlayerAuthInputPacket> savedInputMap = new ConcurrentSkipListMap<>();

    // only for debugging
    public PredictionEngine engine;

    public float fallDistance = 0;

    public boolean submergedInWater, touchingWater;
    public boolean wasInPowderSnow, inPowderSnow;

    public boolean horizontalCollision, verticalCollision;

    public final Map<Fluid, Float> fluidHeight = new HashMap<>();
    public final List<Fluid> submergedFluidTag = new CopyOnWriteArrayList<>();

    // Prediction related method
    public final double getMaxOffset() {
        return GlobalSetting.PLAYER_POSITION_ACCEPTANCE_THRESHOLD;
    }

    public final boolean canControlEOT() {
        return false;
    }

    public final void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
        final PlayerAttributeData lv = this.attributes.get(GeyserAttributeType.MOVEMENT_SPEED.getBedrockIdentifier());
        if (lv == null) {
            // wtf?
            return;
        }

        lv.removeModifier(SPRINTING_SPEED_BOOST.getId());
        if (sprinting) {
            lv.addTemporaryModifier(SPRINTING_SPEED_BOOST);
        }
    }

    public boolean isInLava() {
        return this.tick != 1 && this.fluidHeight.getOrDefault(Fluid.LAVA, 0F) > 0.0;
    }

    public final float getEffectiveGravity(final Vec3 vec3) {
        return vec3.y < 0.0 && this.hasStatusEffect(Effect.SLOW_FALLING) ? Math.min(GRAVITY, 0.01F) : GRAVITY;
    }

    public float getSwimHeight() {
        return this.dimensions.eyeHeight() < 0.4 ? 0.0F : 0.4F;
    }

    public float getMovementSpeed() {
        return this.attributes.get(GeyserAttributeType.MOVEMENT_SPEED.getBedrockIdentifier()).getValue();
    }

    public float getMovementSpeed(float slipperiness) {
        if (onGround) {
            return this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness));
        }

        return sprinting ? 0.025999999F : 0.02F;
    }

    // Others (methods)
    public final void setPos(Vec3 vec3) {
        this.position = vec3;
        this.setBoundingBox(vec3);
    }

    public final void setBoundingBox(Vec3 vec3) {
        this.boundingBox = this.dimensions.getBoxAt(vec3.x, vec3.y, vec3.z);
    }

    public final void setPose(EntityPose pose) {
        this.prevPose = pose;
        this.pose = pose;
        this.dimensions = EntityDimensions.POSE_DIMENSIONS.get(pose);
    }
}
