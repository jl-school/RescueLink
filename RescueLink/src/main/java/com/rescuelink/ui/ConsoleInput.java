package com.rescuelink.ui;

import java.io.Console;
import java.util.Scanner;

/**
 * Safe console input helpers: invalid input never crashes the program, it just asks again.
 * Wraps the single Scanner created in Main so no menu ever opens a second one on System.in.
 */
public final class ConsoleInput {

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.out.println();
            System.out.println("Input closed. Exiting RescueLink.");
            System.exit(0);
        }
        return scanner.nextLine();
    }

    /** Reads an integer between min and max (inclusive), repeating until valid. */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readLine(prompt).trim();
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("Please enter a number from " + min + " to " + max + ".");
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String text = readLine(prompt + " (y/n): ").trim().toLowerCase();
            if (text.equals("y") || text.equals("yes")) {
                return true;
            }
            if (text.equals("n") || text.equals("no")) {
                return false;
            }
            System.out.println("Please answer y or n.");
        }
    }

    /** Hides typing when a real console exists. IntelliJ's Run window has none, so input is visible there. */
    public String readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] chars = console.readPassword(prompt);
            return chars == null ? "" : new String(chars);
        }
        return readLine(prompt + "(visible in this window) ");
    }
}
