package ac.boar.anticheat.util.math;

import lombok.Getter;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
public class Vec3f implements Cloneable {
    public final static Vec3f ZERO = new Vec3f(0, 0, 0);

    public float x, y, z;

    public Vec3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3f(Vector3f vector3f) {
        this.x = vector3f.getX();
        this.y = vector3f.getY();
        this.z = vector3f.getZ();
    }

    public Vec3f(Vector3i vector3i) {
        this.x = vector3i.getX();
        this.y = vector3i.getY();
        this.z = vector3i.getZ();
    }

    public Vector3f toVector3f() {
        return Vector3f.from(this.x, this.y, this.z);
    }

    public Vector3d toVector3d() {
        return Vector3d.from(this.x, this.y, this.z);
    }

    public float squaredDistanceTo(Vec3f vec) {
        float d = vec.x - this.x;
        float e = vec.y - this.y;
        float f = vec.z - this.z;
        return d * d + e * e + f * f;
    }

    public float distanceTo(Vec3f vec) {
        return (float) Math.sqrt(squaredDistanceTo(vec));
    }

    public float horizontalLength() {
        return (float) Math.sqrt(horizontalLengthSquared());
    }

    public float horizontalLengthSquared() {
        return this.x * this.x + this.z * this.z;
    }

    public float lengthSquared() {
        return this.getX() * this.getX() + this.getY() * this.getY() + this.getZ() * this.getZ();
    }

    public float length() {
        return (float) Math.sqrt(this.lengthSquared());
    }

    public Vec3f add(float v) {
        return this.add(v, v, v);
    }

    public Vec3f add(Vec3f vec3f) {
        return this.add(vec3f.x, vec3f.y, vec3f.z);
    }

    public Vec3f add(float v, float v1, float v2) {
        return new Vec3f(this.x + v, this.y + v1, this.z + v2);
    }

    public Vec3f subtract(Vec3f v) {
        return this.subtract(v.getX(), v.getY(), v.getZ());
    }

    public Vec3f subtract(float v, float v1, float v2) {
        return new Vec3f(this.x - v, this.y - v1, this.z - v2);
    }

    public Vec3f multiply(float a) {
        return this.multiply(a, a, a);
    }

    public Vec3f multiply(float v, float v1, float v2) {
        return new Vec3f(this.x * v, this.y * v1, this.z * v2);
    }

    public Vec3f multiply(Vec3f v) {
        return this.multiply(v.getX(), v.getY(), v.getZ());
    }

    public Vec3f divide(float v, float v1, float v2) {
        return new Vec3f(this.x * v, this.y * v1, this.z * v2);
    }

    public Vec3f up(float v) {
        return new Vec3f(this.getX(), this.getY() + v, this.getZ());
    }

    public Vec3f down(float v) {
        return new Vec3f(this.getX(), this.getY() - v, this.getZ());
    }

    public Vec3f north(float v) {
        return new Vec3f(this.getX(), this.getY(), this.getZ() - v);
    }

    public Vec3f south(float v) {
        return new Vec3f(this.getX(), this.getY(), this.getZ() + v);
    }

    public Vec3f east(float v) {
        return new Vec3f(this.getX() + v, this.getY(), this.getZ());
    }

    public Vec3f west(float v) {
        return new Vec3f(this.getX() - v, this.getY(), this.getZ());
    }

    public Vec3f normalize() {
        float length = this.length();
        if (Math.abs(length) < GenericMath.FLT_EPSILON) {
            return Vec3f.ZERO;
        } else {
            return new Vec3f(this.getX() / length, this.getY() / length, this.getZ() / length);
        }
    }

    public Vec3f clone() {
        return new Vec3f(this.x, this.y, this.z);
    }
}