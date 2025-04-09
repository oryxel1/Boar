package ac.boar.anticheat.validator;

import ac.boar.anticheat.data.BreakingData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.block.BlockUtil;
import ac.boar.anticheat.util.MathUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.level.block.type.Block;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Direction;
import org.geysermc.geyser.translator.protocol.bedrock.BedrockInventoryTransactionTranslator;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public final class BreakingBlockValidator {
    private final static List<PlayerActionType> allowedActions = List.of(PlayerActionType.START_BREAK, PlayerActionType.STOP_BREAK, PlayerActionType.CONTINUE_BREAK, PlayerActionType.ABORT_BREAK);

    private final BoarPlayer player;

    private final List<BreakingData> cachedBlockBreak = new ArrayList<>();

    public void handle(final PlayerAuthInputPacket packet) {
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

            final double distance = position.distance(player.position.toVector3i());
            if (distance > 12) {
                this.resyncBlock(position);
                packet.setItemUseTransaction(null);
                packet.getInputData().remove(PlayerAuthInputData.PERFORM_ITEM_INTERACTION);
                return;
            }

            final BlockState state = player.compensatedWorld.getBlockState(position, 0).getState();
            final BreakingData data = findCacheUsingPosition(position);
            if (data == null || !BlockUtil.determineCanBreak(player, state)) {
                packet.setItemUseTransaction(null);
                this.resyncBlock(position);
            } else {
                if (data.getBreakingProcess() >= 1) {
                    player.compensatedWorld.updateBlock(data.getPosition(), 0, player.BEDROCK_AIR);
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
        for (final PlayerBlockActionData action : packet.getPlayerActions()) {
            if ((action.getBlockPosition() == null || !MathUtil.isValid(action.getBlockPosition()) && action.getAction()
                    != PlayerActionType.START_BREAK)
                    || (action.getFace() < 0 || action.getFace() >= Direction.VALUES.length) && action.getAction()
                    != PlayerActionType.ABORT_BREAK
                    || !allowedActions.contains(action.getAction())) {
                continue;
            }

            final BreakingData data = findCacheUsingPosition(action.getBlockPosition());

            if (data != null) {
                final double distance = data.getPosition().distance(player.position.toVector3i());
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
            }

            // In case of data == null, we can just ignore it, since we're going to cancel
            // digging when player send stop digging anyway.
            if (action.getAction() == PlayerActionType.CONTINUE_BREAK && data != null) {
                tickBreaking(data);
                data.setFace(action.getFace());
                data.setState(PlayerActionType.CONTINUE_BREAK);
            }

            if (action.getAction() == PlayerActionType.ABORT_BREAK & data != null) {
                this.cachedBlockBreak.remove(data);
            }
        }

        packet.getPlayerActions().clear();

        if (packet.getPlayerActions().isEmpty()) {
            packet.getInputData().remove(PlayerAuthInputData.PERFORM_BLOCK_ACTIONS);
        }
    }

    private void tickBreaking(final BreakingData data) {
        // TODO: implement this
        // also before implement this I will have to implement inventory compensation lol.
        data.setBreakingProcess(1);
    }

    private void resyncBlock(final Vector3i vector3i) {
        BedrockInventoryTransactionTranslator.restoreCorrectBlock(player.getSession(), vector3i);
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
