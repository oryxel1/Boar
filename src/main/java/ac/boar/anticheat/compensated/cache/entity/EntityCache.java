package ac.boar.anticheat.compensated.cache.entity;

import ac.boar.anticheat.compensated.cache.entity.state.CachedEntityState;
import ac.boar.anticheat.data.EntityDimensions;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.anticheat.util.reach.PositionInterpolator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

@ToString
@RequiredArgsConstructor
@Getter
@Setter
public final class EntityCache {
    private final BoarPlayer player;
    private final EntityType type;
    private final EntityDefinition<?> definition;
    private final EntityDimensions dimensions;
    private final long transactionId, runtimeId;
    private Vec3 serverPosition = Vec3.ZERO;

    private CachedEntityState past, current;

    public void init() {
        this.current = new CachedEntityState(this.player, this);
    }

    public void interpolate(Vec3 pos, boolean lerp) {
        this.past = this.current.clone();

        if (!lerp) {
            this.current.setPos(pos);
        } else {
            final PositionInterpolator lv = this.current.getInterpolator();
            if (lv != null) {
                lv.refreshPositionAndAngles(pos);
            }
        }
    }
}