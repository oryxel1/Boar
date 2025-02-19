package ac.boar.anticheat.compensated.cache.container;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class ContainerCache {
    private final byte id;
    private final ContainerType type;
    private final Vector3i blockPosition;
    private final long uniqueEntityId;

    @Setter
    private List<ItemData> contents = new ObjectArrayList<>();
}
