package ac.boar.anticheat.data.teleport;

import ac.boar.anticheat.util.math.Vec3f;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class TeleportCache {
    private final Vec3f position;
    private final long transactionId;
    private RewindData data;
}