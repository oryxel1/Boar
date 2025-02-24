package ac.boar.anticheat.data;

import ac.boar.anticheat.util.math.Vec3;

public record VelocityData(long transactionId, long tick, Vec3 velocity) {
}
