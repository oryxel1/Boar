package ac.boar.anticheat.data.teleport;

import ac.boar.anticheat.util.math.Vec3f;

public record RewindData(long tick, Vec3f before, Vec3f after) {
}
