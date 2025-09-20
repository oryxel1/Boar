package ac.boar.anticheat.util.math;

import org.geysermc.geyser.level.physics.Direction;

import static org.geysermc.geyser.level.physics.Direction.*;

public class DirectionUtil {
    public static Direction rotateYCounterclockwise(Direction direction) {
        return switch (direction) {
            case NORTH -> WEST;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
            case EAST -> NORTH;
            default -> throw new IllegalStateException("Unable to get CCW facing of " + direction);
        };
    }

    public static Direction getClockWise(Direction direction) {
        return switch (direction.ordinal()) {
            case 2 -> Direction.EAST;
            case 5 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            case 4 -> Direction.NORTH;
            default -> throw new IllegalStateException("Unable to get Y-rotated facing of " + direction);
        };
    }
}
