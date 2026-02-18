package com.robotmotion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CommandProcessor.
 * Verifies command parsing, output messages, and history replay.
 */
@DisplayName("CommandProcessor Unit Tests")
class CommandProcessorTest {

    private CommandProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CommandProcessor();
        processor.process("I 10"); // start with known 10x10 state
    }

    // ─────────────────────────────────────────────────────────────
    // U – Pen Up
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-U1: 'U' command sets pen up")
    void testPenUpCommand() {
        processor.process("D");
        processor.process("U");
        assertFalse(processor.getRobot().isPenDown());
    }

    @Test
    @DisplayName("CP-U2: 'u' (lowercase) command sets pen up")
    void testPenUpLowercase() {
        processor.process("d");
        processor.process("u");
        assertFalse(processor.getRobot().isPenDown());
    }

    // ─────────────────────────────────────────────────────────────
    // D – Pen Down
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-D1: 'D' command sets pen down")
    void testPenDownCommand() {
        processor.process("D");
        assertTrue(processor.getRobot().isPenDown());
    }

    @Test
    @DisplayName("CP-D2: 'd' (lowercase) command sets pen down")
    void testPenDownLowercase() {
        processor.process("d");
        assertTrue(processor.getRobot().isPenDown());
    }

    // ─────────────────────────────────────────────────────────────
    // R – Turn Right
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-R1: 'R' command turns robot right")
    void testTurnRightCommand() {
        processor.process("R");
        assertEquals(Robot.Direction.EAST, processor.getRobot().getFacing());
    }

    @Test
    @DisplayName("CP-R2: 'r' (lowercase) turns right")
    void testTurnRightLowercase() {
        processor.process("r");
        assertEquals(Robot.Direction.EAST, processor.getRobot().getFacing());
    }

    // ─────────────────────────────────────────────────────────────
    // L – Turn Left
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-L1: 'L' command turns robot left")
    void testTurnLeftCommand() {
        processor.process("L");
        assertEquals(Robot.Direction.WEST, processor.getRobot().getFacing());
    }

    @Test
    @DisplayName("CP-L2: 'l' (lowercase) turns left")
    void testTurnLeftLowercase() {
        processor.process("l");
        assertEquals(Robot.Direction.WEST, processor.getRobot().getFacing());
    }

    // ─────────────────────────────────────────────────────────────
    // M – Move
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-M1: 'M 4' moves robot 4 steps north")
    void testMoveCommand() {
        processor.process("M 4");
        assertEquals(4, processor.getRobot().getY());
    }

    @Test
    @DisplayName("CP-M2: 'm 4' (lowercase) moves robot")
    void testMoveLowercase() {
        processor.process("m 4");
        assertEquals(4, processor.getRobot().getY());
    }

    @Test
    @DisplayName("CP-M3: 'M4' (no space) moves robot")
    void testMoveNoSpace() {
        processor.process("M4");
        assertEquals(4, processor.getRobot().getY());
    }

    @Test
    @DisplayName("CP-M4: Invalid move returns error message")
    void testMoveInvalidInput() {
        String result = processor.process("M abc");
        assertTrue(result.startsWith("Error"));
    }

    @Test
    @DisplayName("CP-M5: Move with zero steps stays at position")
    void testMoveZeroSteps() {
        processor.process("M 0");
        assertEquals(0, processor.getRobot().getX());
        assertEquals(0, processor.getRobot().getY());
    }

    // ─────────────────────────────────────────────────────────────
    // P – Print
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-P1: 'P' returns floor string")
    void testPrintCommand() {
        String result = processor.process("P");
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("CP-P2: Print after pen-down move shows asterisks")
    void testPrintAfterDraw() {
        processor.process("D");
        processor.process("M 3");
        String result = processor.process("P");
        assertTrue(result.contains("*"));
    }

    // ─────────────────────────────────────────────────────────────
    // C – Status
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-C1: 'C' returns position/pen/facing status")
    void testStatusCommand() {
        String result = processor.process("C");
        assertTrue(result.contains("Position"));
        assertTrue(result.contains("Pen"));
        assertTrue(result.contains("Facing"));
    }

    @Test
    @DisplayName("CP-C2: Status after D shows pen down")
    void testStatusAfterPenDown() {
        processor.process("D");
        String result = processor.process("C");
        assertTrue(result.contains("down"));
    }

    // ─────────────────────────────────────────────────────────────
    // Q – Quit
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-Q1: 'Q' stops the processor")
    void testQuitCommand() {
        processor.process("Q");
        assertFalse(processor.isRunning());
    }

    @Test
    @DisplayName("CP-Q2: 'q' (lowercase) stops the processor")
    void testQuitLowercase() {
        processor.process("q");
        assertFalse(processor.isRunning());
    }

    // ─────────────────────────────────────────────────────────────
    // I – Initialize
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-I1: 'I 5' initializes 5x5 floor")
    void testInitializeCommand() {
        processor.process("I 5");
        assertEquals(5, processor.getRobot().getSize());
        assertEquals(0, processor.getRobot().getX());
        assertEquals(0, processor.getRobot().getY());
    }

    @Test
    @DisplayName("CP-I2: 'i 5' (lowercase) initializes system")
    void testInitializeLowercase() {
        processor.process("i 5");
        assertEquals(5, processor.getRobot().getSize());
    }

    @Test
    @DisplayName("CP-I3: Initialize clears history")
    void testInitializeClearsHistory() {
        processor.process("D");
        processor.process("M 3");
        processor.process("I 10");
        assertTrue(processor.getHistory().isEmpty());
    }

    @Test
    @DisplayName("CP-I4: Invalid initialize (zero) returns error")
    void testInitializeZeroSize() {
        String result = processor.process("I 0");
        assertTrue(result.startsWith("Error"));
    }

    @Test
    @DisplayName("CP-I5: Invalid initialize (text) returns error")
    void testInitializeInvalidInput() {
        String result = processor.process("I abc");
        assertTrue(result.startsWith("Error"));
    }

    // ─────────────────────────────────────────────────────────────
    // H – History Replay
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-H1: 'H' replays history and returns replay string")
    void testHistoryReplay() {
        processor.process("D");
        processor.process("M 4");
        String result = processor.process("H");
        assertTrue(result.contains("Replaying History"));
    }

    @Test
    @DisplayName("CP-H2: History replay with no commands returns message")
    void testHistoryEmpty() {
        processor.process("I 10"); // clears history
        String result = processor.process("H");
        assertTrue(result.contains("No history"));
    }

    @Test
    @DisplayName("CP-H3: After history replay, robot state reflects replayed commands")
    void testHistoryReplayState() {
        processor.process("D");
        processor.process("M 4");
        processor.process("H");
        // After replay, robot should be at y=4 facing north
        assertEquals(4, processor.getRobot().getY());
    }

    // ─────────────────────────────────────────────────────────────
    // Invalid / Edge
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-E1: Unknown command returns invalid message")
    void testUnknownCommand() {
        String result = processor.process("Z");
        assertTrue(result.toLowerCase().contains("invalid"));
    }

    @Test
    @DisplayName("CP-E2: Null input returns invalid message")
    void testNullInput() {
        String result = processor.process(null);
        assertTrue(result.toLowerCase().contains("invalid"));
    }

    @Test
    @DisplayName("CP-E3: Empty input returns invalid message")
    void testEmptyInput() {
        String result = processor.process("  ");
        assertTrue(result.toLowerCase().contains("invalid"));
    }

    @Test
    @DisplayName("CP-E4: Full scenario from specification example")
    void testFullScenario() {
        processor.process("I 10");
        processor.process("D");
        processor.process("M 4");
        processor.process("R");
        processor.process("M 3");

        Robot r = processor.getRobot();
        assertEquals(3, r.getX());
        assertEquals(4, r.getY());
        assertTrue(r.isPenDown());
        assertEquals(Robot.Direction.EAST, r.getFacing());

        String floor = processor.process("P");
        assertTrue(floor.contains("*"));
    }
}
