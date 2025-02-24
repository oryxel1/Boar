package ac.boar.anticheat.data.teleport;

import ac.boar.anticheat.util.math.Vec3;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class TeleportCache {
    private final Vec3 position;
    private final long transactionId;

    private boolean respawnTeleport;
}