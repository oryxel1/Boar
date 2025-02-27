package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.util.MathUtil;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.TrigMath;

public class PredictionEngineGliding extends PredictionEngine {
    public PredictionEngineGliding(final BoarPlayer player) {
        super(player);
    }

    // Uhhhhh I will clean this up later, ig?
    @Override
    protected Vec3 travel(Vec3 vec3) {
        float v8; // xmm10_4
        float v9; // xmm8_4
        float v10; // xmm6_4
        float v11; // xmm12_4
        float v12; // xmm0_4
        float v14; // xmm11_4
        float v16; // xmm1_4
        float v17; // xmm5_4
        float v18; // xmm9_4
        float v19; // xmm3_4
        float v20; // xmm4_4
        float v21; // xmm3_4
        float v22; // xmm1_4
        float v23; // xmm0_4
        float v24; // xmm0_4
        float v25; // xmm1_4
        float v26; // xmm0_4
        float v27; // xmm2_4
        float v28; // xmm4_4
        float v29; // xmm1_4
        float v30; // xmm0_4
        float v32; // [rsp+20h] [rbp-A8h] BYREF
        // float v33; // [rsp+24h] [rbp-A4h]
        float v34; // [rsp+28h] [rbp-A0h]

        float velX = vec3.x, velY = vec3.y, velZ = vec3.z;

        Vec3 view = MathUtil.getRotationVector(player.pitch, player.yaw);
        v32 = view.x;
        // v33 = view.y;
        v34 = view.z;

        v8 = player.pitch * 0.017453292F;
        v9 = view.horizontalLength();
        v10 = view.lengthSquared();
        v11 = vec3.horizontalLength();
        v12 = TrigMath.cos(v8);
        v14 = v12 * (float)(Math.min(GenericMath.sqrt(v10) * 2.5, 1.0) * v12);
        v16 = -player.getEffectiveGravity(vec3);

        v17 = v34;
        v18 = v32;
        v19 = velY - ((float)((float)(v14 * 0.75) - 1.0) * v16);
        velY = v19;
        v20 = v19;
        if (v19 < 0.0 && v9 > 0.0 ) {
            v21 = (float)(v19 * -0.1) * v14;
            v22 = (v17 * v21 * (float) (1.0 / v9)) + velZ;
            velX = (v18 * v21 * (float) (1.0 / v9)) + velX;
            v23 = v21 + velY;
            velZ = v22;
            velY = v23;
            v20 = v23;
        }
        if (v8 < 0.0) {
            v24 = TrigMath.sin(v8);
            v25 = velX;
            v18 = v32;
            v17 = v34;
            v26 = v24 * v11 * -0.039999999F;
            v27 = v26 * v32;
            v28 = v26 * v34 * (float) (1.0 / v9);
            velY = (float)(v26 * 3.2) + velY;
            velZ = velZ - v28;
            velX = v25 - (v27 * (float)(1.0 / v9));
            v20 = velY;
        }
        if (v9 > 0.0) {
            v29 = (float)((((float) (1.0 / v9) * v18 * v11) - velX) * 0.1) + velX;
            velZ = (float)((((float) (1.0 / v9) * v17 * v11) - velZ) * 0.1) + velZ;
            velX = v29;
        }
        velX = velX * 0.99000001F;
        v30 = velZ * 0.99000001F;
        velY = v20 * 0.98000002f;
        velZ = v30;

        return new Vec3(velX, velY, velZ);
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
