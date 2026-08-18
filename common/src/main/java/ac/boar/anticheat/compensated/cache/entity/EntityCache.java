package ac.boar.anticheat.compensated.cache.entity;

import ac.boar.anticheat.compensated.cache.entity.state.CachedEntityState;
import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.anticheat.util.reach.PositionInterpolator;
import ac.boar.mappings.entity.EntityDefinition;
import ac.boar.mappings.entity.EntityType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataMap;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;

@ToString
@RequiredArgsConstructor
@Getter
@Setter
public final class EntityCache {
    private final BoarPlayer player;
    private final EntityType type;
    private final EntityDefinition definition;
    private final long runtimeId;

    private EntityDimensions dimensions;
    private Vec3 serverPosition = Vec3.ZERO;
    private boolean inVehicle;
    private float pitch, yaw, headYaw;

    private EntityDataMap metadata = new EntityDataMap();

    public void setMetadata(EntityDataMap metadata) {
        this.metadata.putAll(metadata);

        float width = this.metadata.containsKey(EntityDataTypes.WIDTH)
                ? this.metadata.get(EntityDataTypes.WIDTH)
                : this.definition.width();
        float height = this.metadata.containsKey(EntityDataTypes.HEIGHT)
                ? this.metadata.get(EntityDataTypes.HEIGHT)
                : this.definition.height();
        
        // Geyser uses boat collision dimensions that differ from the behavior data.
        if (this.definition.identifier().equalsIgnoreCase("minecraft:boat") || this.definition.identifier().equalsIgnoreCase("minecraft:chest_boat")) {
            // TODO: Verify these dimensions against Bedrock behavior.
            width = 1.6F;
            height = 0.575F;
        }

        float scale = this.metadata.containsKey(EntityDataTypes.SCALE)
                ? this.metadata.get(EntityDataTypes.SCALE)
                : 1.0F;
        this.dimensions = EntityDimensions.fixed(width * scale, height * scale);
    }

    private CachedEntityState current;

    public boolean affectedByOffset;
    public float getYOffset() {
        if (this.affectedByOffset) {
            return this.definition.offset();
        }

        return 0;
    }

    public void init() {
        this.current = new CachedEntityState(this.player, this);
    }

    public void applyRotation(Float pitch, Float yaw, Float headYaw) {
        if (pitch != null) this.pitch = pitch;
        if (yaw != null) this.yaw = yaw;
        if (headYaw != null) this.headYaw = headYaw;
    }

    public void interpolate(Vec3 pos, boolean lerp) {
        this.interpolate(pos.x, pos.y, pos.z, lerp);
    }

    public void interpolate(Float posX, Float posY, Float posZ, boolean lerp) {
        final PositionInterpolator lv = this.current.getInterpolator();
        Vec3 pos = (lv == null || lv.getTargetPos() == null ?
                this.current.getPos() : lv.getTargetPos()).clone();
        if (posX != null) pos.x = posX;
        if (posY != null) pos.y = posY;
        if (posZ != null) pos.z = posZ;

        if (!lerp) {
            this.current.setTeleportPos(pos);
        } else if (lv != null) {
            lv.refreshPositionAndAngles(pos);
        }
    }

}
