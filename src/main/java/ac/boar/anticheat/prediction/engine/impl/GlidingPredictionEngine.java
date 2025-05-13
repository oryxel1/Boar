package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.anticheat.util.MathUtil;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.TrigMath;

public class GlidingPredictionEngine extends PredictionEngine {
    public GlidingPredictionEngine(final BoarPlayer player) {
        super(player);
    }

    @Override
    public Vec3 travel(Vec3 vec3) {
        float velX = vec3.x, velY = vec3.y, velZ = vec3.z;

        final Vec3 view = MathUtil.getRotationVector(player.pitch, player.yaw);

        float v8 = player.pitch * MathUtil.DEGREE_TO_RAD;
        float v9 = view.horizontalLength();
        float v10 = view.lengthSquared();
        float v11 = vec3.horizontalLength();
        float v12 = TrigMath.cos(v8);
        float v14 = v12 * (Math.min((float) GenericMath.sqrt(v10) * 2.5F, 1.0F) * v12);

        float v15 = 1.0F / v9;

        velY -= (v14 * 0.75F - 1.0F) * -player.getEffectiveGravity(vec3);
        if (velY < 0.0 && v9 > 0.0) {
            float v21 = velY * -0.1F * v14;
            velZ += (view.z * v21 * v15);
            velY += v21;
            velX += (view.x * v21 * v15);
        }
        if (v8 < 0.0) {
            float v26 = TrigMath.sin(v8) * v11 * -0.039999999F;
            velX -= v26 * view.x * v15;
            velY += v26 * 3.2F;
            velZ -= v26 * view.z * v15;
        }
        if (v9 > 0.0) {
            velX += (v15 * view.x * v11 - velX) * 0.1F;
            velZ += (v15 * view.z * v11 - velZ) * 0.1F;
        }

        if (player.glideBoostTicks > 0) {
            velX += (view.x * 0.1F) + (((view.x * 1.5F) - velX) * 0.5F);
            velY += (view.y * 0.1F) + (((view.y * 1.5F) - velY) * 0.5F);
            velZ += (view.z * 0.1F) + (((view.z * 1.5F) - velZ) * 0.5F);
        }

        return new Vec3(velX * 0.99000001F, velY * 0.98000002f, velZ * 0.99000001F);
    }

    @Override
    public void finalizeMovement() {}
}
