package ac.boar.anticheat.data.teleport;

import ac.boar.anticheat.util.math.Vec3f;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TeleportCache {
    private final Vec3f position;
    private final long transactionId;
}