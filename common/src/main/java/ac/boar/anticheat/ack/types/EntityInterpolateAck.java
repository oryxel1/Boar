package ac.boar.anticheat.ack.types;

import ac.boar.anticheat.ack.Acknowledgment;

public record EntityInterpolateAck(long runtimeEntityId, Float posX, Float posY, Float posZ,
                                   Float pitch, Float yaw, Float headYaw, boolean lerp) implements Acknowledgment {
}
