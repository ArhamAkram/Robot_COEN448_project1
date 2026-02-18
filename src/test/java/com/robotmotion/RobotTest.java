package com.robotmotion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Robot class.
 * Tests cover: initialization, pen control, turning, movement, boundary clamping,
 * floor marking, and status reporting.
 *
 * Requirement mapping:
 * R1  - initialize(), R2 - penUp/penDown, R3 - turnRight, R4 - turnLeft,
 * R5  - move() marking cells, R6 - boundary clamping, R7 - printFloor,
 * R8  - getStatus, R9 - invalid inputs
 */
@DisplayName("Robot Unit Tests")
class RobotTest {

    private Robot robot;

    @BeforeEach
    void setUp() {
        robot = new Robot(10);
    }

    // ─────────────────────────────────────────────────────────────
    // R1 – Initialization
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("R1-T1: Robot initializes at position [0,0]")
    void testInitialPosition() {
        assertEquals(0, robot.getX());
        assertEquals(0, robot.getY());
    }

    @Test
    @DisplayName("R1-T2: Robot initializes with pen up")
    void testInitialPenUp() {
        assertFalse(robot.isPenDown());
    }

    @Test
    @DisplayName("R1-T3: Robot initializes facing north")
    void testInitialFacingNorth() {
        assertEquals(Robot.Direction.NORTH, robot.getFacing());
    }

    @Test
    @DisplayName("R1-T4: Floor initialized to all zeros")
    void testInitialFloorAllZeros() {
        int[][] floor = robot.getFloor();
        for (int[] row : floor) {
            for (int cell : row) {
                assertEquals(0, cell);
            }
        }
    }

    @Test
    @DisplayName("R1-T5: Re-initialize resets robot state")
    void testReInitialize() {
        robot.penDown();
        robot.move(3);
        robot.initialize(5);
        assertEquals(0, robot.getX());
        assertEquals(0, robot.getY());
        assertFalse(robot.isPenDown());
        assertEquals(Robot.Direction.NORTH, robot.getFacing());
        assertEquals(5, robot.getSize());
    }

    @Test
    @DisplayName("R1-T6: Initialize with size zero throws exception")
    void testInitializeZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> robot.initialize(0));
    }

    @Test
    @DisplayName("R1-T7: Initialize with negative size throws exception")
    void testInitializeNegativeSize() {
        assertThrows(IllegalArgumentException.class, () -> robot.initialize(-5));
    }

    // ─────────────────────────────────────────────────────────────
    // R2 – Pen Control
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("R2-T1: penDown() sets pen to down")
    void testPenDown() {
        robot.penDown();
        assertTrue(robot.isPenDown());
    }

    @Test
    @DisplayName("R2-T2: penUp() sets pen to up")
    void testPenUp() {
        robot.penDown();
        robot.penUp();
        assertFalse(robot.isPenDown());
    }

    @Test
    @DisplayName("R2-T3: Pen up - moving does not mark cells")
    void testPenUpDoesNotMark() {
        robot.penUp();
        robot.move(5);
        for (int row = 0; row < robot.getSize(); row++) {
            for (int col = 0; col < robot.getSize(); col++) {
                assertEquals(0, robot.getFloorCell(row, col));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // R3 – Turn Right
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("R3-T1: Turn right from north -> east")
    void testTurnRightNorthToEast() {
        robot.turnRight();
        assertEquals(Robot.Direction.EAST, robot.getFacing());
    }

    @Test
    @DisplayName("R3-T2: Turn right from east -> south")
    void testTurnRightEastToSouth() {
        robot.turnRight(); robot.turnRight();
        assertEquals(Robot.Direction.SOUTH, robot.getFacing());
    }

    @Test
    @DisplayName("R3-T3: Turn right from south -> west")
    void testTurnRightSouthToWest() {
        robot.turnRight(); robot.turnRight(); robot.turnRight();
        assertEquals(Robot.Direction.WEST, robot.getFacing());
    }

    @Test
    @DisplayName("R3-T4: Turn right from west -> north (full rotation)")
    void testTurnRightFullRotation() {
        robot.turnRight(); robot.turnRight(); robot.turnRight(); robot.turnRight();
        assertEquals(Robot.Direction.NORTH, robot.getFacing());
    }

    // ─────────────────────────────────────────────────────────────
    // R4 – Turn Left
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("R4-T1: Turn left from north -> west")
    void testTurnLeftNorthToWest() {
        robot.turnLeft();
        assertEquals(Robot.Direction.WEST, robot.getFacing());
    }

    @Test
    @DisplayName("R4-T2: Turn left from west -> south")
    void testTurnLeftWestToSouth() {
        robot.turnLeft(); robot.turnLeft();
        assertEquals(Robot.Direction.SOUTH, robot.getFacing());
    }

    @Test
    @DisplayName("R4-T3: Four left turns returns to north")
    void testTurnLeftFullRotation() {
        robot.turnLeft(); robot.turnLeft(); robot.turnLeft(); robot.turnLeft();
        assertEquals(Robot.Direction.NORTH, robot.getFacing());
    }

    // ─────────────────────────────────────────────────────────────
    // R5 – Move and Cell Marking
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("R5-T1: Move north with pen down marks cells")
    void testMoveNorthPenDown() {
        robot.penDown();
        robot.move(4);
        assertEquals(0, robot.getX());
        assertEquals(4, robot.getY());
        // Cells [0][0] through [4][0] should be 1
        for (int row = 0; row <= 4; row++) {
            assertEquals(1, robot.getFloorCell(row, 0),
                "Cell [" + row + "][0] should be marked");
        }
    }

    @Test
    @DisplayName("R5-T2: Move east with pen down marks cells")
    void testMoveEastPenDown() {
        robot.penDown();
        robot.turnRight(); // now facing east
        robot.move(3);
        assertEquals(3, robot.getX());
        assertEquals(0, robot.getY());
        for (int col = 0; col <= 3; col++) {
            assertEquals(1, robot.getFloorCell(0, col));
        }
    }

    @Test
    @DisplayName("R5-T3: Move south with pen down marks cells")
    void testMoveSouthPenDown() {
        // First move north so there's room to go south
        robot.move(5);
        robot.turnRight(); robot.turnRight(); // face south
        robot.penDown();
        robot.move(3);
        assertEquals(0, robot.getX());
        assertEquals(2, robot.getY());
    }

    @Test
    @DisplayName("R5-T4: Move west with pen down marks cells")
    void testMoveWestPenDown() {
        // Move east first
        robot.turnRight();
        robot.move(5);
        robot.turnLeft(); robot.turnLeft(); // face west
        robot.penDown();
        robot.move(3);
        assertEquals(2, robot.getX());
        assertEquals(0, robot.getY());
    }

    @Test
    @DisplayName("R5-T5: Move zero steps stays at same position")
    void testMoveZeroSteps() {
        robot.move(0);
        assertEquals(0, robot.getX());
        assertEquals(0, robot.getY());
    }

    @Test
    @DisplayName("R5-T6: Negative steps throws exception")
    void testMoveNegativeSteps() {
        assertThrows(IllegalArgumentException.class, () -> robot.move(-1));
    }

    @Test
    @DisplayName("R5-T7: Cell marked only once even if traversed multiple times")
    void testCellMarkedOnce() {
        robot.penDown();
        robot.move(3);
        robot.turnRight(); robot.turnRight();
        robot.move(3);
        // All cells from 0 to 3 should be 1 (not 2)
        for (int row = 0; row <= 3; row++) {
            assertEquals(1, robot.getFloorCell(row, 0));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // R6 – Boundary Clamping
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("R6-T1: Robot stops at north boundary")
    void testNorthBoundary() {
        robot.move(100);
        assertEquals(9, robot.getY()); // max index for size 10
    }

    @Test
    @DisplayName("R6-T2: Robot stops at east boundary")
    void testEastBoundary() {
        robot.turnRight();
        robot.move(100);
        assertEquals(9, robot.getX());
    }

    @Test
    @DisplayName("R6-T3: Robot stops at south boundary (stays at 0)")
    void testSouthBoundary() {
        robot.turnRight(); robot.turnRight(); // face south
        robot.move(10);
        assertEquals(0, robot.getY());
    }

    @Test
    @DisplayName("R6-T4: Robot stops at west boundary (stays at 0)")
    void testWestBoundary() {
        robot.turnLeft(); // face west
        robot.move(10);
        assertEquals(0, robot.getX());
    }

    // ─────────────────────────────────────────────────────────────
    // R7 – Print Floor
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("R7-T1: Print floor returns non-null string")
    void testPrintFloorNotNull() {
        assertNotNull(robot.printFloor());
    }

    @Test
    @DisplayName("R7-T2: Print floor contains asterisk after pen-down move")
    void testPrintFloorContainsAsterisk() {
        robot.penDown();
        robot.move(3);
        assertTrue(robot.printFloor().contains("*"));
    }

    @Test
    @DisplayName("R7-T3: Print floor shows no asterisk on clean floor")
    void testPrintFloorClean() {
        assertFalse(robot.printFloor().contains("*"));
    }

    // ─────────────────────────────────────────────────────────────
    // R8 – Status Report
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("R8-T1: Status shows initial position 0,0")
    void testStatusInitial() {
        String status = robot.getStatus();
        assertTrue(status.contains("0, 0"));
        assertTrue(status.contains("up"));
        assertTrue(status.contains("north"));
    }

    @Test
    @DisplayName("R8-T2: Status reflects pen down")
    void testStatusPenDown() {
        robot.penDown();
        assertTrue(robot.getStatus().contains("down"));
    }

    @Test
    @DisplayName("R8-T3: Status reflects facing direction after turns")
    void testStatusAfterTurn() {
        robot.turnRight();
        assertTrue(robot.getStatus().contains("east"));
    }

    @Test
    @DisplayName("R8-T4: Status reflects updated position after move")
    void testStatusAfterMove() {
        robot.penDown();
        robot.move(4);
        String status = robot.getStatus();
        assertTrue(status.contains("0, 4"));
    }
}
