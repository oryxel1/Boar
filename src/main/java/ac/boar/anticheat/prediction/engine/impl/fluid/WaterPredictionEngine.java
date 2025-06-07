package ac.boar.anticheat.prediction.engine.impl.fluid;

import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.inventory.item.BedrockEnchantment;
import org.geysermc.geyser.network.GameProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

import java.util.Map;

public class WaterPredictionEngine extends PredictionEngine {
    private float tickEndSpeed;
    public WaterPredictionEngine(BoarPlayer player) {
        super(player);
    }

    @Override
    public Vec3 travel(Vec3 vec3) {
        ItemData boostSlot = player.compensatedInventory.armorContainer.get(3).getData();
        Map<BedrockEnchantment, Integer> enchantments = CompensatedInventory.getEnchantments(boostSlot);
        Integer depthStrider = enchantments.get(BedrockEnchantment.DEPTH_STRIDER);

        float h = 0;
        if (depthStrider != null) {
            h = 0.33333334f + 0.33333334f * (float)(depthStrider - 1);
        }

        this.tickEndSpeed = h;

        return this.moveRelative(vec3, h > 0 ? 0.02F + ((player.getSpeed() - 0.02F) * h) : 0.02F);
    }

    @Override
    public void finalizeMovement() {
        boolean fastTickEnd = player.getFlagTracker().has(EntityFlag.SWIMMING);

        // On versions below 1.21.80 player can move fast in water by sprinting without swimming but on 1.21.80 this is fixed.
        // HOWEVER, this bugs one again reintroduce itself on 1.21.81+ for certain reason.
        if (player.getFlagTracker().has(EntityFlag.SPRINTING) && !fastTickEnd) {
            if (!GameProtocol.is1_21_80orHigher(player.getSession())) {
                fastTickEnd = true;
            } else {
                Vec3 slow = player.velocity.multiply(0.8F + ((0.54600006f - 0.8F) * this.tickEndSpeed),
                        0.8F, 0.8F + ((0.54600006f - 0.8F) * this.tickEndSpeed));

                // We have to guess based off player claimed tick end since there is no way to know if this is 1.21.80 or 1.21.81+
                if (slow.horizontalLengthSquared() < player.unvalidatedTickEnd.horizontalLengthSquared()) {
                    fastTickEnd = true;
                }
            }
        }

        float f = fastTickEnd ? 0.9F : 0.8F;
        f += (0.54600006f - f) * this.tickEndSpeed;

        player.velocity = player.velocity.multiply(f, 0.8F, f);
        player.velocity = this.getFluidFallingAdjustedMovement(player.getEffectiveGravity(), player.velocity);
    }

    private Vec3 getFluidFallingAdjustedMovement(float gravity, Vec3 motion) {
        if (player.hasEffect(Effect.LEVITATION)) {
            float y = motion.y + (((player.getEffect(Effect.LEVITATION).getAmplifier() + 1) * 0.05F) - motion.y) * 0.2F;
            return new Vec3(motion.x, y, motion.z);
        }

        if (gravity != 0.0 && !player.getFlagTracker().has(EntityFlag.SWIMMING)) {
            return new Vec3(motion.x, motion.y - (gravity / 16.0F), motion.z);
        }

        return motion;
    }
}