package ac.boar.anticheat.collision;

import ac.boar.anticheat.collision.util.CuboidBlockIterator;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.player.data.PlayerData;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.util.math.Vec3f;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.util.BlockUtils;

import java.util.ArrayList;
import java.util.List;

public class Collision {
    private static Vec3f adjustMovementForCollisions(final BoarPlayer player, Vec3f movement, boolean compensated) {
        Box box = player.boundingBox.clone();
        List<Box> collisions = /* this.getWorld().getEntityCollisions(this, lv.stretch(movement)) */ new ArrayList<>();
        Vec3f lv2 = movement.lengthSquared() == 0.0 ? movement : adjustMovementForCollisions(player, movement, box, collisions, compensated);
        boolean collisionX = movement.x != lv2.x, collisionZ = movement.z != lv2.z;
        boolean verticalCollision = movement.y != lv2.y;
        boolean onGround = verticalCollision && movement.y < 0.0;
        if ((onGround || player.onGround) && (collisionX || collisionZ)) {
            Vec3f vec32 = adjustMovementForCollisions(player, new Vec3f(movement.x, PlayerData.STEP_HEIGHT, movement.z), box, collisions, compensated);
            Vec3f vec33 = adjustMovementForCollisions(player, new Vec3f(0, PlayerData.STEP_HEIGHT, 0), box.stretch(movement.x, 0, movement.z), collisions, compensated);
            if (vec33.y < PlayerData.STEP_HEIGHT) {
                Vec3f vec34 = adjustMovementForCollisions(player, new Vec3f(movement.x, 0, movement.z), box.offset(vec33), collisions, compensated).add(vec33);
                if (vec34.horizontalLengthSquared() > vec32.horizontalLengthSquared()) {
                    vec32 = vec34;
                }
            }

            if (vec32.horizontalLengthSquared() > lv2.horizontalLengthSquared()) {
                lv2 = vec32.add(adjustMovementForCollisions(player, new Vec3f(0, -vec32.y, 0), box.offset(vec32), collisions, compensated));
            }
        }

        return lv2;
    }

    private static Vec3f adjustMovementForCollisions(final BoarPlayer player, final Vec3f movement, final Box box, final List<Box> collisions, boolean compensated) {
        collisions.addAll(findCollisionsForMovement(player, box.stretch(movement), compensated));
        return adjustMovementForCollisions(movement, box, collisions);
    }

    private static Vec3f adjustMovementForCollisions(final Vec3f movement, Box box, final List<Box> collisions) {
        if (collisions.isEmpty()) {
            return movement;
        } else {
            float x = movement.x;
            float y = movement.y;
            float z = movement.z;
            if (y != 0.0) {
                y = calculateMaxOffset(Axis.Y, box, collisions, y);
                if (y != 0.0) {
                    box = box.offset(0, y, 0);
                }
            }

            if (x != 0.0) {
                x = calculateMaxOffset(Axis.X, box, collisions, x);
                if (x != 0.0) {
                    box = box.offset(x, 0, 0);
                }
            }

            if (z != 0.0) {
                z = calculateMaxOffset(Axis.Z, box, collisions, z);
            }

            return new Vec3f(x, y, z);
        }
    }

    private static List<Box> findCollisionsForMovement(final BoarPlayer player, final Box box, boolean compensated) {
        final List<Box> collision = new ArrayList<>();
        final CuboidBlockIterator iterator = CuboidBlockIterator.iterator(box);
        final Mutable pos = new Mutable();
        while (iterator.step()) {
            int x = iterator.getX();
            int y = iterator.getY();
            int z = iterator.getZ();
            int l = iterator.getEdgeCoordinatesCount();
            if (l != 3 && player.compensatedWorld.isChunkLoaded(x, z)) {
                pos.set(x, y, z);
                addCollisionBoxesToList(player, pos, box, collision, compensated);
            }
        }

        return collision;
    }

    private static void addCollisionBoxesToList(final BoarPlayer player, final Mutable pos, final Box boundingBox, final List<Box> list, boolean compensated) {
        GeyserSession session = player.getSession();
        BlockState state;
        if (compensated) {
            state = player.compensatedWorld.getBlockState(pos.x, pos.y, pos.z);
        } else {
            state = session.getGeyser().getWorldManager().blockAt(session, pos.x, pos.y, pos.z);
        }

        final List<Box> boxes = BedrockCollision.getCollisionBox(player, pos, state);
        if (boxes != null) {
            for (Box box : boxes) {
                box = box.offset(pos.x, pos.y, pos.z);
                if (box.intersects(boundingBox)) {
                    list.add(box);
                }
            }
            return;
        }

        final BlockCollision collision = BlockUtils.getCollision(state.javaId());
        if (collision == null) {
            return;
        }

        for (final BoundingBox geyserBB : collision.getBoundingBoxes()) {
            final Box box = new Box(geyserBB).offset(pos.x, pos.y, pos.z);

            if (box.intersects(boundingBox)) {
                list.add(box);
            }
        }
    }

    private static float calculateMaxOffset(final Axis axis, final Box boundingBox, final List<Box> collision, float maxDist) {
        Box box = boundingBox.clone();

        for (Box bb : collision) {
            if (Math.abs(maxDist) < 1.0E-7) {
                return 0;
            }

            double oldDist = maxDist;
            maxDist = bb.calculateMaxDistance(axis, box, maxDist);

            // Normally minecraft (java) uses 1.0E-7 when it comes to calculating collision (calculateMaxDistance)
            // We however, uses 3.0E-5 since we have to account for floating point errors. This prevents collision being ignored when there is floating point errors.
            // But, sometimes this causes the anti-cheat to wrongly calculate your movement around 1.0E-5 -> 3.0E-5 offset (floating point error)
            // So we simply check for this and correct it back to 0. NOTE: This is only for cases that your movement is supposed to be 0.
            if (oldDist > 0 && maxDist >= -Box.MAX_TOLERANCE_ERROR && maxDist < -Box.EPSILON || oldDist < 0 && maxDist <= Box.MAX_TOLERANCE_ERROR && maxDist > Box.EPSILON) {
                maxDist = 0;
            }
        }

        return maxDist;
    }
}
