package ac.boar.anticheat.prediction.engine.data;

import ac.boar.anticheat.util.math.Vec3f;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Vector {
    private long transactionId = -1;
    private boolean jumping;

    private Vec3f velocity;
    private VectorType type;

    public Vector(final Vec3f vec3f, final VectorType type) {
        this.type = type;
        this.velocity = vec3f;
    }

    public Vector(final Vec3f vec3f, final VectorType type, final long transactionId) {
        this.type = type;
        this.velocity = vec3f;
        this.transactionId = transactionId;
    }
}