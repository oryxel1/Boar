package ac.boar.anticheat.packets;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.ticker.PlayerTicker;
import ac.boar.anticheat.util.math.Vec3f;
import ac.boar.protocol.event.CloudburstPacketEvent;
import ac.boar.protocol.listener.CloudburstPacketListener;

import ac.boar.util.MathUtil;

import org.bukkit.Bukkit;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.entity.EntityDefinitions;

public class MovementCheckRunner implements CloudburstPacketListener {
    @Override
    public void onPacketReceived(final CloudburstPacketEvent event) {
        final BoarPlayer player = event.getPlayer();
        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }

        // Just to be safe.
        if (player.mcplSession == null) {
            player.disconnect("Failed to find MCPL session, please rejoin.");
            return;
        }

        player.tick = packet.getTick();

        player.getInputData().clear();
        player.getInputData().addAll(packet.getInputData());

        player.movementInput = new Vec3f(MathUtil.sign(packet.getMotion().getX()), 0, MathUtil.sign(packet.getMotion().getY()));
        this.processInputData(player);

        player.prevX = player.x;
        player.prevY = player.y;
        player.prevZ = player.z;
        player.x = packet.getPosition().getX();
        player.y = packet.getPosition().getY() - EntityDefinitions.PLAYER.offset();
        player.z = packet.getPosition().getZ();

        player.bedrockRotation = packet.getRotation();

        player.prevYaw = player.yaw;
        player.prevPitch = player.pitch;
        player.yaw = packet.getRotation().getY();
        player.pitch = packet.getRotation().getX();

        player.actualVelocity = new Vec3f(player.x - player.prevX, player.y - player.prevY, player.z - player.prevZ);
        player.claimedEOT = new Vec3f(packet.getDelta());
        player.prevEotVelocity = player.eotVelocity.clone();

        player.tick();
        if (player.lastTickWasTeleport) {
            player.eotVelocity = Vec3f.ZERO;
            player.updateBoundingBox(player.x, player.y, player.z);
            return;
        }

        new PlayerTicker(player).tick();
        final double offset = player.predictedVelocity.distanceTo(player.actualVelocity);

        final double maxOffset = player.getMaxOffset();
        if (player.actualVelocity.length() > 1e-5 || offset > maxOffset) {
            Bukkit.broadcastMessage((offset > maxOffset ? "§c" : "§a") + "O:" + offset + ", T: " + player.closetVector.getType() + ", P: " +
                    player.predictedVelocity.x + "," + player.predictedVelocity.y + "," + player.predictedVelocity.z + ", MO=" + maxOffset);

            Bukkit.broadcastMessage("§7A: " + player.actualVelocity.x + "," + player.actualVelocity.y + "," + player.actualVelocity.z + ", " +
                    "SPRINTING=" + player.sprinting + ", SNEAKING=" + player.sneaking + ", JUMPING=" + player.closetVector.isJumping() +
                    ", ENGINE=" + player.engine.getClass().getSimpleName());
        }
        if (offset > maxOffset && Boar.IS_IN_DEBUGGING) {
            player.updateBoundingBox(player.x, player.y, player.z);
        }

        correctInputData(player, packet);
    }

    // https://github.com/GeyserMC/Geyser/blob/master/core/src/main/java/org/geysermc/geyser/translator/protocol/bedrock/entity/player/input/BedrockMovePlayer.java#L90
    // Geyser check for our vertical collision for calculation for ground, do this to prevent possible no-fall bypass.
    private void correctInputData(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        if (player.wasFlying || player.flying) {
            return;
        }

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
    }

    private void processInputData(final BoarPlayer player) {
        player.wasFlying = player.flying;
        player.wasSprinting = player.sprinting;
        player.wasSneaking = player.sneaking;
        player.wasGliding = player.gliding;
        player.wasSwimming = player.swimming;

        for (final PlayerAuthInputData input : player.getInputData()) {
            switch (input) {
                // TODO: Prevent player from spoofing elytra gliding.
                case START_GLIDING -> player.gliding = true;
                case STOP_GLIDING -> player.gliding = false;

                // Don't let player do backwards sprinting!
                case START_SPRINTING -> player.sprinting = player.movementInput.getZ() > 0;
                case STOP_SPRINTING -> player.sprinting = false;

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

        final StringBuilder builder = new StringBuilder();
        player.getInputData().forEach(input -> builder.append(input).append(","));
        Bukkit.broadcastMessage(builder.toString());
    }
}
