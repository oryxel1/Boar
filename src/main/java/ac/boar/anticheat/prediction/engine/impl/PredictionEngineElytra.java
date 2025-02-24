package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.util.MathUtil;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.TrigMath;

public class PredictionEngineElytra extends PredictionEngine {
    public PredictionEngineElytra(final BoarPlayer player) {
        super(player);
    }

    @Override
    protected Vec3 travel(Vec3 vec3) {
        if (player.onGround) {
            player.gliding = false;
        }

        Vec3 oldVelocity = vec3.clone();
        Vec3 vec3d = MathUtil.getRotationVector(player.pitch, player.yaw);
        float f = player.pitch * 0.017453292F;
        float d = (float) GenericMath.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        float e = oldVelocity.horizontalLength();
        float g = player.getEffectiveGravity(oldVelocity);
        float h = MathUtil.square(TrigMath.cos(f));
        oldVelocity = oldVelocity.add(0.0F, g * (-1.0F + h * 0.75F), 0.0F);
        float i;
        if (oldVelocity.y < 0.0 && d > 0.0) {
            i = oldVelocity.y * -0.1F * h;
            oldVelocity = oldVelocity.add(vec3d.x * i / d, i, vec3d.z * i / d);
        }

        if (f < 0.0F && d > 0.0) {
            i = e * (-TrigMath.sin(f)) * 0.04F;
            oldVelocity = oldVelocity.add(-vec3d.x * i / d, i * 3.2F, -vec3d.z * i / d);
        }

        if (d > 0.0) {
            oldVelocity = oldVelocity.add((vec3d.x / d * e - oldVelocity.x) * 0.1F, 0.0F, (vec3d.z / d * e - oldVelocity.z) * 0.1F);
        }

        return oldVelocity.multiply(0.99F, 0.98F, 0.99F);
    }

    @Override
    protected Vec3 jump(Vec3 vec3) {
        return vec3;
    }

    @Override
    protected boolean shouldJump() {
        return false;
    }
}
