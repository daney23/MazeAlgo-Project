package mazealgo.viewmodel;

/**
 * The eight directions a player can move in a 2D maze, mapped to the
 * numeric keypad layout so the controller can dispatch a NumPad key
 * straight to a direction without a switch:
 *
 * <pre>
 *      7  8  9
 *       \ | /
 *      4 -+- 6     (5 has no movement)
 *       / | \
 *      1  2  3
 * </pre>
 *
 * Diagonal moves carry the {@code diagonal} flag so the ViewModel can
 * apply the pinhole rule (a diagonal step is only legal if at least
 * one of the two adjacent orthogonal cells around the corner is also
 * passable — same rule used by {@code SearchableMaze}).
 */
public enum MovementDirection {
    UP(-1, 0, false),
    DOWN(1, 0, false),
    LEFT(0, -1, false),
    RIGHT(0, 1, false),
    UP_LEFT(-1, -1, true),
    UP_RIGHT(-1, 1, true),
    DOWN_LEFT(1, -1, true),
    DOWN_RIGHT(1, 1, true);

    public final int dr;
    public final int dc;
    public final boolean diagonal;

    MovementDirection(int dr, int dc, boolean diagonal) {
        this.dr = dr;
        this.dc = dc;
        this.diagonal = diagonal;
    }

    /** Maps a NumPad key (KeyCode.NUMPAD1..9) by its digit. Returns null for 5. */
    public static MovementDirection forNumpadDigit(int digit) {
        return switch (digit) {
            case 1 -> DOWN_LEFT;
            case 2 -> DOWN;
            case 3 -> DOWN_RIGHT;
            case 4 -> LEFT;
            case 6 -> RIGHT;
            case 7 -> UP_LEFT;
            case 8 -> UP;
            case 9 -> UP_RIGHT;
            default -> null;
        };
    }
}
