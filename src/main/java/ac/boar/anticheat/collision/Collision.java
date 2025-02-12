package ac.boar.anticheat.collision;

import ac.boar.anticheat.collision.util.CuboidBlockIterator;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.player.data.PlayerData;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.util.math.Vec3f;
import ac.boar.util.MathUtil;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.level.physics.Axis;
import org.geysermc.geyser.level.physics.BoundingBox;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.collision.BlockCollision;
import org.geysermc.geyser.util.BlockUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Collision {
    public static boolean isSpaceEmpty(final BoarPlayer player, final Box box) {
        return findCollisionsForMovement(player, box, true).isEmpty();
    }

    public static boolean isSpaceAroundPlayerEmpty(final BoarPlayer player, float offsetX, float offsetZ, float f) {
        Box lv = player.boundingBox;
        return isSpaceEmpty(player, new Box(lv.minX + offsetX, lv.minY - f - 1.0E-5F, lv.minZ + offsetZ, lv.maxX + offsetX, lv.minY, lv.maxZ + offsetZ));
    }

    private static boolean canBackOffFromEdge(final BoarPlayer player) {
        return player.onGround || player.fallDistance < 0.6F && !isSpaceAroundPlayerEmpty(player, 0, 0, 0.6F - player.fallDistance);
    }

    public static Vec3f adjustMovementForSneaking(final BoarPlayer player, final Vec3f movement) {
        final float f = PlayerData.STEP_HEIGHT;
        if (movement.y <= 0.0 && (player.sneaking || player.wasSneaking) && canBackOffFromEdge(player)) {
            float d = movement.x;
            float e = movement.z;
            float h = MathUtil.sign(d) * 0.05F;
            float i = MathUtil.sign(e) * 0.05F;

            while (d != 0 && isSpaceAroundPlayerEmpty(player, d, 0, f)) {
                if (Math.abs(d) <= 0.05) {
                    d = 0;
                    break;
                }

                d -= h;
            }

            while (e != 0.0 && isSpaceAroundPlayerEmpty(player, 0, e, f)) {
                if (Math.abs(e) <= 0.05) {
                    e = 0;
                    break;
                }

                e -= i;
            }

            while (d != 0.0 && e != 0.0 && isSpaceAroundPlayerEmpty(player, d, e, f)) {
                if (Math.abs(d) <= 0.05) {
                    d = 0;
                } else {
                    d -= h;
                }

                if (Math.abs(e) <= 0.05) {
                    e = 0;
                } else {
                    e -= i;
                }
            }

            return new Vec3f(d, movement.y, e);
        } else {
            return movement;
        }
    }

    public static Vec3f adjustMovementForCollisions(final BoarPlayer player, Vec3f movement, boolean compensated) {
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

    public static Optional<Vector3i> findSupportingBlockPos(final PlayerData player, Box box) {
        Vector3i lv = null;
        double d = Double.MAX_VALUE;
        final CuboidBlockIterator lv2 = CuboidBlockIterator.iterator(box);

        while (lv2.step()) {
            final Vector3i lv3 = Vector3i.from(lv2.getX(), lv2.getY(), lv2.getZ());
            double e = lv3.distanceSquared(player.x, player.y, player.z);
            if (e < d || e == d && (lv == null || lv.compareTo(lv3) < 0)) {
                lv = lv3.clone();
                d = e;
            }
        }

        return Optional.ofNullable(lv);
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
            if (Math.abs(maxDist) < Box.EPSILON) {
                return 0;
            }

            maxDist = bb.calculateMaxDistance(axis, box, maxDist);
        }

        return maxDist;
    }
}
