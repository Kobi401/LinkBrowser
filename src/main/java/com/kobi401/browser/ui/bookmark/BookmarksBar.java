package com.kobi401.browser.ui.bookmark;

import com.kobi401.browser.ui.GUI.BrowserUI;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class BookmarksBar {
    private final HBox bookmarksContainer;
    private final Map<String, Bookmark> bookmarks; // Stores title → Bookmark
    private final WebEngine webEngine;
    private final BrowserUI browserUI;

    public BookmarksBar(BrowserUI browserUI, WebEngine webEngine) {
        this.bookmarksContainer = new HBox(5);
        this.browserUI = browserUI;
        this.bookmarks = new LinkedHashMap<>();
        this.webEngine = webEngine;
    }

    public void addBookmark(String title, String url) {
        if (!bookmarks.containsKey(title)) {
            Bookmark bookmark = new Bookmark(title, url);
            bookmarks.put(title, bookmark);
            Button bookmarkButton = createBookmarkButton(bookmark);
            bookmarksContainer.getChildren().add(bookmarkButton);
        }
    }

    private Button createBookmarkButton(Bookmark bookmark) {
        Button bookmarkButton = new Button(bookmark.getTitle());
        bookmarkButton.setGraphic(new ImageView(loadFavicon(bookmark.getUrl())));
        bookmarkButton.setOnAction(e -> openBookmark(bookmark));
        bookmarkButton.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 5; -fx-border-radius: 5;");
        return bookmarkButton;
    }

    public void openBookmark(Bookmark bookmark) {
        Platform.runLater(() -> {
            browserUI.createNewTab(bookmark.getUrl());
        });
    }

    public void removeBookmark(String title) {
        bookmarks.remove(title);
        updateBookmarksContainer();
    }

    public List<String> getBookmarkTitles() {
        return new ArrayList<>(bookmarks.keySet());
    }

    public String getBookmarkUrl(String title) {
        Bookmark bookmark = bookmarks.get(title);
        return bookmark != null ? bookmark.getUrl() : null;
    }

    private void updateBookmarksContainer() {
        bookmarksContainer.getChildren().clear();
        bookmarks.forEach((title, bookmark) -> {
            Button bookmarkButton = createBookmarkButton(bookmark);
            bookmarksContainer.getChildren().add(bookmarkButton);
        });
    }

    private Image loadFavicon(String url) {
        Image favicon = faviconCache.get(url);
        if (favicon == null) {
            try {
                favicon = new Image(url + "/favicon.ico", 16, 16, true, true);
                faviconCache.put(url, favicon); // Cache the favicon
            } catch (Exception e) {
                favicon = new Image(getClass().getResourceAsStream("/Images/default_favicon.png"), 16, 16, true, true);
            }
        }
        return favicon;
    }

    public HBox getBookmarksContainer() {
        return bookmarksContainer;
    }

    public List<Bookmark> getBookmarks() {
        return new ArrayList<>(bookmarks.values());
    }

    private final Map<String, Image> faviconCache = new LinkedHashMap<>(); // Cache for favicons

    public static class Bookmark {
        private final String title;
        private final String url;

        public Bookmark(String title, String url) {
            this.title = title;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }
    }
}
