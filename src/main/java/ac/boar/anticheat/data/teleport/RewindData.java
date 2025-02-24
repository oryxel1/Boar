package ac.boar.anticheat.data.teleport;

import ac.boar.anticheat.util.math.Vec3;

public record RewindData(long tick, Vec3 before, Vec3 after) {
}
