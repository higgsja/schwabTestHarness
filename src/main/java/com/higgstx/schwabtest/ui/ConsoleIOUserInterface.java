// src/main/java/com/higgstx/schwabtest/ui/ConsoleIOUserInterface.java
package com.higgstx.schwabtest.ui;

import org.springframework.stereotype.Component;

import java.io.Console;
import java.io.PrintWriter;
import java.util.Scanner;

@Component("consoleUserInterface") // Give it a specific name if you have multiple UIs
public class ConsoleIOUserInterface implements UserInterface {

    private final Console console = System.console();
    private final PrintWriter writer;
    private final Scanner scanner; // Fallback for when Console is null

    public ConsoleIOUserInterface() {
        if (console != null) {
            this.writer = console.writer();
            this.scanner = null; // No need for Scanner if Console is available
        } else {
            // Fallback to System.out and Scanner if not running in an interactive console
            this.writer = new PrintWriter(System.out);
            this.scanner = new Scanner(System.in);
        }
    }

    @Override
    public void displayMessage(String message) {
        writer.println(message);
        writer.flush(); // Ensure output is written immediately
    }

    @Override
    public void displayError(String message) {
        // Error messages can still go to System.err or be logged as errors
        if (console != null) {
            console.writer().println("ERROR: " + message);
            console.writer().flush();
        } else {
            System.err.println("ERROR: " + message);
        }
    }

    @Override
    public String getUserInput(String prompt) {
        if (console != null) {
            return console.readLine(prompt); // Console.readLine takes the prompt directly
        } else {
            // Fallback for non-interactive console
            writer.print(prompt);
            writer.flush();
            return scanner.nextLine().trim();
        }
    }

    @Override
    public void displaySeparator(int length) {
        displayMessage("=".repeat(length));
    }

    @Override
    public void displayHeader(String header) {
        displaySeparator(60);
        displayMessage("          " + header.toUpperCase()); // Centered roughly
        displaySeparator(60);
    }

    @Override
    public void displaySubHeader(String subHeader) {
        displayMessage("\n--- " + subHeader + " ---");
    }

    @Override
    public void exit(int status) {
        if (scanner != null) {
            scanner.close(); // Close fallback scanner if it was used
        }
        writer.close(); // Close the writer
        System.exit(status);
    }
}