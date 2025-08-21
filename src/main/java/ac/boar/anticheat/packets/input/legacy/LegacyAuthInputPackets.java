package ac.boar.anticheat.packets.input.legacy;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.impl.OffsetHandlerCheck;
import ac.boar.anticheat.collision.Collider;
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
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.item.Items;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.util.Iterator;
import java.util.Map;

public class LegacyAuthInputPackets {
    public static void updateUnvalidatedPosition(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        player.prevUnvalidatedPosition = player.unvalidatedPosition.clone();
        player.unvalidatedPosition = new Vec3(packet.getPosition().sub(0, player.getYOffset(), 0));
        player.unvalidatedTickEnd = new Vec3(packet.getDelta());
    }

    public static void doPostPrediction(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        player.postTick();
        player.getTeleportUtil().cachePosition(player.tick, player.position.add(0, player.getYOffset(), 0).toVector3f());

        final UncertainRunner uncertainRunner = new UncertainRunner(player);

        // Properly calculated offset by comparing position instead of poorly calculated velocity that get calculated using (pos - prevPos) to account for floating point errors.
        float offset = player.position.distanceTo(player.unvalidatedPosition);
        float extraOffset = uncertainRunner.extraOffset(offset);
        offset -= extraOffset;
        offset -= uncertainRunner.extraOffsetNonTickEnd(offset);

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
        correctInputData(player, packet);

        if (offset < player.getMaxOffset()) {
            player.setPos(player.unvalidatedPosition.clone(), false);
        }

        // This is broken.
//        final List<Box> collisions = player.compensatedWorld.collectColliders(new ArrayList<>(), player.boundingBox.contract(1.0E-3F));
//        if (!collisions.isEmpty()) {
//            Vec3 offsetVec = Collider.moveOutOfBlocks(player.boundingBox.clone(), collisions);
//
//            // Bedrock behaviour, push player out of blocks if they're get clipped in it....
//            // and uhhhh if PUSH_TOWARDS_CLOSEST_SPACE flag is not present I think the max offset is around
//            // 0.08 -> 0.081 instead of 0.01F?
//            if (offsetVec.length() <= 0.01F) {
//                float offsetX = Math.min(0.01F, Math.abs(offsetVec.x)) * MathUtil.sign(offsetVec.x);
//                float offsetY = Math.min(0.01F, Math.abs(offsetVec.y)) * MathUtil.sign(offsetVec.y);
//                float offsetZ = Math.min(0.01F, Math.abs(offsetVec.z)) * MathUtil.sign(offsetVec.z);
//
//                player.setPos(player.position.add(offsetX, offsetY, offsetZ), false);
//            }
//        }

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

        player.prevPosition = player.position;
    }

    public static void correctInputData(final BoarPlayer player, final PlayerAuthInputPacket packet) {
        if (player.isMovementExempted()) {
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

            // Player isn't moving forward but is sprinting and their flag sync, this shouldn't happen.
            if (player.input.z <= 0 && player.getFlagTracker().has(EntityFlag.SPRINTING) && player.desyncedFlag.get() == -1) {
                player.getFlagTracker().set(EntityFlag.SPRINTING, false);

                // Tell geyser that the player "want" to stop sprinting.
                packet.getInputData().add(PlayerAuthInputData.STOP_SPRINTING);
            }
        }
    }

    public static void processInputData(final BoarPlayer player) {
        if (!player.getFlagTracker().has(EntityFlag.USING_ITEM)) {
            player.sinceTridentUse = 0;
        }

        for (final PlayerAuthInputData input : player.getInputData()) {
            switch (input) {
                case START_GLIDING -> {
                    final ContainerCache cache = player.compensatedInventory.armorContainer;

                    // Prevent player from spoofing elytra gliding.
                    player.getFlagTracker().set(EntityFlag.GLIDING, player.compensatedInventory.translate(cache.get(1).getData()).getId() == Items.ELYTRA.javaId());
                }
                case STOP_GLIDING -> player.getFlagTracker().set(EntityFlag.GLIDING, false);

                // Don't let player do backwards sprinting!
                case START_SPRINTING -> player.setSprinting(player.input.getZ() > 0);
                case STOP_SPRINTING -> player.setSprinting(false);
                case START_SNEAKING -> player.getFlagTracker().set(EntityFlag.SNEAKING, true);
                case STOP_SNEAKING -> player.getFlagTracker().set(EntityFlag.SNEAKING, false);

                case START_SWIMMING -> player.getFlagTracker().set(EntityFlag.SWIMMING, true);
                case STOP_SWIMMING -> player.getFlagTracker().set(EntityFlag.SWIMMING, false);

                case START_FLYING -> player.getFlagTracker().setFlying(player.abilities.contains(Ability.MAY_FLY) || player.abilities.contains(Ability.FLYING));
                case STOP_FLYING -> player.getFlagTracker().setFlying(false);

                case STOP_SPIN_ATTACK -> {
                    if (player.dirtySpinStop) {
                        player.stopRiptide();
                        player.velocity = player.velocity.multiply(-0.2F);
                    }
                }

                case START_USING_ITEM -> {
                    final ItemData itemData = player.compensatedInventory.inventoryContainer.getHeldItemData();
                    ItemStack item = player.compensatedInventory.translate(itemData);

                    player.getFlagTracker().set(EntityFlag.USING_ITEM, true);
                    player.getItemUseTracker().use(itemData, item.getId());
                    player.getItemUseTracker().setDirtyUsing(false);
                }

                case START_CRAWLING -> player.getFlagTracker().set(EntityFlag.CRAWLING, true);
                case STOP_CRAWLING -> player.getFlagTracker().set(EntityFlag.CRAWLING, false);
            }
        }

        if (player.getItemUseTracker().isDirtyUsing()) {
            player.getItemUseTracker().setDirtyUsing(false);

            // Shit hack TODO: Properly check for when the item CAN BE USE
            player.getSession().releaseItem();
        }
        player.dirtySpinStop = false;
    }
}
