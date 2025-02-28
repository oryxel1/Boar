package ac.boar.anticheat.packets;

import ac.boar.anticheat.GlobalSetting;
import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.compensated.cache.container.ContainerCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.ticker.PlayerTicker;
import ac.boar.anticheat.util.ChatUtil;
import ac.boar.anticheat.util.math.Vec3;
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

        player.prevUnvalidatedPosition = player.unvalidatedPosition;
        player.unvalidatedPosition = new Vec3(packet.getPosition().sub(0, EntityDefinitions.PLAYER.offset(), 0));

        player.unvalidatedTickEnd = new Vec3(packet.getDelta());
        player.prevVelocity = player.velocity.clone();

        if (player.teleportUtil.teleportInQueue() && GlobalSetting.REWIND_INFO_DEBUG) {
            ChatUtil.alert("Player trying to send position with tick " + player.tick + " while teleporting!");
            return;
        }

        player.tick();
        if (player.lastTickWasTeleport) {
            player.setPos(player.unvalidatedPosition);

            player.sinceTeleport = 0;
            player.velocity = Vec3.ZERO;
            return;
        }

        player.sinceTeleport++;
        player.sinceSpawnIn++;

        if (!player.hasSpawnedIn || player.sinceSpawnIn < 2) {
            final double offset = player.unvalidatedPosition.distanceTo(player.prevUnvalidatedPosition);
            if (offset > 1.0E-7) {
                player.teleportUtil.setbackTo(player.teleportUtil.lastKnowValid);
            }
            return;
        }

        new PlayerTicker(player).tick();

        // Instead of comparing velocity calculate by (pos - prevPos) that player sends to our predicted velocity
        // We compare predicted position to player claimed position, this is because player velocity will never be accurate.
        // For example player want to move by 0.1 block in the Z coord, but due to floating point error, when player add this
        // velocity to their position to move, floating point errors fuck this up and make player move just slightly further or less than
        // what player suppose to be. So we also do the same thing to accounted for this!
        // Also, this is more accurate and a way better method than compare poorly calculated velocity (velocity calculate from pos - prevPos)
        final double offset = player.position.distanceTo(player.unvalidatedPosition);

        correctInputData(player, packet);

        for (Map.Entry<Class<?>, Check> entry : player.checkHolder.entrySet()) {
            Check v = entry.getValue();
            if (v instanceof OffsetHandlerCheck check) {
                check.onPredictionComplete(offset);
            }
        }

        // Have to do this due to loss precision, especially elytra!
        if (player.velocity.distanceTo(player.unvalidatedTickEnd) < player.getMaxOffset()) {
            player.velocity = player.unvalidatedTickEnd.clone();
        }
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
        packet.setDelta(player.prevVelocity.toVector3f());

        if (packet.getInputData().contains(PlayerAuthInputData.START_SPRINTING) && packet.getInputData().contains(PlayerAuthInputData.STOP_SPRINTING)) {
            packet.getInputData().remove(!player.sprinting ? packet.getInputData().contains(PlayerAuthInputData.START_SPRINTING) : PlayerAuthInputData.STOP_SPRINTING);
        }
    }

    public static void processInputMovePacket(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        player.setInputData(packet.getInputData());

        // No other choice than to trust player input, I can't predict this, but I kinda add some protection for abuses in InputA check.
        // This is due to player joystick thingy btw, TODO: figure this out.
        player.input = new Vec3(MathUtil.clamp(packet.getMotion().getX(), -1, 1), 0, MathUtil.clamp(packet.getMotion().getY(), -1, 1));
        player.lastTickWasJoystick = packet.getAnalogMoveVector().lengthSquared() > 0;

        processInputData(player);

        player.prevYaw = player.yaw;
        player.prevPitch = player.pitch;
        player.yaw = packet.getRotation().getY();
        player.pitch = packet.getRotation().getX();

        player.prevInteractRotation = player.interactRotation;
        player.interactRotation = packet.getInteractRotation().clone();

        player.bedrockRotation = packet.getRotation();
        player.cameraOrientation = packet.getCameraOrientation();
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
                    player.gliding = player.compensatedInventory.translate(cache.get(1).getData()).getId() == Items.ELYTRA.javaId();
                    if (!player.gliding) {
                        player.teleportUtil.rewind(player.tick - 1);
                    }
                }
                case STOP_GLIDING -> player.gliding = false;

                // Don't let player do backwards sprinting!
                case START_SPRINTING -> {
                    player.sprinting = player.input.getZ() > 0;
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

        if (player.sprinting && player.input.getZ() <= 0) {
            player.sprinting = false;
            player.sinceSprinting = 1;
        }

//        final StringBuilder builder = new StringBuilder();
//        player.getInputData().forEach(input -> builder.append(input).append(","));
//        Bukkit.broadcastMessage(builder.toString());
    }
}
