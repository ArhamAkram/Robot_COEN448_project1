package com.robotmotion;

/**
 * Robot class representing the robot's state and movement logic.
 * The robot operates on an N×N floor grid.
 */
public class Robot {

    /** Directions the robot can face, in clockwise order. */
    public enum Direction {
        NORTH, EAST, SOUTH, WEST;


        public Direction turnRight() {
            return values()[(this.ordinal() + 1) % 4];
        }

        public Direction turnLeft() {
            return values()[(this.ordinal() + 3) % 4];
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    private int[][] floor;
    private int size;
    private int x;          // column index
    private int y;          // row index
    private boolean penDown;
    private Direction facing;

    /**
     * Constructs a Robot with a given floor size.
     * Robot starts at [0,0], pen up, facing north.
     *
     * @param size the N dimension of the N×N floor (must be > 0)
     */
    public Robot(int size) {
        initialize(size);
    }

    /**
     * Initializes (or re-initializes) the robot and floor.
     *
     * @param size the N dimension of the N×N floor (must be > 0)
     * @throws IllegalArgumentException if size <= 0
     */
    public void initialize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Floor size must be greater than zero.");
        }
        this.size = size;
        this.floor = new int[size][size];
        this.x = 0;
        this.y = 0;
        this.penDown = false;
        this.facing = Direction.NORTH;
    }

    /** Lifts the pen up. */
    public void penUp() {
        this.penDown = false;
    }

    /** Puts the pen down. */
    public void penDown() {
        this.penDown = true;
    }

    /** Turns the robot 90° to the right. */
    public void turnRight() {
        facing = facing.turnRight();
    }

    /** Turns the robot 90° to the left. */
    public void turnLeft() {
        facing = facing.turnLeft();
    }

    /**
     * Moves the robot forward by the given number of steps.
     * If the pen is down, marks each visited cell as 1.
     * Movement stops at the boundary of the floor.
     *
     * @param steps number of steps to move (must be >= 0)
     * @throws IllegalArgumentException if steps < 0
     */
    public void move(int steps) {
        if (steps < 0) {
            throw new IllegalArgumentException("Steps must be a non-negative integer.");
        }
        for (int i = 0; i < steps; i++) {
            if (penDown) {
                floor[y][x] = 1;
            }
            int newX = x;
            int newY = y;
            switch (facing) {
                case NORTH: newY = y + 1; break;
                case EAST:  newX = x + 1; break;
                case SOUTH: newY = y - 1; break;
                case WEST:  newX = x - 1; break;
            }
            // Clamp to boundaries
            if (newX < 0 || newX >= size || newY < 0 || newY >= size) {
                break; // stop at boundary
            }
            x = newX;
            y = newY;
            if (penDown) {
                floor[y][x] = 1;
            }
        }
    }

    /**
     * Returns a formatted string representation of the floor grid,
     * with row indices on the left and column indices at the bottom.
     *
     * @return the floor display string
     */
    public String printFloor() {
        StringBuilder sb = new StringBuilder();
        for (int row = size - 1; row >= 0; row--) {
            sb.append(String.format("%2d  ", row));
            for (int col = 0; col < size; col++) {
                sb.append(floor[row][col] == 1 ? "*" : " ");
                if (col < size - 1) sb.append(" ");
            }
            sb.append("\n");
        }
        // Column index line
        sb.append("    ");
        for (int col = 0; col < size; col++) {
            sb.append(String.format("%-2d", col));
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Returns the current status of the robot.
     *
     * @return position, pen state, and facing direction as a string
     */
    public String getStatus() {
        return String.format("Position: %d, %d - Pen: %s - Facing: %s",
                x, y, penDown ? "down" : "up", facing);
    }

    // --- Getters for testing ---

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isPenDown() { return penDown; }
    public Direction getFacing() { return facing; }
    public int getSize() { return size; }
    public int[][] getFloor() { return floor; }
    public int getFloorCell(int row, int col) { return floor[row][col]; }
}
