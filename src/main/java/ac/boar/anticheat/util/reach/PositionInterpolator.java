// FabricMC 7c48b8c4
package ac.boar.anticheat.util.reach;

import java.util.function.Consumer;

import ac.boar.anticheat.collision.Collider;
import ac.boar.anticheat.compensated.cache.entity.state.CachedEntityState;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.anticheat.util.MathUtil;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

public class PositionInterpolator {
    public static final int DEFAULT_INTERPOLATION_DURATION = 3;
    private final CachedEntityState entity;
    @Setter
    private int lerpDuration;
    private Data data = new Data(0, Vec3.ZERO);
    @Nullable
    private Vec3 lastPos;
    @Nullable
    private Consumer<PositionInterpolator> callback;

    public PositionInterpolator(CachedEntityState entity) {
        this(entity, 3, null);
    }

    public PositionInterpolator(CachedEntityState entity, int lerpDuration) {
        this(entity, lerpDuration, null);
    }

    public PositionInterpolator(CachedEntityState entity, @Nullable Consumer<PositionInterpolator> callback) {
        this(entity, 3, callback);
    }

    public PositionInterpolator(CachedEntityState entity, int lerpDuration, @Nullable Consumer<PositionInterpolator> callback) {
        this.lerpDuration = lerpDuration;
        this.entity = entity;
        this.callback = callback;
    }

    @Override
    public PositionInterpolator clone() {
        final PositionInterpolator interpolator = new PositionInterpolator(entity);
        interpolator.setLerpDuration(lerpDuration);
        interpolator.lastPos = this.lastPos == null ? null : this.lastPos.clone();
        interpolator.data = new Data(data.step, data.pos.clone());
        interpolator.callback = callback;

        return interpolator;
    }

    public Vec3 getLerpedPos() {
        return this.data.step > 0 ? this.data.pos : this.entity.getPos();
    }

    public void refreshPositionAndAngles(Vec3 pow) {
        if (this.lerpDuration == 0) {
            this.entity.setPos(pow);
            this.clear();
            return;
        }
        this.data.step = this.lerpDuration;
        this.data.pos = pow;
        this.lastPos = this.entity.getPos();
        if (this.callback != null) {
            this.callback.accept(this);
        }
    }

    public boolean isInterpolating() {
        return this.data.step > 0;
    }

    public void tick() {
        if (!this.isInterpolating()) {
            this.clear();
            return;
        }
        float d = 1.0F / this.data.step;
        if (this.lastPos != null) {
            Vec3 lv = this.entity.getPos().subtract(this.lastPos);
            if (this.entity.getPlayer().compensatedWorld.noCollision(this.entity.calculateBoundingBox().offset(this.data.pos.add(lv)))) {
                this.data.addPos(lv);
            }
        }
        float e = MathUtil.lerp(d, this.entity.getPos().getX(), this.data.pos.x);
        float h = MathUtil.lerp(d, this.entity.getPos().getY(), this.data.pos.y);
        float i = MathUtil.lerp(d, this.entity.getPos().getZ(), this.data.pos.z);
        Vec3 lv2 = new Vec3(e, h, i);
        this.entity.setPos(lv2);
        this.data.tick();
        this.lastPos = lv2;
    }

    public void clear() {
        this.data.step = 0;
        this.lastPos = null;
    }

    static class Data {
        protected int step;
        Vec3 pos;

        Data(int step, Vec3 pos) {
            this.step = step;
            this.pos = pos;
        }

        public void tick() {
            --this.step;
        }

        public void addPos(Vec3 pos) {
            this.pos = this.pos.add(pos);
        }
    }
}

