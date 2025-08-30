// src/main/java/com/higgstx/schwabtest/ui/UserInterface.java
package com.higgstx.schwabtest.ui;

public interface UserInterface {
    void displayMessage(String message);
    void displayError(String message);
    String getUserInput(String prompt);
    void displaySeparator(int length);
    void displayHeader(String header);
    void displaySubHeader(String subHeader);
    void exit(int status);
}