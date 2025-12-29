package prisons.solar.npclib.core.ai.pathfinding;

/**
 * All 26 possible directions for 3D grid movement in pathfinding.
 * Includes 8 horizontal (N, S, E, W, NE, NW, SE, SW),
 * 8 horizontal-vertical combinations, 2 pure vertical (UP, DOWN),
 * and 8 diagonal-vertical directions.
 */
public enum Direction {
    // 4 Cardinal directions (horizontal) - indices 0-3
    NORTH(0, 0, -1, false, false, 0),
    SOUTH(0, 0, 1, false, false, 1),
    EAST(1, 0, 0, false, false, 2),
    WEST(-1, 0, 0, false, false, 3),

    // 4 Diagonal directions (horizontal) - indices 4-7
    NORTH_EAST(1, 0, -1, true, false, 4),
    NORTH_WEST(-1, 0, -1, true, false, 5),
    SOUTH_EAST(1, 0, 1, true, false, 6),
    SOUTH_WEST(-1, 0, 1, true, false, 7),

    // 2 Pure vertical - indices 8-9
    UP(0, 1, 0, false, true, 8),
    DOWN(0, -1, 0, false, true, 9),

    // 8 Cardinal + vertical - indices 10-17
    NORTH_UP(0, 1, -1, false, true, 10),
    NORTH_DOWN(0, -1, -1, false, true, 11),
    SOUTH_UP(0, 1, 1, false, true, 12),
    SOUTH_DOWN(0, -1, 1, false, true, 13),
    EAST_UP(1, 1, 0, false, true, 14),
    EAST_DOWN(1, -1, 0, false, true, 15),
    WEST_UP(-1, 1, 0, false, true, 16),
    WEST_DOWN(-1, -1, 0, false, true, 17),

    // 8 Diagonal + vertical - indices 18-25
    NORTH_EAST_UP(1, 1, -1, true, true, 18),
    NORTH_EAST_DOWN(1, -1, -1, true, true, 19),
    NORTH_WEST_UP(-1, 1, -1, true, true, 20),
    NORTH_WEST_DOWN(-1, -1, -1, true, true, 21),
    SOUTH_EAST_UP(1, 1, 1, true, true, 22),
    SOUTH_EAST_DOWN(1, -1, 1, true, true, 23),
    SOUTH_WEST_UP(-1, 1, 1, true, true, 24),
    SOUTH_WEST_DOWN(-1, -1, 1, true, true, 25);

    public final int dx;
    public final int dy;
    public final int dz;
    public final boolean isDiagonal;
    public final boolean hasVertical;
    public final int index;

    Direction(int dx, int dy, int dz, boolean isDiagonal, boolean hasVertical, int index) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.isDiagonal = isDiagonal;
        this.hasVertical = hasVertical;
        this.index = index;
    }

    // Useful subsets for different use cases
    public static final Direction[] CARDINAL = {NORTH, SOUTH, EAST, WEST};
    public static final Direction[] DIAGONAL_HORIZONTAL = {NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST};
    public static final Direction[] HORIZONTAL = {NORTH, SOUTH, EAST, WEST, NORTH_EAST, NORTH_WEST, SOUTH_EAST, SOUTH_WEST};
    public static final Direction[] ALL = values();
}
