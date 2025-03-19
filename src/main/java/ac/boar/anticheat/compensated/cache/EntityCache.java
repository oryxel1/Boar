package ac.boar.anticheat.compensated.cache;

import ac.boar.anticheat.data.EntityDimensions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
}