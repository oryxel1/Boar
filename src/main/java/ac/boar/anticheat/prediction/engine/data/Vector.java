package ac.boar.anticheat.prediction.engine.data;

import ac.boar.anticheat.util.math.Vec3;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Vector {
    public final static Vector NONE = new Vector(Vec3.ZERO, VectorType.NORMAL);

    private long transactionId = -1;
    private boolean jumping;

    private Vec3 velocity;
    private VectorType type;

    public Vector(final Vec3 vec3, final VectorType type) {
        this.type = type;
        this.velocity = vec3;
    }

    public Vector(final Vec3 vec3, final VectorType type, final long transactionId) {
        this.type = type;
        this.velocity = vec3;
        this.transactionId = transactionId;
    }
}