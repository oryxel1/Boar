package ac.boar.util;

import ac.boar.anticheat.util.math.Vec3;

import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector3i;

public class MathUtil {
    public final static float DEGREE_TO_RAG = 0.017453292F;

    public static float sign(final float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0;
        }

        return value == 0 ? value : value > 0 ? 1 : -1;
    }

    public static float square(float v) {
        return v * v;
    }

    public static boolean compare(final Vector3i vector3i, final Vector3i vector3i1) {
        return vector3i != null && vector3i1 != null && vector3i.getX() == vector3i1.getX() && vector3i.getY() == vector3i1.getY() && vector3i.getZ() == vector3i1.getZ();
    }

    public static boolean isValid(final Vector3i vector3i) {
        return Float.isFinite(vector3i.getX()) && Float.isFinite(vector3i.getY()) &&
                Float.isFinite(vector3i.getZ());
    }

    public static Vec3 getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = TrigMath.cos(g);
        float i = TrigMath.sin(g);
        float j = TrigMath.cos(f);
        float k = TrigMath.sin(f);
        return new Vec3(i * j, -k, h * j);
    }

    public static Vec3 movementInputToVelocity(final Vec3 movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 lv = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
            float h = TrigMath.sin(yaw * DEGREE_TO_RAG);
            float i = TrigMath.cos(yaw * DEGREE_TO_RAG);
            return new Vec3(lv.x * i - lv.z * h, lv.y, lv.z * i + lv.x * h);
        }
    }
}
