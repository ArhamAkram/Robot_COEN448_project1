package com.robotmotion;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for the Robot Motion Simulator.
 *
 * <p>Each test exercises the full system end-to-end: commands pass through
 * {@link CommandProcessor#process(String)} → {@link Robot} state changes →
 * floor array updates → {@link Robot#printFloor()} output, thereby covering
 * all three classes (Main-level logic aside) in realistic multi-step scenarios.
 *
 * <p>Test scenarios:
 * <ul>
 *   <li>IT1 – Specification example L-shape (pen down throughout)</li>
 *   <li>IT2 – <b>3-cycle shape: 2 cycles pen-down, 1 cycle pen-up</b> (core requirement)</li>
 *   <li>IT3 – Closed rectangle (pen down all 4 sides)</li>
 *   <li>IT4 – History replay reproduces identical floor state</li>
 *   <li>IT5 – Re-initialize clears floor; status reflects reset</li>
 *   <li>IT6 – Boundary clamping does not corrupt floor</li>
 *   <li>IT7 – Mixed-case commands produce same results as uppercase</li>
 * </ul>
 */
@DisplayName("Integration Tests – Full System End-to-End")
@TestMethodOrder(OrderAnnotation.class)
class RobotIntegrationTest {

    /**
     * Fresh CommandProcessor (and therefore a fresh Robot) for every test.
     * Each test independently initialises the floor size it needs.
     */
    private CommandProcessor cp;

    @BeforeEach
    void setUp() {
        cp = new CommandProcessor();
    }

    // =========================================================================
    // IT1 – Specification Example: L-shape
    //
    //  Pen DOWN for entire path.
    //  Commands: I 10 → D → M 4 (north) → R → M 3 (east)
    //
    //  Expected floor (10×10):
    //    row 4:  col 0,1,2,3  marked  (* * * *)
    //    rows 0-3: col 0      marked  (vertical bar)
    //
    //  Shape drawn (rows 0-4, cols 0-3 shown):
    //    4:  * * * *
    //    3:  *
    //    2:  *
    //    1:  *
    //    0:  *
    //
    //  Touches: Robot, CommandProcessor.cmdInitialize, cmdPenDown,
    //           cmdMove, cmdTurnRight, cmdStatus, cmdPrint
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("IT1 – Spec example: L-shape, pen down, north 4 then east 3")
    void testSpecificationExample_LShape() {

        cp.process("I 10");
        cp.process("D");
        cp.process("M 4");
        cp.process("R");
        cp.process("M 3");

        Robot r = cp.getRobot();

        // ── Final robot state ──────────────────────────────────────────────
        assertEquals(3, r.getX(),   "Robot should be at column 3");
        assertEquals(4, r.getY(),   "Robot should be at row 4");
        assertTrue(r.isPenDown(),   "Pen should still be down");
        assertEquals(Robot.Direction.EAST, r.getFacing(), "Should face east");

        // ── Vertical bar: col 0, rows 0-4 ─────────────────────────────────
        for (int row = 0; row <= 4; row++) {
            assertEquals(1, r.getFloorCell(row, 0),
                    "Expected mark at [row=" + row + ", col=0]");
        }

        // ── Horizontal bar: row 4, cols 0-3 ───────────────────────────────
        for (int col = 0; col <= 3; col++) {
            assertEquals(1, r.getFloorCell(4, col),
                    "Expected mark at [row=4, col=" + col + "]");
        }

        // ── Everything above row 4 should be blank ─────────────────────────
        for (int row = 5; row < 10; row++) {
            for (int col = 0; col < 10; col++) {
                assertEquals(0, r.getFloorCell(row, col),
                        "Unexpected mark at [row=" + row + ", col=" + col + "]");
            }
        }

        // ── Columns 1-3 of rows 0-3 should be blank ───────────────────────
        for (int row = 0; row <= 3; row++) {
            for (int col = 1; col < 10; col++) {
                assertEquals(0, r.getFloorCell(row, col),
                        "Unexpected mark at [row=" + row + ", col=" + col + "]");
            }
        }

        // ── printFloor output contains '*' and index labels ────────────────
        String floorOutput = cp.process("P");
        assertTrue(floorOutput.contains("*"),   "Floor output must contain '*'");
        assertTrue(floorOutput.contains("0"),   "Floor output must contain column index 0");
        assertTrue(floorOutput.contains("9"),   "Floor output must contain column index 9");

        // ── C command reports correct state ────────────────────────────────
        String status = cp.process("C");
        assertTrue(status.contains("3, 4"),    "Status must show position 3, 4");
        assertTrue(status.contains("down"),    "Status must show pen down");
        assertTrue(status.contains("east"),    "Status must show facing east");
    }

    // =========================================================================
    // IT2 – 3-Cycle Shape: 2 cycles pen-down, 1 cycle pen-up
    //
    //  This is the primary required scenario.
    //
    //  Cycle 1 – PEN DOWN  : face north, move 4 steps
    //            Marks col 0, rows 0-4
    //  Cycle 2 – PEN UP    : turn right (east), move 3 steps — NO marks
    //            Robot travels from [0,4] to [3,4] leaving no trace
    //  Cycle 3 – PEN DOWN  : turn right (south), move 4 steps
    //            Marks col 3, rows 4-0
    //
    //  Shape (rows 0-4, cols 0-3 shown) — two vertical bars with a gap:
    //    4:  *     *          (col0 and col3 only)
    //    3:  *     *
    //    2:  *     *
    //    1:  *     *
    //    0:  *     *
    //
    //  Col 1 and col 2 of rows 0-4 are explicitly NOT marked (pen was up).
    //
    //  Covers all classes: CommandProcessor (process/cmdPenDown/cmdPenUp/
    //  cmdTurnRight/cmdMove/cmdPrint/cmdStatus), Robot (penDown/penUp/
    //  turnRight/move/printFloor/getStatus/getFloorCell)
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("IT2 – 3-cycle shape: cycle1 pen-down north 4, cycle2 pen-UP east 3, cycle3 pen-down south 4")
    void testThreeCycleShape_TwoPenDownOnePenUp() {

        // ── Cycle 1: pen DOWN, move north 4 ───────────────────────────────
        cp.process("I 10");
        cp.process("D");           // pen down
        cp.process("M 4");         // north 4 → robot at [0,4]

        Robot r = cp.getRobot();
        assertEquals(0, r.getX()); assertEquals(4, r.getY());
        assertTrue(r.isPenDown());

        // ── Cycle 2: turn east, pen UP, move east 3 ───────────────────────
        cp.process("R");           // now facing east
        cp.process("U");           // pen UP  ← no marks this cycle
        cp.process("M 3");         // east 3 → robot at [3,4]

        assertEquals(3, r.getX()); assertEquals(4, r.getY());
        assertFalse(r.isPenDown(), "Pen must be up during cycle 2");
        assertEquals(Robot.Direction.EAST, r.getFacing());

        // Row 4, cols 1 and 2 must be blank (pen was up)
        assertEquals(0, r.getFloorCell(4, 1), "Col1 row4 must NOT be marked (pen was up)");
        assertEquals(0, r.getFloorCell(4, 2), "Col2 row4 must NOT be marked (pen was up)");

        // ── Cycle 3: turn south, pen DOWN, move south 4 ───────────────────
        cp.process("R");           // now facing south
        cp.process("D");           // pen DOWN again
        cp.process("M 4");         // south 4 → robot at [3,0]

        assertEquals(3, r.getX()); assertEquals(0, r.getY());
        assertTrue(r.isPenDown(), "Pen must be down during cycle 3");
        assertEquals(Robot.Direction.SOUTH, r.getFacing());

        // ── Verify exact floor state ───────────────────────────────────────
        // Cycle-1 marks: col 0, rows 0-4
        int[] cycle1MarkedRows = {0, 1, 2, 3, 4};
        for (int row : cycle1MarkedRows) {
            assertEquals(1, r.getFloorCell(row, 0),
                    "Cycle-1: expected mark at [row=" + row + ", col=0]");
        }

        // Cycle-2 gap: row 4, cols 1 and 2 must be blank
        assertEquals(0, r.getFloorCell(4, 1), "Cycle-2 gap: [row=4,col=1] must be blank");
        assertEquals(0, r.getFloorCell(4, 2), "Cycle-2 gap: [row=4,col=2] must be blank");

        // Cycle-3 marks: col 3, rows 0-4
        int[] cycle3MarkedRows = {0, 1, 2, 3, 4};
        for (int row : cycle3MarkedRows) {
            assertEquals(1, r.getFloorCell(row, 3),
                    "Cycle-3: expected mark at [row=" + row + ", col=3]");
        }

        // Columns 1 and 2 across all rows must be entirely blank
        for (int row = 0; row < 10; row++) {
            assertEquals(0, r.getFloorCell(row, 1),
                    "Col 1 must be entirely blank (pen was up when crossing it)");
            assertEquals(0, r.getFloorCell(row, 2),
                    "Col 2 must be entirely blank (pen was up when crossing it)");
        }

        // No marks beyond col 3
        for (int row = 0; row < 10; row++) {
            for (int col = 4; col < 10; col++) {
                assertEquals(0, r.getFloorCell(row, col),
                        "No marks expected beyond col 3 at [row=" + row + ",col=" + col + "]");
            }
        }

        // ── printFloor renders the two-bar shape correctly ─────────────────
        String floor = cp.process("P");
        assertTrue(floor.contains("*"), "Floor must contain '*' marks");

        // Each of rows 0-4 in printFloor must contain exactly 2 asterisks
        String[] floorLines = floor.split("\n");
        int markedRowCount = 0;
        for (String line : floorLines) {
            // Identify data rows by the leading row index (not the column-index footer)
            if (line.matches("^\\s*[0-9]+\\s+.*")) {
                long stars = line.chars().filter(c -> c == '*').count();
                if (stars > 0) {
                    assertEquals(2, stars,
                            "Each marked row must have exactly 2 asterisks; line: [" + line + "]");
                    markedRowCount++;
                }
            }
        }
        assertEquals(5, markedRowCount,
                "Exactly 5 rows (0-4) should contain asterisks");

        // ── C command reflects final state ─────────────────────────────────
        String status = cp.process("C");
        assertTrue(status.contains("3, 0"),  "Final position must be 3, 0");
        assertTrue(status.contains("down"),  "Pen must be down at end");
        assertTrue(status.contains("south"), "Must be facing south at end");
    }

    // =========================================================================
    // IT3 – Closed Rectangle
    //
    //  Pen DOWN for all 4 sides.
    //  Commands: I 10 → D → M 4 (north) → R → M 4 (east) →
    //                           R → M 4 (south) → R → M 4 (west)
    //
    //  Expected: a 5×5 rectangle outline on the floor.
    //    rows 0 and 4: cols 0-4 all marked
    //    rows 1-3: only col 0 and col 4 marked
    //    robot returns to [0,0]
    // =========================================================================
    @Test
    @Order(3)
    @DisplayName("IT3 – Closed rectangle: pen-down all 4 sides, robot returns to origin")
    void testClosedRectangle_PenDownAllSides() {

        cp.process("I 10");
        cp.process("D");
        cp.process("M 4");    // north  → [0,4]
        cp.process("R");
        cp.process("M 4");    // east   → [4,4]
        cp.process("R");
        cp.process("M 4");    // south  → [4,0]
        cp.process("R");
        cp.process("M 4");    // west   → [0,0]

        Robot r = cp.getRobot();

        // Robot back at origin
        assertEquals(0, r.getX(), "Robot must return to col 0");
        assertEquals(0, r.getY(), "Robot must return to row 0");
        assertEquals(Robot.Direction.WEST, r.getFacing());

        // Bottom row (row 0) cols 0-4 marked
        for (int col = 0; col <= 4; col++)
            assertEquals(1, r.getFloorCell(0, col),
                    "Bottom edge: [row=0,col=" + col + "] must be marked");

        // Top row (row 4) cols 0-4 marked
        for (int col = 0; col <= 4; col++)
            assertEquals(1, r.getFloorCell(4, col),
                    "Top edge: [row=4,col=" + col + "] must be marked");

        // Left column (col 0) rows 0-4 marked
        for (int row = 0; row <= 4; row++)
            assertEquals(1, r.getFloorCell(row, 0),
                    "Left edge: [row=" + row + ",col=0] must be marked");

        // Right column (col 4) rows 0-4 marked
        for (int row = 0; row <= 4; row++)
            assertEquals(1, r.getFloorCell(row, 4),
                    "Right edge: [row=" + row + ",col=4] must be marked");

        // Interior cells must be blank
        for (int row = 1; row <= 3; row++)
            for (int col = 1; col <= 3; col++)
                assertEquals(0, r.getFloorCell(row, col),
                        "Interior: [row=" + row + ",col=" + col + "] must be blank");

        // Anything outside the 5×5 block must be blank
        for (int row = 5; row < 10; row++)
            for (int col = 0; col < 10; col++)
                assertEquals(0, r.getFloorCell(row, col),
                        "Outside rectangle: [row=" + row + ",col=" + col + "] must be blank");

        // printFloor must contain exactly 16 asterisks (5+5+3+3 = 16, corners shared)
        String floor = cp.process("P");
        long totalStars = floor.chars().filter(c -> c == '*').count();
        assertEquals(16, totalStars,
                "Rectangle perimeter must have exactly 16 marked cells");
    }

    // =========================================================================
    // IT4 – History Replay Reproduces Identical Floor
    //
    //  Runs the IT2 command sequence, then issues H (replay).
    //  Verifies that after replay the floor is identical to after the
    //  original run — covering CommandProcessor.cmdHistory() and its
    //  interaction with Robot.initialize() and all movement methods.
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("IT4 – History replay produces floor state identical to original run")
    void testHistoryReplay_ReproducesIdenticalFloor() {

        // Run the IT2 sequence (two-bar shape)
        cp.process("I 10");
        cp.process("D");
        cp.process("M 4");
        cp.process("R");
        cp.process("U");
        cp.process("M 3");
        cp.process("R");
        cp.process("D");
        cp.process("M 4");

        Robot r = cp.getRobot();

        // Capture the floor state after original run
        int[][] originalFloor = new int[10][10];
        for (int row = 0; row < 10; row++)
            for (int col = 0; col < 10; col++)
                originalFloor[row][col] = r.getFloorCell(row, col);

        // Capture position and facing after original run
        int origX = r.getX(), origY = r.getY();
        Robot.Direction origFacing = r.getFacing();

        // Execute history replay
        String historyOutput = cp.process("H");
        assertTrue(historyOutput.contains("Replaying History"),
                "H command must output replay header");
        assertTrue(historyOutput.contains("End of History"),
                "H command must output end marker");

        // After replay, floor must be identical
        for (int row = 0; row < 10; row++)
            for (int col = 0; col < 10; col++)
                assertEquals(originalFloor[row][col], r.getFloorCell(row, col),
                        "Replay: floor mismatch at [row=" + row + ",col=" + col + "]");

        // Position and facing must also be restored
        assertEquals(origX,      r.getX(),      "Replay: x position must match");
        assertEquals(origY,      r.getY(),      "Replay: y position must match");
        assertEquals(origFacing, r.getFacing(), "Replay: facing must match");
    }

    // =========================================================================
    // IT5 – Re-Initialize Clears Floor; Status Reflects Reset
    //
    //  Draws on a 10×10 floor, then issues I 5, verifying the new 5×5 floor
    //  is blank, robot is at [0,0] pen-up facing north, and history is cleared.
    // =========================================================================


    // =========================================================================
    // IT6 – Boundary Clamping Does Not Corrupt Floor
    //
    //  Robot starts at [0,0] and is sent beyond every boundary.
    //  Verifies the robot stops correctly and no ArrayIndexOutOfBoundsException
    //  is thrown. Pen is down during boundary moves so we can verify clamped
    //  cells are marked and nothing outside is touched.
    // =========================================================================
    @Test
    @Order(6)
    @DisplayName("IT6 – Boundary clamping: robot stops at all 4 edges, no floor corruption")
    void testBoundaryClamping_NoFloorCorruption() {

        cp.process("I 5");
        Robot r = cp.getRobot();

        // Move beyond north boundary from [0,0]
        cp.process("D");
        assertDoesNotThrow(() -> cp.process("M 100"),
                "Moving beyond boundary must not throw");
        assertEquals(0,  r.getX(), "X must stay at 0 after north overflow");
        assertEquals(4,  r.getY(), "Y must clamp at 4 (N-1) after north overflow");

        // Turn right → east; move beyond east boundary
        cp.process("R");
        assertDoesNotThrow(() -> cp.process("M 100"));
        assertEquals(4,  r.getX(), "X must clamp at 4 after east overflow");
        assertEquals(4,  r.getY());

        // Turn right → south; move beyond south boundary
        cp.process("R");
        assertDoesNotThrow(() -> cp.process("M 100"));
        assertEquals(4,  r.getX());
        assertEquals(0,  r.getY(), "Y must clamp at 0 after south overflow");

        // Turn right → west; move beyond west boundary
        cp.process("R");
        assertDoesNotThrow(() -> cp.process("M 100"));
        assertEquals(0,  r.getX(), "X must clamp at 0 after west overflow");
        assertEquals(0,  r.getY());

        // No cell outside 5×5 should exist (floor is exactly 5×5)
        assertEquals(5, r.getSize(), "Floor must remain 5×5");

        // Every cell along the perimeter should be marked (pen was down)
        // Top row and bottom row
        for (int col = 0; col < 5; col++) {
            assertEquals(1, r.getFloorCell(4, col), "Top row col " + col + " must be marked");
            assertEquals(1, r.getFloorCell(0, col), "Bottom row col " + col + " must be marked");
        }
        // Left col and right col
        for (int row = 0; row < 5; row++) {
            assertEquals(1, r.getFloorCell(row, 0), "Left col row " + row + " must be marked");
            assertEquals(1, r.getFloorCell(row, 4), "Right col row " + row + " must be marked");
        }
    }

    // =========================================================================
    // IT7 – Mixed-Case Commands Produce Identical Result to Uppercase
    //
    //  Runs the IT2 two-bar scenario using entirely lowercase commands,
    //  then compares the resulting floor cell-by-cell against a reference
    //  run done with uppercase commands. All classes touched.
    // =========================================================================
    @Test
    @Order(7)
    @DisplayName("IT7 – Lowercase commands produce identical floor to uppercase commands")
    void testMixedCaseCommands_IdenticalResult() {

        // ── Reference run with UPPERCASE commands ──────────────────────────
        CommandProcessor cpUpper = new CommandProcessor();
        cpUpper.process("I 10");
        cpUpper.process("D");
        cpUpper.process("M 4");
        cpUpper.process("R");
        cpUpper.process("U");
        cpUpper.process("M 3");
        cpUpper.process("R");
        cpUpper.process("D");
        cpUpper.process("M 4");
        Robot rUpper = cpUpper.getRobot();

        // ── Test run with lowercase commands ──────────────────────────────
        CommandProcessor cpLower = new CommandProcessor();
        cpLower.process("i 10");
        cpLower.process("d");
        cpLower.process("m 4");
        cpLower.process("r");
        cpLower.process("u");
        cpLower.process("m 3");
        cpLower.process("r");
        cpLower.process("d");
        cpLower.process("m 4");
        Robot rLower = cpLower.getRobot();

        // ── Compare every cell ────────────────────────────────────────────
        for (int row = 0; row < 10; row++)
            for (int col = 0; col < 10; col++)
                assertEquals(rUpper.getFloorCell(row, col),
                        rLower.getFloorCell(row, col),
                        "Cell mismatch at [row=" + row + ",col=" + col + "]");

        // ── Position and state must also match ────────────────────────────
        assertEquals(rUpper.getX(),      rLower.getX(),      "X must match");
        assertEquals(rUpper.getY(),      rLower.getY(),      "Y must match");
        assertEquals(rUpper.getFacing(), rLower.getFacing(), "Facing must match");
        assertEquals(rUpper.isPenDown(), rLower.isPenDown(), "Pen state must match");

        // ── printFloor output must be character-for-character identical ───
        assertEquals(cpUpper.process("P"), cpLower.process("P"),
                "printFloor output must be identical regardless of command case");
    }
}

