package ac.boar.anticheat.data.input;

import ac.boar.anticheat.util.math.Vec3;

public record VelocityData(long stackId, long tick, Vec3 velocity) {
}
