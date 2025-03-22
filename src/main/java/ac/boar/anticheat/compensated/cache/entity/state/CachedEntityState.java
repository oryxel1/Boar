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
    private Vec3 pos = Vec3.ZERO;
    private PositionInterpolator interpolator = new PositionInterpolator(this, 3);

    public void tick() {
        if (this.isInterpolating()) {
            this.interpolator.tick();
        }
    }

    public boolean isInterpolating() {
        return this.getInterpolator() != null && this.getInterpolator().isInterpolating();
    }

    public Box getBoundingBox() {
        return this.calculateBoundingBox();
    }

    public Box calculateBoundingBox() {
        return this.entity.getDimensions().getBoxAt(this.pos);
    }

    @Override
    public CachedEntityState clone() {
        final CachedEntityState state = new CachedEntityState(player, entity);
        state.setPos(this.pos.clone());
        state.setInterpolator(this.interpolator == null ? null : this.interpolator.clone());

        return state;
    }
}
