package ac.boar.anticheat.packets.input.legacy;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.data.input.VelocityData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.UncertainRunner;
import ac.boar.anticheat.prediction.engine.data.VectorType;
import ac.boar.anticheat.util.InputUtil;

import ac.boar.anticheat.util.math.Vec3;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.item.Items;

import java.util.Iterator;
import java.util.Map;

public class LegacyAuthInputPackets {
    public static void updateUnvalidatedPosition(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        player.prevUnvalidatedPosition = player.unvalidatedPosition.clone();
        player.unvalidatedPosition = new Vec3(packet.getPosition().sub(0, player.getYOffset(), 0));
        player.unvalidatedTickEnd = new Vec3(packet.getDelta());
    }

    public static void doPostPrediction(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        final UncertainRunner uncertainRunner = new UncertainRunner(player);

        // Properly calculated offset by comparing position instead of poorly calculated velocity that get calculated using (pos - prevPos) to account for floating point errors.
        double offset = player.position.distanceTo(player.unvalidatedPosition);
        float extraOffset = uncertainRunner.extraOffset(offset);
        offset -= extraOffset;

        uncertainRunner.doTickEndUncertain();
        correctInputData(player, packet);

        for (Map.Entry<Class<?>, Check> entry : player.getCheckHolder().entrySet()) {
            Check v = entry.getValue();
            if (v instanceof OffsetHandlerCheck check) {
                check.onPredictionComplete(offset);
            }
        }

        // Have to do this due to loss precision, especially elytra!
        if (player.velocity.distanceTo(player.unvalidatedTickEnd) - extraOffset < player.getMaxOffset()) {
            player.velocity = player.unvalidatedTickEnd.clone();
        }

        if (offset < player.getMaxOffset()) {
            Vec3 oldPrevPos = player.prevPosition;
            player.setPos(player.unvalidatedPosition.clone());
            player.prevPosition = oldPrevPos;
        }

        // Also clear out old velocity.
        if (player.bestPossibility.getType() == VectorType.VELOCITY) {
            Iterator<Map.Entry<Long, VelocityData>> iterator = player.queuedVelocities.entrySet().iterator();

            Map.Entry<Long, VelocityData> entry;
            while (iterator.hasNext() && (entry = iterator.next()) != null) {
                if (entry.getKey() > player.bestPossibility.getStackId()) {
                    break;
                } else {
                    iterator.remove();
                }
            }
        }
    }

    public static void correctInputData(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        if (player.isFullyExempted()) {
            return;
        }

        // https://github.com/GeyserMC/Geyser/blob/master/core/src/main/java/org/geysermc/geyser/translator/protocol/bedrock/entity/player/input/BedrockMovePlayer.java#L90
        // Geyser check for our vertical collision for calculation for ground, do this to prevent possible no-fall bypass.
        packet.getInputData().remove(PlayerAuthInputData.HORIZONTAL_COLLISION);
        packet.getInputData().remove(PlayerAuthInputData.VERTICAL_COLLISION);

        if (player.horizontalCollision) {
            packet.getInputData().add(PlayerAuthInputData.HORIZONTAL_COLLISION);
        }

        if (player.verticalCollision) {
            packet.getInputData().add(PlayerAuthInputData.VERTICAL_COLLISION);
        }

        // Prevent player from spoofing this to trick Geyser into sending the wrong ground status.
        packet.setDelta(player.velocity.toVector3f());
    }

    public static void processAuthInput(final BoarPlayer player, final PlayerAuthInputPacket packet, boolean processInputData) {
        player.setInputData(packet.getInputData());

        InputUtil.processInput(player, packet);

        player.prevYaw = player.yaw;
        player.prevPitch = player.pitch;
        player.yaw = packet.getRotation().getY();
        player.pitch = packet.getRotation().getX();

        player.rotation = packet.getRotation();
        player.interactRotation = packet.getInteractRotation().clone();

        player.inputMode = packet.getInputMode();

        if (processInputData) {
            processInputData(player);
        }
    }

    public static void processInputData(final BoarPlayer player) {
        player.wasFlying = player.flying;

        for (final PlayerAuthInputData input : player.getInputData()) {
            switch (input) {
                case START_GLIDING -> {
                    final ContainerCache cache = player.compensatedInventory.armorContainer;

                    // Prevent player from spoofing elytra gliding.
                    player.getFlagTracker().set(EntityFlag.GLIDING, player.compensatedInventory.translate(cache.get(1).getData()).getId() == Items.ELYTRA.javaId());
                    if (!player.getFlagTracker().has(EntityFlag.GLIDING)) {
                        player.getTeleportUtil().rewind(player.tick - 1);
                    }
                }
                case STOP_GLIDING -> {
                    player.getFlagTracker().set(EntityFlag.GLIDING, false);
                    player.glideBoostTicks = 0;
                }

                // Don't let player do backwards sprinting!
                case START_SPRINTING -> player.setSprinting(player.input.getZ() > 0);
                case STOP_SPRINTING -> player.setSprinting(false);
                case START_SNEAKING -> player.getFlagTracker().set(EntityFlag.SNEAKING, true);
                case STOP_SNEAKING -> player.getFlagTracker().set(EntityFlag.SNEAKING, false);

                case START_SWIMMING -> player.getFlagTracker().set(EntityFlag.SWIMMING, player.touchingWater && player.submergedInWater);
                case STOP_SWIMMING -> player.getFlagTracker().set(EntityFlag.SWIMMING, false);

                case START_FLYING -> player.flying = player.abilities.contains(Ability.MAY_FLY) || player.abilities.contains(Ability.FLYING);
                case STOP_FLYING -> player.flying = false;

                case STOP_SPIN_ATTACK -> {
                    if (player.dirtySpinStop) {
                        player.stopRiptide();
                        player.velocity = player.velocity.multiply(-0.2F);
                    }
                }

                case START_USING_ITEM -> {
                    if (player.getUseItemCache().getConsumer() != null) {
                        player.getUseItemCache().getConsumer().accept(player.getUseItemCache());
                    }
                }
            }
        }

        if (player.getUseItemCache().getConsumer() != null) {
            player.getUseItemCache().setConsumer(null);

            // A bit hacky but ok.
            if (!player.getInputData().contains(PlayerAuthInputData.START_USING_ITEM)) {
                player.getSession().releaseItem();
            }
        }

        player.dirtySpinStop = false;

//        final StringBuilder builder = new StringBuilder();
//        player.getInputData().forEach(input -> builder.append(input).append(","));
//        Bukkit.broadcastMessage(builder.toString());
    }
}
