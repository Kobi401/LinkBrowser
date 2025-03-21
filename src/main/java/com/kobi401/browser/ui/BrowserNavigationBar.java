package com.kobi401.browser.ui;

import com.kobi401.browser.download.DownloadsManager;
import com.kobi401.browser.engine.BrowserEngine;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.Arrays;
import java.util.List;

public class BrowserNavigationBar {
    private HBox navigationBar;
    private CustomTextField urlField;
    private Button goButton, backButton, forwardButton, refreshButton, newTabButton, settingsButton, downloadsButton;
    private DownloadsManager downloadsManager;
    private BookmarksBar bookmarksBar;

    public BrowserNavigationBar(BrowserUI browserUI) {
        navigationBar = new HBox(5);

        urlField = new CustomTextField();
        urlField.setPromptText("Enter URL...");

        goButton = new Button("Go");
        backButton = new Button("←");
        forwardButton = new Button("→");
        refreshButton = new Button("⟳");
        newTabButton = new Button("+");
        settingsButton = new Button("⚙");
        downloadsButton = new Button("↓");

        downloadsManager = new DownloadsManager();

        goButton.setOnAction(e -> browserUI.getCurrentWebView().getEngine().load(urlField.getText()));
        backButton.setOnAction(e -> browserUI.getCurrentWebView().getEngine().executeScript("history.back()"));
        forwardButton.setOnAction(e -> browserUI.getCurrentWebView().getEngine().executeScript("history.forward()"));
        refreshButton.setOnAction(e -> browserUI.getCurrentWebView().getEngine().reload());
        newTabButton.setOnAction(e -> browserUI.createNewTab("https://www.google.com"));

        settingsButton.setOnAction(e -> browserUI.openSettingsPage(BrowserEngine.getInstance()));
        downloadsButton.setOnAction(e -> browserUI.showNotificationInBrowser("Error", "Downloads isn't supported yet!"));

        bookmarksBar = new BookmarksBar(BrowserEngine.getInstance().getWebEngine());
        bookmarksBar.addBookmark("Google", "https://www.google.com");
        bookmarksBar.addBookmark("YouTube", "https://www.youtube.com");
        bookmarksBar.addBookmark("GitHub", "https://github.com");

        navigationBar.getChildren().addAll(
                backButton, forwardButton, refreshButton, urlField, goButton, newTabButton,
                bookmarksBar.getBookmarksContainer(), settingsButton, downloadsButton
        );

        //ThemeManager.applyNavigationBarTheme(this, "dark");
    }

    public HBox getNavigationBar() {
        return navigationBar;
    }

    public List<Button> getButtons() {
        return Arrays.asList(backButton, forwardButton, refreshButton, goButton, newTabButton, settingsButton, downloadsButton);
    }

    public CustomTextField getUrlField() {
        return urlField;
    }
}

