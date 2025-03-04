package ac.boar.anticheat.util.reach;

import ac.boar.anticheat.compensated.cache.EntityCache;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.util.MathUtil;
import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
public class EntityInterpolation {
    private final EntityCache entity;

    private Vec3 serverPosition, position;
    private int step;

    public EntityInterpolation(final EntityCache entity, final Vec3 position) {
        this.entity = entity;
        this.serverPosition = this.position = position;
        this.step = 0;
    }

    public void tick() {
        if (this.step > 0) {
            this.step();
        }
    }

    public Box getBoundingBox() {
        return this.entity.getDimensions().getBoxAt(this.position);
    }

    public void setPosition(final Vec3 vec3) {
        this.serverPosition = this.position = vec3;
    }

    public void interpolatePosition(Vec3 position, int interpolationSteps) {
        this.serverPosition = position;
        this.step = interpolationSteps;
    }

    private void step() {
        float j = 1.0F / this.step;
        float x = MathUtil.lerp(j, this.position.x, this.serverPosition.x);
        float y = MathUtil.lerp(j, this.position.y, this.serverPosition.y);
        float z = MathUtil.lerp(j, this.position.z, this.serverPosition.z);

        // Bukkit.broadcastMessage("step! current=" + this.position + ", target=" + this.serverPosition);

        this.position = new Vec3(x, y, z);
        this.step--;
    }

    @Override
    public EntityInterpolation clone()  {
        final EntityInterpolation cloned = new EntityInterpolation(entity, position);
        cloned.serverPosition = this.serverPosition;
        cloned.step = this.step;

        return cloned;
    }
}
