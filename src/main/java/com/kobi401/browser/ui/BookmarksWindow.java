package com.kobi401.browser.ui;

import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class BookmarksWindow {
    private final Stage bookmarksStage;
    private final BookmarksBar bookmarksBar;
    private final ListView<BookmarksBar.Bookmark> bookmarksListView;  // Change to ListView<Bookmark>

    public BookmarksWindow(BookmarksBar bookmarksBar) {
        this.bookmarksBar = bookmarksBar;
        this.bookmarksStage = new Stage();
        this.bookmarksListView = new ListView<>();

        bookmarksStage.setTitle("Bookmarks");
        bookmarksStage.setWidth(300);
        bookmarksStage.setHeight(400);

        updateBookmarksListView();

        bookmarksListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            openSelectedBookmark();
        });

        BorderPane layout = new BorderPane();
        layout.setCenter(bookmarksListView);

        Scene scene = new Scene(layout);
        bookmarksStage.setScene(scene);
    }

    private void updateBookmarksListView() {
        bookmarksListView.getItems().clear();
        bookmarksBar.getBookmarkTitles().forEach(title -> {
            String url = bookmarksBar.getBookmarkUrl(title);
            bookmarksListView.getItems().add(new BookmarksBar.Bookmark(title, url));
        });
    }

    private void openSelectedBookmark() {
        BookmarksBar.Bookmark selectedBookmark = bookmarksListView.getSelectionModel().getSelectedItem();
        if (selectedBookmark != null) {
            String url = selectedBookmark.getUrl();
            if (url != null) {
                //bookmarksBar.openBookmark(url);
            }
        }
    }

    public void show() {
        bookmarksStage.show();
    }
}
