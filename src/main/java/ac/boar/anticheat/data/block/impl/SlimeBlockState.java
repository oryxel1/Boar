package ac.boar.anticheat.data.block.impl;

import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.player.BoarPlayer;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.level.block.type.BlockState;

public class SlimeBlockState extends BoarBlockState {
    public SlimeBlockState(BlockState state, Vector3i position, int layer) {
        super(state, position, layer);
    }

    @Override
    public void onSteppedOn(BoarPlayer player, Vector3i vector3i) {
        float d = Math.abs(player.velocity.y);
        if (d < 0.1 && !player.getFlagTracker().has(EntityFlag.SNEAKING)) {
            float e = 0.4F + d * 0.2F;
            player.velocity = player.velocity.multiply(e, 1, e);
        }
    }

    @Override
    public void updateEntityMovementAfterFallOn(BoarPlayer player, boolean living) {
        if (player.getFlagTracker().has(EntityFlag.SNEAKING)) {
            player.velocity.y = 0;
        } else {
            if (player.velocity.y < 0.0) {
                player.velocity.y = -player.velocity.y * (living ? 1 : 0.8F);
            }
        }
    }
}
