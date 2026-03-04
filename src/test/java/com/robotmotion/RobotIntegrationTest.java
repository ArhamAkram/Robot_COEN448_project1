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


