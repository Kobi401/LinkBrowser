package com.kobi401.browser.ui.TextField;

import com.kobi401.browser.memory.history.BrowserHistoryManager;
import com.kobi401.browser.utils.fs.LinkFileManager;
import javafx.application.Platform;
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
    private List<Object> suggestions;
    private final Popup suggestionPopup;

    public CustomTextField() {
        historyManager = new BrowserHistoryManager();
        suggestionList = new ListView<>();
        suggestionPopup = new Popup();
        suggestions = loadSuggestionsFromFile("suggestions.link");

        suggestionPopup.getContent().add(suggestionList);
        suggestionPopup.setAutoHide(true);
        suggestionList.setOnMouseClicked(event -> selectSuggestion());

        this.textProperty().addListener((observable, oldValue, newValue) -> updateSuggestions(newValue));

        this.setOnKeyPressed(event -> handleKeyPress(event));

        this.setStyle("-fx-background-color: #f5f5f7; "
                + "-fx-background-radius: 12px; "
                + "-fx-border-color: #c8c8c8; "
                + "-fx-border-radius: 12px; "
                + "-fx-border-width: 1px; "
                + "-fx-padding: 5px 10px; "
                + "-fx-font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; "
                + "-fx-font-size: 13px; "
                + "-fx-pref-height: 30px; "
                + "-fx-pref-width: 200px;");

        this.setOnMouseEntered(e -> this.setStyle("-fx-background-color: #e3e3e3; "
                + "-fx-background-radius: 12px; "
                + "-fx-border-color: #b0b0b0; "
                + "-fx-border-width: 1px; "
                + "-fx-font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; "
                + "-fx-font-size: 13px; "
                + "-fx-padding: 5px 10px;"));
        this.setOnMouseExited(e -> this.setStyle("-fx-background-color: #f5f5f7; "
                + "-fx-background-radius: 12px; "
                + "-fx-border-color: #c8c8c8; "
                + "-fx-border-width: 1px; "
                + "-fx-font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; "
                + "-fx-font-size: 13px; "
                + "-fx-padding: 5px 10px;"));
    }

    private List<Object> loadSuggestionsFromFile(String filePath) {
        return LinkFileManager.loadValuesFromFile(filePath);
    }

    /**
     * Updates suggestions dynamically based on user input.
     * Suggestions are sorted by how similar they are to the user input.
     */
    private void updateSuggestions(String query) {
        List<String> filteredSuggestions = new ArrayList<>();

        if (!query.isEmpty()) {
            filteredSuggestions = suggestions.stream()
                    .filter(obj -> obj instanceof String)
                    .map(obj -> (String) obj)
                    .filter(link -> link.toLowerCase().contains(query.toLowerCase()))
                    .sorted((link1, link2) -> {
                        int score1 = getSimilarityScore(query, link1);
                        int score2 = getSimilarityScore(query, link2);
                        return Integer.compare(score2, score1);
                    })
                    .collect(Collectors.toList());

            List<String> filteredHistory = filterHistory(query);
            filteredSuggestions.addAll(0, filteredHistory);
        }

        if (!filteredSuggestions.isEmpty()) {
            suggestionList.getItems().setAll(filteredSuggestions);
            adjustListViewSize(filteredSuggestions.size());
            showSuggestionPopup();
        } else {
            suggestionPopup.hide();
        }
    }

    /**
     * Adjusts the size of the suggestion list dynamically based on the number of items.
     * Ensures it doesn't exceed a maximum height.
     */
    private void adjustListViewSize(int suggestionCount) {
        int maxHeight = 200;
        int itemHeight = 30;
        double height = Math.min(suggestionCount * itemHeight, maxHeight);
        suggestionList.setPrefHeight(height);
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
     * Returns a similarity score for the query based on a simple prefix match and Levenshtein distance.
     * This is used to sort suggestions based on their relevance to the query.
     */
    private int getSimilarityScore(String query, String suggestion) {
        if (suggestion.toLowerCase().startsWith(query.toLowerCase())) {
            return 10; //lets give a high score for prefix matches
        }

        //i think this is you how you do that stupid LevelHipstein or whatever algorithm
        int levenshteinDistance = calculateLevenshteinDistance(query.toLowerCase(), suggestion.toLowerCase());
        return Math.max(0, 10 - levenshteinDistance);
    }

    /**
     * Levenshtein distance calculation to determine the difference between two strings.
     */
    public int calculateLevenshteinDistance(String a, String b) {
        if (a == null) {
            return (b == null) ? 0 : b.length();
        }
        if (b == null) {
            return a.length();
        }
        int lenA = a.length();
        int lenB = b.length();

        //maybe make sure we always have a smaller string as "a"?
        if (lenA > lenB) {
            String temp = a;
            a = b;
            b = temp;
            lenA = a.length();
            lenB = b.length();
        }

        //two arrays to reduce space complexity
        int[] previous = new int[lenA + 1];
        int[] current = new int[lenA + 1];

        //initialize the "previous" array
        for (int i = 0; i <= lenA; i++) {
            previous[i] = i;
        }

        for (int j = 1; j <= lenB; j++) {
            current[0] = j;

            //the distance for each character of the first string
            for (int i = 1; i <= lenA; i++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;

                //gotta take that minimum of insertion, deletion, or substitution
                current[i] = Math.min(Math.min(current[i - 1] + 1, previous[i] + 1), previous[i - 1] + cost);
            }

            //swap references, so previous always holds the previous row, and current holds the current one
            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[lenA];
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
