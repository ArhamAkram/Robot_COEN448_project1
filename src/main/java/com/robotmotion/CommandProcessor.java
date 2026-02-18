package com.robotmotion;

import java.util.ArrayList;
import java.util.List;

/**
 * CommandProcessor parses and executes robot commands.
 * Maintains a history of valid commands for replay.
 */
public class CommandProcessor {

    private Robot robot;
    private List<String> history;
    private boolean running;

    public CommandProcessor() {
        this.history = new ArrayList<>();
        this.running = true;
        // Default initialization with size 10
        this.robot = new Robot(10);
    }

    /**
     * Processes a single command string.
     *
     * @param input raw command input from the user
     * @return the output string to display (may be empty)
     */
    public String process(String input) {
        if (input == null) return "Invalid command.";
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "Invalid command.";

        char cmd = Character.toUpperCase(trimmed.charAt(0));
        String result;

        switch (cmd) {
            case 'U':
                result = cmdPenUp();
                break;
            case 'D':
                result = cmdPenDown();
                break;
            case 'R':
                result = cmdTurnRight();
                break;
            case 'L':
                result = cmdTurnLeft();
                break;
            case 'M':
                result = cmdMove(trimmed);
                break;
            case 'P':
                result = cmdPrint();
                break;
            case 'C':
                result = cmdStatus();
                break;
            case 'Q':
                result = cmdQuit();
                break;
            case 'I':
                result = cmdInitialize(trimmed);
                break;
            case 'H':
                result = cmdHistory();
                break;
            default:
                return "Invalid command: " + trimmed;
        }

        // Record valid commands in history (except Q and H themselves)
        if (cmd != 'Q' && cmd != 'H' && !result.startsWith("Error")) {
            history.add(trimmed);
        }

        return result;
    }

    private String cmdPenUp() {
        robot.penUp();
        return "Pen is now up.";
    }

    private String cmdPenDown() {
        robot.penDown();
        return "Pen is now down.";
    }

    private String cmdTurnRight() {
        robot.turnRight();
        return "Turned right. Now facing: " + robot.getFacing();
    }

    private String cmdTurnLeft() {
        robot.turnLeft();
        return "Turned left. Now facing: " + robot.getFacing();
    }

    private String cmdMove(String input) {
        try {
            // Format: M<optional space><integer>
            String rest = input.substring(1).trim();
            int steps = Integer.parseInt(rest);
            if (steps < 0) {
                return "Error: Steps must be a non-negative integer.";
            }
            robot.move(steps);
            return "Moved " + steps + " step(s). " + robot.getStatus();
        } catch (NumberFormatException e) {
            return "Error: Invalid move command. Usage: M <integer> (e.g., M 5)";
        }
    }

    private String cmdPrint() {
        return robot.printFloor();
    }

    private String cmdStatus() {
        return robot.getStatus();
    }

    private String cmdQuit() {
        running = false;
        return "Program stopped.";
    }

    private String cmdInitialize(String input) {
        try {
            String rest = input.substring(1).trim();
            int n = Integer.parseInt(rest);
            if (n <= 0) {
                return "Error: Floor size must be greater than zero.";
            }
            robot.initialize(n);
            history.clear();
            return "System initialized: " + n + "×" + n + " floor. Robot at [0,0], pen up, facing north.";
        } catch (NumberFormatException e) {
            return "Error: Invalid initialize command. Usage: I <integer> (e.g., I 10)";
        }
    }

    private String cmdHistory() {
        if (history.isEmpty()) {
            return "No history to replay.";
        }
        StringBuilder sb = new StringBuilder("--- Replaying History ---\n");
        // Save current state by re-initializing with current size, then replay
        int size = robot.getSize();
        robot.initialize(size);
        for (String cmd : history) {
            sb.append("> ").append(cmd).append("\n");
            char c = Character.toUpperCase(cmd.charAt(0));
            // Re-execute but skip adding to history again
            switch (c) {
                case 'U': robot.penUp(); break;
                case 'D': robot.penDown(); break;
                case 'R': robot.turnRight(); break;
                case 'L': robot.turnLeft(); break;
                case 'M':
                    try {
                        int steps = Integer.parseInt(cmd.substring(1).trim());
                        robot.move(steps);
                    } catch (NumberFormatException ignored) {}
                    break;
                case 'P':
                    sb.append(robot.printFloor());
                    break;
                case 'C':
                    sb.append(robot.getStatus()).append("\n");
                    break;
                case 'I':
                    try {
                        int n = Integer.parseInt(cmd.substring(1).trim());
                        robot.initialize(n);
                    } catch (NumberFormatException ignored) {}
                    break;
            }
        }
        sb.append("--- End of History ---");
        return sb.toString();
    }

    public boolean isRunning() { return running; }
    public Robot getRobot() { return robot; }
    public List<String> getHistory() { return new ArrayList<>(history); }
}
