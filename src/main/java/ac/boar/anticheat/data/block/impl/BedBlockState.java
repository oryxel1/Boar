package ac.boar.anticheat.data.block.impl;

import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.type.BlockState;

import java.util.List;

public class BedBlockState extends BoarBlockState {
    public BedBlockState(BlockState state, Vector3i position, int layer) {
        super(state, position, layer);
    }

    @Override
    public void updateEntityMovementAfterFallOn(BoarPlayer player, boolean living) {
        // Don't ask questions, just know that it does works.
        List<Box> boxes = this.findCollision(player, getPosition(), null, false);
        float maxY = 0;
        for (Box box : boxes) {
            maxY = Math.max(maxY, box.maxY);
        }

        if (player.position.y - maxY > 0.5) {
            player.velocity.y = 0;
            return;
        }

        final float d = living ? 1.0F : 0.8F;
        Vec3 velocity = player.velocity.clone();
        velocity.y = -velocity.y * 0.75F * d;

        if (velocity.y > 0.75) {
            velocity.y = 0.75F;
        }

        if (player.velocity.y < 0) {
            if (player.unvalidatedTickEnd.y > 0) {
                player.velocity = velocity;
            } else {
                player.velocity.y = 0;
            }
        } else {
            player.velocity = velocity;
        }
    }
}
