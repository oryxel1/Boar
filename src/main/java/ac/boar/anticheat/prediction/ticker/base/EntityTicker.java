package ac.boar.anticheat.prediction.ticker.base;

import ac.boar.anticheat.collision.Collider;
import ac.boar.anticheat.data.BoarBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Vec3;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;

@RequiredArgsConstructor
public class EntityTicker {
    protected final BoarPlayer player;

    public void tick() {
        this.baseTick();
    }
    public void baseTick() {}

    protected final void doSelfMove(Vec3 vec3) {
        if (player.stuckSpeedMultiplier.lengthSquared() > 1.0E-7) {
            vec3 = vec3.multiply(player.stuckSpeedMultiplier);
            player.stuckSpeedMultiplier = Vec3.ZERO;
            player.velocity = Vec3.ZERO.clone();
        }

        Vec3 vec32 = Collider.collide(player, vec3 = Collider.maybeBackOffFromEdge(player, vec3));
        player.setPos(player.prevUnvalidatedPosition.add(vec32));

        boolean bl = !MathUtil.equal(vec3.x, vec32.x);
        boolean bl2 = !MathUtil.equal(vec3.z, vec32.z);
        player.horizontalCollision = bl || bl2;
        player.verticalCollision = vec3.y != vec32.y;
        player.onGround = player.verticalCollision && vec3.y < 0.0;
        // this.minorHorizontalCollision = this.horizontalCollision ? this.isHorizontalCollisionMinor(vec32) : false;
        Vector3i blockPos = player.getOnPos(0.2F);
        BoarBlockState blockState = player.compensatedWorld.getBlockState(blockPos, 0);
//        if (this.isLocalInstanceAuthoritative()) {
//            this.checkFallDamage(vec32.y, this.onGround(), blockState, blockPos);
//        }
        if (player.horizontalCollision) {
            player.velocity = new Vec3(bl ? 0 : player.velocity.x, player.velocity.y, bl2 ? 0 : player.velocity.z);
        }

        if (vec3.y != vec32.y) {
            blockState.updateEntityMovementAfterFallOn(player, true);
        }


        // There is none of this thing on Bedrock.
//        float f = this.getBlockSpeedFactor();
//        this.setDeltaMovement(this.getDeltaMovement().multiply(f, 1.0, f));
//        profilerFiller.pop();
    }
}