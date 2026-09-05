package ac.boar.anticheat.compensated.entity.utils;

import ac.boar.anticheat.util.math.Vec3;

public interface ClientVehicle {
    // MojMap LivingEntity#getRiddenInput
    Vec3 getRiddenInput(Vec3 input);

    // MojMap LivingEntity#getRiddenSpeed
    float getVehicleSpeed();

    // MojMap Mob#getControllingPassenger
    boolean shouldSimulateMovement();
}
