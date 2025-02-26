package ac.boar.anticheat.util.math;

import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;

public class Box implements Cloneable {
    public final static Box EMPTY = new Box(0, 0, 0, 0, 0, 0);

    public final static float EPSILON = 1.0E-7F;

    public final float minX, minY, minZ;
    public final float maxX, maxY, maxZ;

    public Box(float x1, float y1, float z1, float x2, float y2, float z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public Box(final BoundingBox boundingBox) {
        this.minX = (float) boundingBox.getMin(Axis.X);
        this.minY = (float) boundingBox.getMin(Axis.Y);
        this.minZ = (float) boundingBox.getMin(Axis.Z);
        this.maxX = (float) boundingBox.getMax(Axis.X);
        this.maxY = (float) boundingBox.getMax(Axis.Y);
        this.maxZ = (float) boundingBox.getMax(Axis.Z);
    }

    public Vec3 toVec3f(float width) {
        return new Vec3(this.minX + (width / 2F), this.minY, this.maxZ - (width / 2F));
    }

    public static Box of(Vec3 center, float dx, float dy, float dz) {
        return new Box(center.x - dx / 2.0F, center.y - dy / 2.0F, center.z - dz / 2.0F, center.x + dx / 2.0F, center.y + dy / 2.0F, center.z + dz / 2.0F);
    }

    public float chooseMin(Axis axis) {
        return switch (axis) {
            case X -> this.minX;
            case Y -> this.minY;
            default -> this.minZ;
        };
    }

    public float chooseMax(Axis axis) {
        return switch (axis) {
            case X -> this.maxX;
            case Y -> this.maxY;
            default -> this.maxZ;
        };
    }

    public boolean isOverlapped(Axis axis, Box other) {
        return switch (axis) {
            case X -> other.maxY - this.minY > 0 && this.maxY - other.minY > 0 && other.maxZ - this.minZ > 0 && this.maxZ - other.minZ > 0;
            case Y -> other.maxX - this.minX > 0 && this.maxX - other.minX > 0 && other.maxZ - this.minZ > 0 && this.maxZ - other.minZ > 0;
            default -> other.maxX - this.minX > 0 && this.maxX - other.minX > 0 && other.maxY - this.minY > 0 && this.maxY - other.minY >0;
        };
    }

    public float calculateMaxDistance(Axis axis, Box other, float maxDist) {
        if (!isOverlapped(axis, other) || maxDist == 0) {
            return maxDist;
        }

        if (maxDist > 0) {
            float d1 = chooseMin(axis) - other.chooseMax(axis);

            if (d1 >= -1.0E-3) {
                maxDist = Math.min(maxDist, d1);
            }
        } else {
            float d0 = chooseMax(axis) - other.chooseMin(axis);

            if (d0 <= 1.0E-3) {
                maxDist = Math.max(maxDist, d0);
            }
        }
        return maxDist;
    }

    public Box withMinX(float minX) {
        return new Box(minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public Box withMinY(float minY) {
        return new Box(this.minX, minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public Box withMinZ(float minZ) {
        return new Box(this.minX, this.minY, minZ, this.maxX, this.maxY, this.maxZ);
    }

    public Box withMaxX(float maxX) {
        return new Box(this.minX, this.minY, this.minZ, maxX, this.maxY, this.maxZ);
    }

    public Box withMaxY(float maxY) {
        return new Box(this.minX, this.minY, this.minZ, this.maxX, maxY, this.maxZ);
    }

    public Box withMaxZ(float maxZ) {
        return new Box(this.minX, this.minY, this.minZ, this.maxX, this.maxY, maxZ);
    }

    public Box shrink(float x, float y, float z) {
        float d = this.minX;
        float e = this.minY;
        float f = this.minZ;
        float g = this.maxX;
        float h = this.maxY;
        float i = this.maxZ;
        if (x < 0.0) {
            d -= x;
        } else if (x > 0.0) {
            g -= x;
        }

        if (y < 0.0) {
            e -= y;
        } else if (y > 0.0) {
            h -= y;
        }

        if (z < 0.0) {
            f -= z;
        } else if (z > 0.0) {
            i -= z;
        }

        return new Box(d, e, f, g, h, i);
    }

    public Box stretch(Vec3 scale) {
        return this.stretch(scale.x, scale.y, scale.z);
    }

    public Box stretch(float x, float y, float z) {
        float d = this.minX;
        float e = this.minY;
        float f = this.minZ;
        float g = this.maxX;
        float h = this.maxY;
        float i = this.maxZ;
        if (x < 0.0) {
            d += x;
        } else if (x > 0.0) {
            g += x;
        }

        if (y < 0.0) {
            e += y;
        } else if (y > 0.0) {
            h += y;
        }

        if (z < 0.0) {
            f += z;
        } else if (z > 0.0) {
            i += z;
        }

        return new Box(d, e, f, g, h, i);
    }

    public Box expand(float x, float y, float z) {
        float d = this.minX - x;
        float e = this.minY - y;
        float f = this.minZ - z;
        float g = this.maxX + x;
        float h = this.maxY + y;
        float i = this.maxZ + z;
        return new Box(d, e, f, g, h, i);
    }

    public Box expand(float value) {
        return this.expand(value, value, value);
    }

    public Box intersection(Box Box) {
        float d = Math.max(this.minX, Box.minX);
        float e = Math.max(this.minY, Box.minY);
        float f = Math.max(this.minZ, Box.minZ);
        float g = Math.min(this.maxX, Box.maxX);
        float h = Math.min(this.maxY, Box.maxY);
        float i = Math.min(this.maxZ, Box.maxZ);
        return new Box(d, e, f, g, h, i);
    }

    public Box union(Box Box) {
        float d = Math.min(this.minX, Box.minX);
        float e = Math.min(this.minY, Box.minY);
        float f = Math.min(this.minZ, Box.minZ);
        float g = Math.max(this.maxX, Box.maxX);
        float h = Math.max(this.maxY, Box.maxY);
        float i = Math.max(this.maxZ, Box.maxZ);
        return new Box(d, e, f, g, h, i);
    }

    public Box offset(float x, float y, float z) {
        return new Box(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    public Box offset(Vec3 vec) {
        return this.offset(vec.x, vec.y, vec.z);
    }

    public boolean intersects(Box Box) {
        return this.intersects(Box.minX, Box.minY, Box.minZ, Box.maxX, Box.maxY, Box.maxZ);
    }

    public boolean intersects(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.minX < maxX && this.maxX > minX && this.minY < maxY && this.maxY > minY && this.minZ < maxZ && this.maxZ > minZ;
    }

    public boolean intersects(Vec3 pos1, Vec3 pos2) {
        return this.intersects(Math.min(pos1.x, pos2.x), Math.min(pos1.y, pos2.y), Math.min(pos1.z, pos2.z), Math.max(pos1.x, pos2.x), Math.max(pos1.y, pos2.y), Math.max(pos1.z, pos2.z));
    }

    public boolean contains(Vec3 pos) {
        return this.contains(pos.x, pos.y, pos.z);
    }

    public boolean contains(float x, float y, float z) {
        return x >= this.minX && x < this.maxX && y >= this.minY && y < this.maxY && z >= this.minZ && z < this.maxZ;
    }

    public Box contract(float x, float y, float z) {
        return this.expand(-x, -y, -z);
    }

    public Box contract(float value) {
        return this.expand(-value);
    }

    public Vec3 getMinPos() {
        return new Vec3(this.minX, this.minY, this.minZ);
    }

    public Vec3 getMaxPos() {
        return new Vec3(this.maxX, this.maxY, this.maxZ);
    }

    public float getAverageSideLength() {
        float d = this.getLengthX();
        float e = this.getLengthY();
        float f = this.getLengthZ();
        return (d + e + f) / 3;
    }

    public float getLengthX() {
        return this.maxX - this.minX;
    }

    public float getLengthY() {
        return this.maxY - this.minY;
    }

    public float getLengthZ() {
        return this.maxZ - this.minZ;
    }

    public boolean isNaN() {
        return Double.isNaN(this.minX) || Double.isNaN(this.minY) || Double.isNaN(this.minZ) || Double.isNaN(this.maxX) || Double.isNaN(this.maxY) || Double.isNaN(this.maxZ);
    }

    public String toString() {
        return "AABB[" + this.minX + ", " + this.minY + ", " + this.minZ + "] -> [" + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
    }

    @Override
    public Box clone() {
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }
}