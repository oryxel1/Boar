package ac.boar.anticheat.data.teleport;

import ac.boar.anticheat.data.PredictionData;
import ac.boar.anticheat.util.math.Vec3;

public record RewindData(long tick, Vec3 position, PredictionData data) {
}
