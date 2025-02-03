package ac.boar.anticheat.data;

import ac.boar.anticheat.util.math.Vec3f;

public record VelocityData(long transactionId, long tick, Vec3f velocity) {
}
