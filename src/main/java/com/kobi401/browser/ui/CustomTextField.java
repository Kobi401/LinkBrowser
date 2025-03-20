package com.kobi401.browser.ui;

import com.kobi401.browser.memory.history.BrowserHistoryManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class CustomTextField extends TextField {
    private final ListView<String> suggestionList;
    private final BrowserHistoryManager historyManager;
    private final List<String> basicSuggestions;
    private final Popup suggestionPopup;

    public CustomTextField() {
        historyManager = new BrowserHistoryManager();
        suggestionList = new ListView<>();
        suggestionPopup = new Popup();

        // Predefined popular websites
        basicSuggestions = List.of(
                "https://www.google.com",
                "https://www.mozilla.org/firefox/",
                "https://www.apple.com/safari/",
                "https://www.microsoft.com/edge",
                "https://www.opera.com",
                "https://www.wikipedia.org",
                "https://www.youtube.com",
                "https://www.github.com",
                "https://www.stackoverflow.com"
        );

        suggestionPopup.getContent().add(suggestionList);
        suggestionPopup.setAutoHide(true); // Hide when clicking elsewhere

        // Clicking on a suggestion fills the text field
        suggestionList.setOnMouseClicked(event -> selectSuggestion());

        // Handle text changes to update suggestions
        this.textProperty().addListener((observable, oldValue, newValue) -> updateSuggestions(newValue));

        // Handle keyboard interactions
        this.setOnKeyPressed(event -> handleKeyPress(event));
    }

    /**
     * Updates suggestions dynamically based on user input.
     */
    private void updateSuggestions(String query) {
        List<String> filteredSuggestions = new ArrayList<>(basicSuggestions);

        if (!query.isEmpty()) {
            List<String> filteredHistory = filterHistory(query);
            filteredSuggestions.addAll(0, filteredHistory); // History first
        }

        // Show suggestions if available
        if (!filteredSuggestions.isEmpty()) {
            suggestionList.getItems().setAll(filteredSuggestions);
            showSuggestionPopup();
        } else {
            suggestionPopup.hide();
        }
    }

    /**
     * Filters browser history based on the query.
     */
    private List<String> filterHistory(String query) {
        return historyManager.getHistory().stream()
                .filter(url -> url.toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Displays the suggestion popup directly below the text field.
     */
    private void showSuggestionPopup() {
        Platform.runLater(() -> {
            if (!suggestionPopup.isShowing()) {
                double x = this.localToScreen(0, 0).getX();
                double y = this.localToScreen(0, this.getHeight()).getY();
                suggestionPopup.show(this, x, y);
            }
        });
    }

    /**
     * Handles keyboard navigation and selection of suggestions.
     */
    private void handleKeyPress(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN) {
            suggestionList.requestFocus();
            suggestionList.getSelectionModel().selectFirst();
        } else if (event.getCode() == KeyCode.ENTER) {
            selectSuggestion();
        }
    }

    /**
     * Selects the highlighted suggestion or first suggestion.
     */
    private void selectSuggestion() {
        String selectedUrl = suggestionList.getSelectionModel().getSelectedItem();
        if (selectedUrl != null) {
            this.setText(selectedUrl);
            this.positionCaret(selectedUrl.length());
            suggestionPopup.hide();
        }
    }

    /**
     * Provides a container with the search bar and suggestions.
     */
    public VBox getSearchContainer() {
        VBox container = new VBox();
        container.getChildren().add(this);
        return container;
    }
}
