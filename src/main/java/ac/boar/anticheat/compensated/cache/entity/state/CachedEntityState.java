package ac.boar.anticheat.compensated.cache.entity.state;

import ac.boar.anticheat.compensated.cache.entity.EntityCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.anticheat.util.reach.PositionInterpolator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public final class CachedEntityState {
    private final BoarPlayer player;
    private final EntityCache entity;
    private Vec3 prevPos = Vec3.ZERO;
    private Vec3 pos = Vec3.ZERO;
    private PositionInterpolator interpolator = new PositionInterpolator(this, 3);

    public void tick() {
        if (this.isInterpolating()) {
            this.interpolator.tick();
        } else {
            this.prevPos = this.pos.clone();
        }
    }

    public boolean isInterpolating() {
        return this.getInterpolator() != null && this.getInterpolator().isInterpolating();
    }

    public Box getBoundingBox() {
        return this.calculateBoundingBox();
    }

    public Box getBoundingBox(float f) {
        return this.calculateBoundingBox(this.prevPos.add(this.pos.subtract(this.prevPos).multiply(f)));
    }

    public Box calculateBoundingBox() {
        return this.entity.getDimensions().getBoxAt(this.pos);
    }

    public Box calculateBoundingBox(Vec3 vec3) {
        return this.entity.getDimensions().getBoxAt(vec3);
    }

    public void setTeleportPos(Vec3 pos) {
        this.prevPos = this.pos = pos;
    }

    public void setPos(Vec3 pos) {
        this.prevPos = this.pos;
        this.pos = pos;
    }

    @Override
    public CachedEntityState clone() {
        final CachedEntityState state = new CachedEntityState(player, entity);
        state.setPos(this.pos.clone());
        state.setPrevPos(this.prevPos.clone());
        state.setInterpolator(this.interpolator == null ? null : this.interpolator.clone());

        return state;
    }
}
