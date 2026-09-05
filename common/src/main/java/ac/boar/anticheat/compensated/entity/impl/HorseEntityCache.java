package ac.boar.anticheat.compensated.entity.impl;

import ac.boar.anticheat.compensated.entity.BaseEntityCache;
import ac.boar.anticheat.compensated.entity.utils.ClientVehicle;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.mappings.entity.EntityDefinition;
import ac.boar.mappings.entity.EntityType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

public class HorseEntityCache extends BaseEntityCache implements ClientVehicle {
    public float playerJumpPendingScale;
    public boolean onGround;
    public boolean allowStandSliding;
    public float moveSpeed = 0.7f;
    public float jumpStrength = 0.42f;

    public HorseEntityCache(BoarPlayer player, EntityType type, EntityDefinition definition, long runtimeId) {
        super(player, type, definition, runtimeId);
    }

    @Override
    public Vec3 getRiddenInput(Vec3 input) {
        if (onGround && this.playerJumpPendingScale == 0.0F && getMetadata().getFlag(EntityFlag.STANDING) && !this.allowStandSliding) {
            return Vec3.ZERO;
        } else {
            float sideways = input.x * 0.5F;
            float forward = input.z;
            if (forward <= 0.0F) {
                forward *= 0.25F;
            }

            return new Vec3(sideways, 0, forward);
        }
    }

    @Override
    public float getVehicleSpeed() {
        return moveSpeed;
    }

    @Override
    public boolean shouldSimulateMovement() {
        return getMetadata().getFlag(EntityFlag.SADDLED) && !passengers.isEmpty() && passengers.getFirst() == getPlayer().runtimeEntityId;
    }
}
