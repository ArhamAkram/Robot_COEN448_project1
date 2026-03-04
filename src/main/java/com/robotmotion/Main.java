package com.robotmotion;

import java.util.Scanner;

/**
 * Main entry point for the Robot Motion Simulator.
 * Accepts command-line input from the user.
 */
public class  Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        CommandProcessor processor = new CommandProcessor();

        System.out.println("=== Robot Motion Simulator ===");
        System.out.println("Default: 10x10 floor, pen up, facing north at [0,0].");
        System.out.println("Commands: U, D, R, L, M <n>, P, C, Q, I <n>, H");
        System.out.println("==============================");

        while (processor.isRunning()) {
            System.out.print("> Enter command: ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine();
            String output = processor.process(input);
            if (!output.isEmpty()) {
                System.out.println(output);
            }
        }

        scanner.close();
        System.out.println("Goodbye!");
    }
}
