package com.kobi401.browser.ui.themes;

import com.kobi401.browser.ui.TextField.CustomTextField;
import com.kobi401.browser.ui.navigationbar.BrowserNavigationBar;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

import java.net.URL;

public class ThemeManager {
    private static boolean isDarkMode = true;

    private static final String LIGHT_THEME_CSS = "light-theme.css";
    private static final String DARK_THEME_CSS = "dark-theme.css";

    public static void applyJavaTheme(Scene scene, boolean darkMode) {
        isDarkMode = darkMode;
        String themeCSS = darkMode ? DARK_THEME_CSS : LIGHT_THEME_CSS;
        URL resourceURL = ThemeManager.class.getClassLoader().getResource(themeCSS);
        if (resourceURL != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(resourceURL.toExternalForm());
        } else {
            System.err.println("CSS file not found: " + themeCSS);
        }
    }

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    public static void applyColorTheme(Region component, String theme) {
        switch (theme.toLowerCase()) {
            case "light":
                setLightColors(component);
                break;
            case "dark":
                setDarkColors(component);
                break;
            default:
                setDefaultColors(component);
        }
    }

    private static void setLightColors(Region component) {
        component.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: black; " +
                        "-fx-border-color: #e0e0e0; "
        );
    }

    private static void setDarkColors(Region component) {
        component.setStyle(
                "-fx-background-color: #2f2f2f; " +
                        "-fx-text-fill: white; " +
                        "-fx-border-color: #444444; "
        );
    }

    private static void setDefaultColors(Region component) {
        component.setStyle(
                "-fx-background-color: #f0f0f0; " +
                        "-fx-text-fill: black; " +
                        "-fx-border-color: #ccc; "
        );
    }

    public static void toggleTheme(Scene scene, Region component) {
        boolean darkMode = isDarkMode();
        applyJavaTheme(scene, !darkMode);
        applyColorTheme(component, darkMode ? "light" : "dark");
    }

    public static void applyMenuBarTheme(MenuBar menuBar, String theme) {
        switch (theme.toLowerCase()) {
            case "light":
                menuBar.getStyleClass().add("light-menu-bar");
                break;
            case "dark":
                menuBar.getStyleClass().add("dark-menu-bar");
                break;
            default:
                menuBar.getStyleClass().add("default-menu-bar");
                break;
        }

        for (javafx.scene.control.Menu menu : menuBar.getMenus()) {
            for (MenuItem item : menu.getItems()) {
                applyMenuItemTheme(item, theme);
            }
        }
    }

    public static void applyMenuItemTheme(MenuItem menuItem, String theme) {
        switch (theme.toLowerCase()) {
            case "light":
                menuItem.getStyleClass().add("light-menu-item");
                break;
            case "dark":
                menuItem.getStyleClass().add("dark-menu-item");
                break;
            default:
                menuItem.getStyleClass().add("default-menu-item");
                break;
        }
    }

    public static void applyNavigationBarTheme(BrowserNavigationBar navigationBar, String theme) {
        for (Button button : navigationBar.getButtons()) {
            applyButtonTheme(button, theme);
        }
        applyTextFieldTheme(navigationBar.getUrlField(), theme);
        applyColorTheme(navigationBar.getNavigationBar(), theme);
    }

    private static void applyButtonTheme(Button button, String theme) {
        switch (theme.toLowerCase()) {
            case "light":
                button.getStyleClass().add("light-button");
                break;
            case "dark":
                button.getStyleClass().add("dark-button");
                break;
            default:
                button.getStyleClass().add("default-button");
                break;
        }
    }

    private static void applyTextFieldTheme(CustomTextField textField, String theme) {
        switch (theme.toLowerCase()) {
            case "light":
                textField.getStyleClass().add("light-text-field");
                break;
            case "dark":
                textField.getStyleClass().add("dark-text-field");
                break;
            default:
                textField.getStyleClass().add("default-text-field");
                break;
        }
    }
}
