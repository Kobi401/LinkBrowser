package com.kobi401.browser.ui.bookmark;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class BookmarksWindow {
    private final Stage bookmarksStage;
    private final BookmarksBar bookmarksBar;
    private final ListView<BookmarksBar.Bookmark> bookmarksListView;

    public BookmarksWindow(BookmarksBar bookmarksBar) {
        this.bookmarksBar = bookmarksBar;
        this.bookmarksStage = new Stage();
        this.bookmarksListView = new ListView<>();

        bookmarksStage.setTitle("Bookmarks");
        bookmarksStage.setWidth(350);
        bookmarksStage.setHeight(400);

        initializeListView();

        updateBookmarksListView();

        bookmarksListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            openSelectedBookmark();
        });

        BorderPane layout = new BorderPane();
        layout.setCenter(bookmarksListView);

        Scene scene = new Scene(layout);
        bookmarksStage.setScene(scene);
    }

    private void initializeListView() {
        bookmarksListView.setCellFactory(listView -> new BookmarkCell());
    }

    private void updateBookmarksListView() {
        ObservableList<BookmarksBar.Bookmark> bookmarkItems = FXCollections.observableArrayList(bookmarksBar.getBookmarks());
        bookmarksListView.setItems(bookmarkItems);

        if (bookmarkItems.isEmpty()) {
            showNoBookmarksAlert();
        }
    }

    private void openSelectedBookmark() {
        BookmarksBar.Bookmark selectedBookmark = bookmarksListView.getSelectionModel().getSelectedItem();
        if (selectedBookmark != null) {
            bookmarksBar.openBookmark(selectedBookmark);
        }
    }

    private void showNoBookmarksAlert() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("No Bookmarks");
        alert.setHeaderText(null);
        alert.setContentText("There are no bookmarks to display.");
        alert.showAndWait();
    }

    public void show() {
        bookmarksStage.show();
    }

    private static class BookmarkCell extends javafx.scene.control.ListCell<BookmarksBar.Bookmark> {
        @Override
        protected void updateItem(BookmarksBar.Bookmark item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.getTitle());
            }
        }
    }
}