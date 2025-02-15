package ac.boar.anticheat.handler;

import ac.boar.anticheat.data.BreakingData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.util.MathUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class BreakingBlockValidator {
    private final static List<PlayerActionType> allowedActions = List.of(PlayerActionType.START_BREAK, PlayerActionType.STOP_BREAK, PlayerActionType.CONTINUE_BREAK);

    private final BoarPlayer player;

    private final List<BreakingData> cachedBlockBreak = new ArrayList<>();

    @Getter
    private final List<PlayerBlockActionData> valid = new ArrayList<>();

    public void handle(final PlayerAuthInputPacket packet) {
        if (!this.valid.isEmpty()) {
            // Geyser refused to translate these actions, rather they fail Geyser validation check,
            // or we're the one cancelling auth input packet causing this.
            // Whatever the case is, we need to resync player world.
            for (final PlayerBlockActionData action : this.valid) {
                if (action.getAction() == PlayerActionType.CONTINUE_BREAK) {
                    continue;
                }

                this.resyncBlock(action.getBlockPosition());
            }

            this.valid.clear();
        }

        final boolean isPerformingBlockAction = packet.getInputData().contains(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS);

        if (isPerformingBlockAction) {
            handleBlockAction(packet);
        }

        final boolean isPerformingInteraction = packet.getInputData().contains(PlayerAuthInputData.PERFORM_ITEM_INTERACTION);
        final ItemUseTransaction transaction = packet.getItemUseTransaction();
        if (transaction != null && transaction.getActionType() == 2 && isPerformingInteraction) {
            final Vector3i position = transaction.getBlockPosition();
            final int face = transaction.getBlockFace();
            if (position == null || !MathUtil.isValid(position) || face < 0 || face >= Direction.VALUES.length) {
                packet.getInputData().remove(PlayerAuthInputData.PERFORM_ITEM_INTERACTION);
                packet.setItemUseTransaction(null);
                return;
            }

            final double distance = position.distance(player.x, player.y, player.z);
            if (distance > 12) {
                this.resyncBlock(position);
                packet.setItemUseTransaction(null);
                packet.getInputData().remove(PlayerAuthInputData.PERFORM_ITEM_INTERACTION);
                return;
            }

            final BlockState state = player.compensatedWorld.getBlockState(position);
            boolean canBreak = state.block().destroyTime() != -1 && !state.is(Blocks.AIR) && !state.is(Blocks.CAVE_AIR) && !state.is(Blocks.VOID_AIR)
                    && !state.is(Blocks.LAVA) && !state.is(Blocks.WATER);

            final BreakingData data = findCacheUsingPosition(position);
            if (data == null || !canBreak) {
                packet.setItemUseTransaction(null);
                this.resyncBlock(position);
            } else {
                if (data.getBreakingProcess() >= 1) {
                    player.compensatedWorld.updateBlock(data.getPosition(), Block.JAVA_AIR_ID);

                    // We have to manually save this so we can handle it later on lol.
                    final PlayerBlockActionData action = new PlayerBlockActionData();
                    action.setAction(PlayerActionType.STOP_BREAK);
                    action.setBlockPosition(position);
                    action.setFace(face);
                    this.valid.add(action);
                } else {
                    packet.setItemUseTransaction(null);
                    this.resyncBlock(position);
                }

                this.cachedBlockBreak.remove(data);
            }
        }

        // So geyser won't attempt to process null transaction use.
        if (packet.getItemUseTransaction() == null && isPerformingInteraction) {
            packet.getInputData().remove(PlayerAuthInputData.PERFORM_ITEM_INTERACTION);
        }
    }

    private void handleBlockAction(final PlayerAuthInputPacket packet) {
        this.valid.clear();

        for (final PlayerBlockActionData action : packet.getPlayerActions()) {
            if ((action.getBlockPosition() == null || !MathUtil.isValid(action.getBlockPosition()) && action.getAction()
                    != PlayerActionType.START_BREAK)
                    || (action.getFace() < 0 || action.getFace() >= Direction.VALUES.length) && action.getAction()
                    != PlayerActionType.ABORT_BREAK||
                    !allowedActions.contains(action.getAction())) {
                continue;
            }

            final BreakingData data = findCacheUsingPosition(action.getBlockPosition());

            if (data != null) {
                final double distance = data.getPosition().distance(player.x, player.y, player.z);
                if (distance > 12) {
                    this.resyncBlock(data.getPosition());
                    continue;
                }
            }

            if (action.getAction() == PlayerActionType.START_BREAK) {
                if (data != null) {
                    this.cachedBlockBreak.remove(data);
                }
                this.cachedBlockBreak.add(new BreakingData(action.getAction(), action.getBlockPosition(), action.getFace()));

                this.valid.add(action);
            }

            // In case of data == null, we can just ignore it, since we're going to cancel
            // digging when player send stop digging anyway.
            if (action.getAction() == PlayerActionType.CONTINUE_BREAK && data != null) {
                tickBreaking(data);
                data.setFace(action.getFace());
                data.setState(PlayerActionType.CONTINUE_BREAK);
                this.valid.add(action);
            }
        }

        packet.getPlayerActions().clear();
        packet.getPlayerActions().addAll(this.valid);

        if (packet.getPlayerActions().isEmpty()) {
            packet.getInputData().remove(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS);
        }
    }

    private void tickBreaking(final BreakingData data) {
        // TODO: implement this
        // also before implement this I will have to implement inventory compensation lol.
        data.setBreakingProcess(1);
    }

    // TODO: resync item.
    private void resyncBlock(final Vector3i vector3i) {
        final BlockState state = player.getSession().getGeyser().getWorldManager().blockAt(player.getSession(), vector3i);
        state.block().updateBlock(player.getSession(), state, vector3i);
    }

    private BreakingData findCacheUsingPosition(final Vector3i vector3i) {
        for (final BreakingData data : this.cachedBlockBreak) {
            if (MathUtil.compare(vector3i, data.getPosition())) {
                return data;
            }
        }

        return null;
    }
}
