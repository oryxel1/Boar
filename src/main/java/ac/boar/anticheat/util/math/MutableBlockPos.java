package ac.boar.anticheat.util.math;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
@Setter
public final class MutableBlockPos {
    public int x, y, z;

    public MutableBlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MutableBlockPos add(int x, int y, int z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public MutableBlockPos add(Vector3i vector3i) {
        return add(vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }

    public MutableBlockPos set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public MutableBlockPos set(Vector3i vector3i) {
        return set(vector3i.getX(), vector3i.getY(), vector3i.getZ());
    }

    public MutableBlockPos set(Vector3i vector3i, Vector3i vector31) {
        return set(vector3i.getX() + vector31.getX(), vector3i.getY() + vector31.getY(), vector3i.getZ() + vector31.getZ());
    }
}