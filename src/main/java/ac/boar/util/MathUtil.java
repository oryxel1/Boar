package ac.boar.util;

import ac.boar.anticheat.util.math.Vec3f;

import org.cloudburstmc.math.TrigMath;

public class MathUtil {
    public static float sign(final float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0;
        }

        return value == 0 ? value : value > 0 ? 1 : -1;
    }

    public static Vec3f getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = TrigMath.cos(g);
        float i = TrigMath.sin(g);
        float j = TrigMath.cos(f);
        float k = TrigMath.sin(f);
        return new Vec3f(i * j, -k, h * j);
    }

    public static float square(float v) {
        return v * v;
    }
}
