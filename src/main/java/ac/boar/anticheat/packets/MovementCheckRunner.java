package ac.boar.anticheat.packets;

import ac.boar.anticheat.GlobalSetting;
import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.data.teleport.RewindData;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.ticker.PlayerTicker;
import ac.boar.anticheat.util.ChatUtil;
import ac.boar.anticheat.util.math.Vec3f;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;

import ac.boar.util.MathUtil;

import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.cloudburstmc.protocol.bedrock.packet.SetLocalPlayerAsInitializedPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.item.Items;

import java.util.Map;

public class MovementCheckRunner implements CloudburstPacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();

        if (event.getPacket() instanceof SetLocalPlayerAsInitializedPacket packet) {
            if (packet.getRuntimeEntityId() != player.runtimeEntityId || player.hasSpawnedIn) {
                return;
            }

            player.hasSpawnedIn = true;
            player.sinceSpawnIn = 0;
        }

        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        // Just to be safe.
        if (player.mcplSession == null) {
            player.disconnect("Failed to find MCPL session, please rejoin.");
            return;
        }

        if (player.tick == Long.MIN_VALUE) {
            player.tick = Math.max(0, packet.getTick()) - 1;
        }
        player.tick++;
        if (packet.getTick() != player.tick || packet.getTick() < 0) {
            player.disconnect("Invalid tick id=" + packet.getTick());
            return;
        }

        player.breakingValidator.handle(packet);

        processInputMovePacket(player, packet);

        player.prevX = player.x;
        player.prevY = player.y;
        player.prevZ = player.z;
        player.x = packet.getPosition().getX();
        player.y = packet.getPosition().getY() - EntityDefinitions.PLAYER.offset();
        player.z = packet.getPosition().getZ();

        player.actualVelocity = new Vec3f(player.x - player.prevX, player.y - player.prevY, player.z - player.prevZ);
        player.claimedEOT = new Vec3f(packet.getDelta());
        player.prevEotVelocity = player.eotVelocity.clone();

        if (player.teleportUtil.teleportInQueue() && GlobalSetting.REWIND_INFO_DEBUG) {
            ChatUtil.alert("Player trying to send position with tick " + player.tick + " while teleporting!");
            return;
        }

        player.tick();
        if (player.lastTickWasTeleport) {
            player.sinceTeleport = 0;
            player.eotVelocity = Vec3f.ZERO;
            player.updateBoundingBox(player.x, player.y, player.z);
            return;
        }

        player.sinceTeleport++;
        player.sinceSpawnIn++;
        if (player.sinceTeleport == 1 && player.teleportUtil.prevRewindTeleport != null) {
            final RewindData data = player.teleportUtil.prevRewindTeleport;
            player.teleportUtil.rewind(player.tick - 1, data.before(), data.after());
            return;
        }

        if (!player.hasSpawnedIn || player.sinceSpawnIn < 2) {
            final double offset = player.actualVelocity.distanceTo(Vec3f.ZERO);
            if (offset > 1.0E-7) {
                player.teleportUtil.setbackTo(null, player.teleportUtil.lastKnowValid);
            }
            player.postPredictionVelocities.clear();
            return;
        }

        new PlayerTicker(player).tick();
        final double offset = player.predictedVelocity.distanceTo(player.actualVelocity);

        if (player.canControlEOT()) {
            player.eotVelocity = player.claimedEOT;
        }

        correctInputData(player, packet);

        // Player didn't accept rewind teleport properly, rewind again!
        if (player.lastTickWasRewind && player.teleportUtil.prevRewind != null && !player.teleportUtil.teleportInQueue() && offset > player.getMaxOffset()) {
            final RewindData data = player.teleportUtil.prevRewind;
            long tickDistance = player.tick - data.tick();

            // We're past the point where we can rewind, and trying to rewind past this point (even if we send the latest tick id) it wouldn't act correctly.
            // Or player accept the position but not velocity, or complicated.....
            // Solution? We send a normal teleport and then rewind teleport after that!
            if (!player.teleportUtil.getSavedKnowValid().containsKey(data.tick()) || tickDistance > GlobalSetting.TICKS_TILL_FORCE_REWIND) {
                player.teleportUtil.setbackTo(data, player.teleportUtil.lastKnowValid);
            } else {
                player.teleportUtil.rewind(data);
            }

            player.postPredictionVelocities.clear();
            return;
        }

        for (Map.Entry<Class<?>, Check> entry : player.checkHolder.entrySet()) {
            Check v = entry.getValue();
            if (v instanceof OffsetHandlerCheck check) {
                check.onPredictionComplete(offset);
            }
        }

        player.postPredictionVelocities.clear();
    }

    public static void processInputMovePacket(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        player.setInputData(packet.getInputData());

        player.movementInput = new Vec3f(MathUtil.sign(packet.getMotion().getX()), 0, MathUtil.sign(packet.getMotion().getY()));

        processInputData(player);

        player.prevYaw = player.yaw;
        player.prevPitch = player.pitch;
        player.yaw = packet.getRotation().getY();
        player.pitch = packet.getRotation().getX();

        player.bedrockRotation = packet.getRotation();
        player.cameraOrientation = packet.getCameraOrientation();
    }

    private void correctInputData(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        if (player.wasFlying || player.flying) {
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

        // Technically this should be eotVelocity, but since geyser check for this once instead of previous for the ground status
        // We will have to "correct" this one to previous eot velocity so that ground status is properly calculated!
        // Prevent cheater simply send (0, 0, 0) value to never be on ground ("NoGround" no-fall), and never receive fall damage.
        packet.setDelta(player.prevEotVelocity.toVector3f());

        if (packet.getInputData().contains(PlayerAuthInputData.START_SPRINTING) && packet.getInputData().contains(PlayerAuthInputData.STOP_SPRINTING)) {
            packet.getInputData().remove(!player.sprinting ? packet.getInputData().contains(PlayerAuthInputData.START_SPRINTING) : PlayerAuthInputData.STOP_SPRINTING);
        }
    }

    public static void processInputData(final BoarPlayer player) {
        player.wasFlying = player.flying;
        player.wasSprinting = player.sprinting;
        player.wasSneaking = player.sneaking;
        player.wasGliding = player.gliding;
        player.wasSwimming = player.swimming;

        for (final PlayerAuthInputData input : player.getInputData()) {
            switch (input) {
                case START_GLIDING -> {
                    final ContainerCache cache = player.compensatedInventory.armorContainer;

                    // Prevent player from spoofing elytra gliding.
                    player.gliding = player.compensatedInventory.translate(cache.get(1)).getId() == Items.ELYTRA.javaId();
                    if (!player.gliding) {
                        player.teleportUtil.rewind(player.tick - 1, player.beforeCollisionVelocity, player.predictedVelocity);
                    }
                }
                case STOP_GLIDING -> player.gliding = false;

                // Don't let player do backwards sprinting!
                case START_SPRINTING -> {
                    player.sprinting = player.movementInput.getZ() > 0;
                    player.setSprinting(player.sprinting);
                }
                case STOP_SPRINTING -> {
                    player.sprinting = false;
                    player.setSprinting(false);
                }
                case START_SNEAKING -> player.sneaking = true;
                case STOP_SNEAKING -> player.sneaking = false;

                case START_SWIMMING -> player.swimming = true;
                case STOP_SWIMMING -> player.swimming = false;

                case START_FLYING -> player.flying = player.abilities.contains(Ability.MAY_FLY) || player.abilities.contains(Ability.FLYING);
                case STOP_FLYING -> player.flying = false;
            }
        }

        if (player.sprinting) {
            player.sinceSprinting = 0;
        } else {
            player.sinceSprinting++;
        }

        if (player.sprinting && player.movementInput.getZ() <= 0) {
            player.sprinting = false;
            player.sinceSprinting = 1;
        }

//        final StringBuilder builder = new StringBuilder();
//        player.getInputData().forEach(input -> builder.append(input).append(","));
//        Bukkit.broadcastMessage(builder.toString());
    }
}
