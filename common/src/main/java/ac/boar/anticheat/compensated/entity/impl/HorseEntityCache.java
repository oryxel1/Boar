package ac.boar.anticheat.compensated.entity.impl;

import ac.boar.anticheat.compensated.entity.BaseEntityCache;
import ac.boar.anticheat.compensated.entity.utils.ClientVehicle;
import ac.boar.anticheat.data.vanilla.AttributeInstance;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.mappings.entity.EntityDefinition;
import ac.boar.mappings.entity.EntityType;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

public class HorseEntityCache extends BaseEntityCache implements ClientVehicle {
    public float playerJumpPendingScale;
    public boolean allowStandSliding;
    public AttributeInstance moveSpeed = new AttributeInstance(0.7f);
    public AttributeInstance jumpStrength = new AttributeInstance(0.42f);
    public Vec3 velocity = Vec3.ZERO;

    public HorseEntityCache(BoarPlayer player, EntityType type, EntityDefinition definition, long runtimeId) {
        super(player, type, definition, runtimeId);

        moveSpeed.clearDirty();
        jumpStrength.clearDirty();
    }

    public void tickJumping(PlayerAuthInputPacket packet) {
        final BoarPlayer player = getPlayer();

        boolean holdingJump = packet.getInputData().contains(PlayerAuthInputData.JUMPING);
        if (player.wasJumping && !holdingJump) {
            playerJumpPendingScale = player.jumpRidingScale * 100f >= 90 ? 1.0F : 0.4F + 0.4F * (player.jumpRidingScale * 100f) / 90.0F;
            allowStandSliding = true;
        } else if (!player.wasJumping && holdingJump) {
            player.jumpingTicks = 0;
            player.jumpRidingScale = 0;
        } else if (holdingJump) {
            if (player.jumpingTicks < 10) {
                player.jumpRidingScale = player.jumpingTicks * 0.1f;
            } else {
                player.jumpRidingScale = 0.8f + 2.0f / (player.jumpingTicks - 9) * 0.1f;
            }
            player.jumpingTicks++;
        }
    }

    @Override
    public Vec3 getRiddenInput(Vec3 input) {
        if (getPlayer().onGround && this.playerJumpPendingScale == 0.0F && getMetadata().getFlag(EntityFlag.STANDING) && !this.allowStandSliding) {
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
        return moveSpeed.getValue();
    }

    @Override
    public boolean shouldSimulateMovement() {
        return getMetadata().getFlag(EntityFlag.SADDLED) && !passengers.isEmpty() && passengers.getFirst() == getPlayer().runtimeEntityId;
    }
}
