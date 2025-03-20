package com.kobi401.browser.ui;

import com.kobi401.browser.memory.history.BrowserHistoryManager;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.ArrayList;

public class CustomTextField extends TextField {
    private ListView<String> suggestionList;
    private BrowserHistoryManager historyManager;
    private List<String> basicSuggestions;

    public CustomTextField() {
        historyManager = new BrowserHistoryManager();
        suggestionList = new ListView<>();
        suggestionList.setVisible(false);

        basicSuggestions = new ArrayList<>();
        basicSuggestions.add("https://www.google.com");
        basicSuggestions.add("https://www.mozilla.org/firefox/");
        basicSuggestions.add("https://www.apple.com/safari/");
        basicSuggestions.add("https://www.microsoft.com/edge");
        basicSuggestions.add("https://www.opera.com");

        suggestionList.setOnMouseClicked(event -> {
            String selectedUrl = suggestionList.getSelectionModel().getSelectedItem();
            if (selectedUrl != null) {
                setText(selectedUrl);
                suggestionList.setVisible(false);
            }
        });

        this.textProperty().addListener((observable, oldValue, newValue) -> {
            updateSuggestions(newValue);
        });
    }

    private void updateSuggestions(String query) {
        List<String> filteredHistory = filterHistory(query);
        List<String> allSuggestions = new ArrayList<>(basicSuggestions);

        if (!query.isEmpty()) {
            allSuggestions.addAll(filteredHistory);
        }

        suggestionList.getItems().clear();
        suggestionList.getItems().addAll(allSuggestions);

        suggestionList.setVisible(!allSuggestions.isEmpty());
    }

    private List<String> filterHistory(String query) {
        List<String> allHistory = historyManager.getHistory();
        return allHistory.stream()
                .filter(url -> url.toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    public VBox getSuggestionsContainer() {
        VBox container = new VBox();
        container.getChildren().addAll(this, suggestionList);
        return container;
    }
}
