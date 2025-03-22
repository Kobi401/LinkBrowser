package com.kobi401.browser.ui;

import com.kobi401.browser.devtools.DeveloperConsole;
import com.kobi401.browser.devtools.InspectElementTool;
import com.kobi401.browser.download.DownloadsManager;
import com.kobi401.browser.download.DownloadsWindow;
import com.kobi401.browser.engine.BrowserEngine;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;

public class BrowserMenuBar {
    private MenuBar menuBar;
    private DownloadsManager downloadsManager;

    public BrowserMenuBar(BrowserUI browserUI) {
        menuBar = new MenuBar();
        downloadsManager = new DownloadsManager();

        Menu fileMenu = new Menu("File");
        MenuItem newTab = new MenuItem("New Tab");
        MenuItem newWindow = new MenuItem("New Window");
        MenuItem exit = new MenuItem("Exit");

        newTab.setAccelerator(KeyCombination.keyCombination("Ctrl+T"));
        newWindow.setAccelerator(KeyCombination.keyCombination("Ctrl+N"));
        exit.setAccelerator(KeyCombination.keyCombination("Ctrl+Q"));

        newTab.setOnAction(e -> browserUI.createNewTab("https://www.google.com"));
        newWindow.setOnAction(e -> openNewWindow(browserUI));
        exit.setOnAction(e -> System.exit(0));

        fileMenu.getItems().addAll(newTab, newWindow, new SeparatorMenuItem(), exit);

        Menu navigationMenu = new Menu("Navigation");
        MenuItem back = new MenuItem("← Back");
        MenuItem forward = new MenuItem("→ Forward");
        MenuItem refresh = new MenuItem("⟳ Refresh");
        MenuItem goToURL = new MenuItem("Go to URL");

        back.setAccelerator(KeyCombination.keyCombination("Alt+Left"));
        forward.setAccelerator(KeyCombination.keyCombination("Alt+Right"));
        refresh.setAccelerator(KeyCombination.keyCombination("F5"));
        goToURL.setAccelerator(KeyCombination.keyCombination("Ctrl+L"));

        back.setOnAction(e -> browserUI.getCurrentWebView().getEngine().executeScript("history.back()"));
        forward.setOnAction(e -> browserUI.getCurrentWebView().getEngine().executeScript("history.forward()"));
        refresh.setOnAction(e -> browserUI.getCurrentWebView().getEngine().reload());
        //goToURL.setOnAction(e -> browserUI.showGoToURLDialog());

        navigationMenu.getItems().addAll(back, forward, refresh, goToURL);

        Menu bookmarksMenu = new Menu("Bookmarks");
        MenuItem addBookmark = new MenuItem("Add Bookmark");
        MenuItem manageBookmarks = new MenuItem("Manage Bookmarks");

        addBookmark.setAccelerator(KeyCombination.keyCombination("Ctrl+D"));
        manageBookmarks.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+B"));

        addBookmark.setOnAction(e -> addBookmark(browserUI));
        manageBookmarks.setOnAction(e -> openBookmarkManager());

        bookmarksMenu.getItems().addAll(addBookmark, manageBookmarks);

        Menu historyMenu = new Menu("History");
        MenuItem viewHistory = new MenuItem("View History");
        MenuItem clearHistory = new MenuItem("Clear History");

        viewHistory.setAccelerator(KeyCombination.keyCombination("Ctrl+H"));
        clearHistory.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+H"));

        viewHistory.setOnAction(e -> viewBrowserHistory());
        clearHistory.setOnAction(e -> clearBrowserHistory());

        historyMenu.getItems().addAll(viewHistory, clearHistory);

        Menu downloadsMenu = new Menu("Downloads");
        MenuItem openDownloads = new MenuItem("Open Downloads");

        openDownloads.setAccelerator(KeyCombination.keyCombination("Ctrl+J"));
        openDownloads.setOnAction(e -> openDownloadsWindow());

        downloadsMenu.getItems().add(openDownloads);

        Menu developerMenu = new Menu("Developer");
        MenuItem inspectElement = new MenuItem("Inspect Element");
        MenuItem devConsole = new MenuItem("Developer Console");

        inspectElement.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+I"));
        devConsole.setAccelerator(KeyCombination.keyCombination("Ctrl+Shift+J"));

        inspectElement.setOnAction(e -> new InspectElementTool(browserUI).show());
        devConsole.setOnAction(e -> new DeveloperConsole(browserUI).show());

        developerMenu.getItems().addAll(inspectElement, devConsole);

        Menu settingsMenu = new Menu("Settings");
        MenuItem openSettings = new MenuItem("Open Settings");

        openSettings.setAccelerator(KeyCombination.keyCombination("Ctrl+Comma"));
        openSettings.setOnAction(e -> openSettingsPage());

        settingsMenu.getItems().add(openSettings);

        Menu helpMenu = new Menu("Help");
        MenuItem about = new MenuItem("About");

        about.setAccelerator(KeyCombination.keyCombination("F1"));
        about.setOnAction(e -> browserUI.showAboutWindow());

        helpMenu.getItems().add(about);

        menuBar.getMenus().addAll(fileMenu, navigationMenu, bookmarksMenu, historyMenu, downloadsMenu, developerMenu, settingsMenu, helpMenu);

        //ThemeManager.applyMenuBarTheme(menuBar, ThemeManager.isDarkMode() ? "dark" : "light");
    }

    public MenuBar getMenuBar() {
        return menuBar;
    }


    private void openNewWindow(BrowserUI browserUI) {
        showNotification("New Window", "This feature is not yet implemented.");
    }

    private void openDownloadsWindow() {
        DownloadsWindow downloadsWindow = new DownloadsWindow(downloadsManager);
        downloadsWindow.show();
    }

    private void addBookmark(BrowserUI browserUI) {
        String currentURL = browserUI.getCurrentWebView().getEngine().getLocation();
        showNotification("Bookmark Added", "Added: " + currentURL);
    }

    private void openBookmarkManager() {
        BookmarksWindow downloadsWindow = new BookmarksWindow(new BookmarksBar(BrowserEngine.getInstance().getWebEngine()));
        downloadsWindow.show();
    }

    private void viewBrowserHistory() {
        showNotification("View History", "Feature not implemented yet.");
    }

    private void clearBrowserHistory() {
        showNotification("Clear History", "Feature not implemented yet.");
    }

    private void openSettingsPage() {
        //browserUI.openSettingsPage(BrowserEngine.getInstance();
    }

    private void showNotification(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
