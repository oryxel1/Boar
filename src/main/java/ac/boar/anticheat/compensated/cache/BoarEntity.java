package ac.boar.anticheat.compensated.cache;

import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.util.math.Box;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

@RequiredArgsConstructor
@Getter
@Setter
public class BoarEntity {
    private final EntityType type;
    private final EntityDefinition<?> definition;
    private final EntityDimensions dimensions;
    private final long transactionId, runtimeId;
    private Vector3f position = Vector3f.ZERO, serverPosition = Vector3f.ZERO;
    private Box boundingBox = Box.EMPTY, prevBoundingBox = null;
}