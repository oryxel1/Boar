package ac.boar.anticheat.compensated.cache;

import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.anticheat.util.reach.EntityInterpolation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

@ToString
@RequiredArgsConstructor
@Getter
@Setter
public class EntityCache {
    private final EntityType type;
    private final EntityDefinition<?> definition;
    private final EntityDimensions dimensions;
    private final long transactionId, runtimeId;
    private Vector3f position = Vector3f.ZERO, serverPosition = Vector3f.ZERO;

    private EntityInterpolation interpolation, pastInterpolation;

    public void init() {
        this.interpolation = new EntityInterpolation(this, new Vec3(this.position));
    }

    public void doLerping(Vec3 position, boolean lerp) {
        this.pastInterpolation = this.interpolation.clone();

        if (lerp) {
            // Is this the same on bedrock, 3 steps? seems like it.
            this.interpolation.interpolatePosition(position, 3);
        } else {
            this.interpolation.setPosition(position);
        }
    }
}