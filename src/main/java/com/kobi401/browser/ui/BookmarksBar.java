package com.kobi401.browser.ui;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.ArrayList;
import java.util.List;

public class BookmarksBar {
    private final HBox bookmarksContainer;
    private final List<Bookmark> bookmarks;
    private final List<WebEngine> tabEngines;

    public BookmarksBar() {
        bookmarksContainer = new HBox(5);
        bookmarks = new ArrayList<>();
        tabEngines = new ArrayList<>();
    }

    public void addBookmark(String title, String url, WebEngine webEngine, WebView webView) {
        Bookmark bookmark = new Bookmark(title, url);
        bookmarks.add(bookmark);
        Button bookmarkButton = new Button(title);
        bookmarkButton.setGraphic(new ImageView(loadFavicon(url)));
        bookmarkButton.setOnAction(e -> openInNewTab(url, webEngine, webView));
        bookmarkButton.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 5; -fx-border-radius: 5;");
        bookmarksContainer.getChildren().add(bookmarkButton);
    }

    private void openInNewTab(String url, WebEngine currentWebEngine, WebView webView) {
        WebView newTabWebView = new WebView();
        WebEngine newTabEngine = newTabWebView.getEngine();
        tabEngines.add(newTabEngine);
        newTabEngine.load(url);
        newTabEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
               //not done
            }
        });

        currentWebEngine.load(url);
    }

    private Image loadFavicon(String url) {
        try {
            String faviconUrl = url + "/favicon.ico";
            return new Image(faviconUrl, 16, 16, true, true);
        } catch (Exception e) {
            return new Image(getClass().getResourceAsStream("/Images/default_favicon.png"), 16, 16, true, true);
        }
    }

    public HBox getBookmarksContainer() {
        return bookmarksContainer;
    }

    private static class Bookmark {
        String title;
        String url;

        public Bookmark(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }
}
