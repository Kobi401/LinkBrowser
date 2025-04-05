package com.kobi401.browser.ui.navigationbar;

import com.kobi401.browser.download.downloadManager.DownloadsManager;
import com.kobi401.browser.engine.main.BrowserEngine;
import com.kobi401.browser.ui.GUI.BrowserUI;
import com.kobi401.browser.ui.TextField.CustomTextField;
import com.kobi401.browser.ui.settings.SettingsWindow;
import com.kobi401.browser.ui.bookmark.BookmarksBar;
import com.kobi401.browser.utils.fs.LinkFileManager;
import com.kobi401.browser.utils.tracking.BrowserTracker;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class BrowserNavigationBar {
    private HBox navigationBar;
    private CustomTextField urlField;
    private Button goButton, backButton, forwardButton, refreshButton, newTabButton, settingsButton, downloadsButton;
    private DownloadsManager downloadsManager;
    private BookmarksBar bookmarksBar;

    private static final String HOLIDAY_PROMPT_FORMAT = "Happy %s! %s";

    public BrowserNavigationBar(BrowserUI browserUI) {
        navigationBar = new HBox(10);
        navigationBar.setStyle("-fx-background-color: linear-gradient(to right, #e0e0e0, #f2f2f2); "
                + "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 3px 8px; -fx-pref-height: 35px;");

        List<Object> prompts = LinkFileManager.loadValuesFromFile("quotes_facts.link");
        List<String> textPrompts = prompts.stream()
                .filter(obj -> obj instanceof String)
                .map(obj -> (String) obj)
                .collect(Collectors.toList());

        String randomPrompt = textPrompts.isEmpty() ? "Enter URL..." : getRandomPrompt(textPrompts);

        String holidayPrompt = getHolidayPrompt();
        if (holidayPrompt != null) {
            randomPrompt = holidayPrompt;
        }

        urlField = new CustomTextField();
        urlField.setPromptText(randomPrompt);
        urlField.setStyle("-fx-background-color: white; -fx-border-radius: 8px; -fx-padding: 3px 8px;");

        adjustTextFieldWidth(randomPrompt);

        urlField.promptTextProperty().addListener((obs, oldVal, newVal) -> adjustTextFieldWidth(newVal));

        urlField.setOnMouseClicked(e -> updatePrompt(textPrompts));
        urlField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) updatePrompt(textPrompts);
        });

        goButton = createButton("Go");
        backButton = createButton("←");
        forwardButton = createButton("→");
        refreshButton = createButton("⟳");
        newTabButton = createButton("+");
        settingsButton = createButton("⚙");
        downloadsButton = createButton("↓");

        downloadsManager = new DownloadsManager();

        goButton.setOnAction(e -> browserUI.getCurrentWebView().getEngine().load(urlField.getText()));
        backButton.setOnAction(e -> browserUI.getCurrentWebView().getEngine().executeScript("history.back()"));
        forwardButton.setOnAction(e -> browserUI.getCurrentWebView().getEngine().executeScript("history.forward()"));
        refreshButton.setOnAction(e -> browserUI.getCurrentWebView().getEngine().reload());
        newTabButton.setOnAction(e -> browserUI.createNewTab("https://www.google.com"));
        settingsButton.setOnAction(e -> {
            BrowserTracker browserTracker = BrowserEngine.getTracker();
            SettingsWindow settingsWindow = new SettingsWindow(BrowserEngine.getInstance(), BrowserEngine.getWebView(), browserTracker);
            settingsWindow.open();
        });
        downloadsButton.setOnAction(e -> browserUI.showNotificationInBrowser("Error", "Use the Download button on the Menubar!"));

        bookmarksBar = new BookmarksBar(browserUI, BrowserEngine.getInstance().getWebEngine());
        bookmarksBar.addBookmark("Google", "https://www.google.com");
        bookmarksBar.addBookmark("YouTube", "https://www.youtube.com");
        bookmarksBar.addBookmark("GitHub", "https://github.com");

        navigationBar.getChildren().addAll(
                backButton, forwardButton, refreshButton, urlField, goButton, newTabButton,
                bookmarksBar.getBookmarksContainer(), settingsButton, downloadsButton
        );

        navigationBar.setStyle("-fx-font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; -fx-font-size: 14px;");
        urlField.setStyle("-fx-font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;");

        HBox.setHgrow(urlField, Priority.ALWAYS);
        urlField.setMaxWidth(Double.MAX_VALUE);

        navigationBar.setSpacing(10);
        navigationBar.setStyle("-fx-alignment: center-left;");
    }

    /**
     * Selects a random prompt from the given list.
     */
    private String getRandomPrompt(List<String> textPrompts) {
        return textPrompts.get(new Random().nextInt(textPrompts.size()));
    }

    /**
     * Returns a holiday prompt if today's date matches a holiday.
     * The prompt format is: "Happy <Holiday Name>! <Holiday Message>"
     */
    private String getHolidayPrompt() {
        LocalDate today = LocalDate.now();
        switch (today.getMonthValue()) {
            case 1: // New Year's Day
                if (today.getDayOfMonth() == 1) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "New Year's Day", "Here’s to new beginnings and endless possibilities.");
                }
                break;
            case 2: // Valentine's Day
                if (today.getDayOfMonth() == 14) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Valentine's Day", "Love is in the air.");
                }
                break;
            case 3: // St. Patrick's Day
                if (today.getDayOfMonth() == 17) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "St. Patrick's Day", "May the luck of the Irish be with you!");
                }
                break;
            case 4: // Easter (Variable Date)
                if (isEaster(today)) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Easter", "Wishing you a day filled with joy and renewal.");
                }
                break;
            case 5: // Memorial Day (Last Monday in May)
                if (today.getMonthValue() == 5 && today.getDayOfWeek().getValue() == 1 && today.getDayOfMonth() >= 25 && today.getDayOfMonth() <= 31) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Memorial Day", "Honoring those who have sacrificed for our freedom.");
                }
                break;
            case 6: // Juneteenth
                if (today.getMonthValue() == 6 && today.getDayOfMonth() == 19) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Juneteenth", "Celebrating freedom and equality for all.");
                }
                break;
            case 7: // Independence Day
                if (today.getDayOfMonth() == 4) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Independence Day", "Celebrate freedom with pride.");
                }
                break;
            case 9: // Labor Day (First Monday in September)
                if (today.getMonthValue() == 9 && today.getDayOfWeek().getValue() == 1 && today.getDayOfMonth() <= 7) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Labor Day", "Honoring the contributions of the American workforce.");
                }
                break;
            case 10: // Halloween
                if (today.getDayOfMonth() == 31) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Halloween", "May your night be spooky and fun.");
                }
                break;
            case 11: // Thanksgiving (Fourth Thursday in November)
                if (today.getMonthValue() == 11 && today.getDayOfWeek().getValue() == 4 && today.getDayOfMonth() >= 23 && today.getDayOfMonth() <= 29) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Thanksgiving", "Grateful for today and all that it brings.");
                }
                break;
            case 12: // Christmas
                if (today.getDayOfMonth() == 25) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Christmas", "May your heart be light and your troubles be bright.");
                }
                break;
            case 13: // Martin Luther King Jr. Day (Third Monday in January)
                if (today.getMonthValue() == 1 && today.getDayOfWeek().getValue() == 1 && today.getDayOfMonth() >= 15 && today.getDayOfMonth() <= 21) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Martin Luther King Jr. Day", "Honoring the legacy of Dr. King’s pursuit of justice and equality.");
                }
                break;
            case 14: // Presidents' Day (Third Monday in February)
                if (today.getMonthValue() == 2 && today.getDayOfWeek().getValue() == 1 && today.getDayOfMonth() >= 15 && today.getDayOfMonth() <= 21) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Presidents' Day", "Celebrating the leadership of our nation’s presidents.");
                }
                break;
            case 15: // Patriot Day (September 11)
                if (today.getMonthValue() == 9 && today.getDayOfMonth() == 11) {
                    return String.format(HOLIDAY_PROMPT_FORMAT, "Patriot Day", "Remembering and honoring those lost on 9/11.");
                }
                break;
            default:
                return null; // No holiday today
        }

        return null; // No holiday prompt today
    }

    /**
     * Check if the current date is Easter Sunday.
     */
    private boolean isEaster(LocalDate date) {
        //this is using the "Computus" algorithm the math might be slightly wrong so check the date of easter
        // and see if its correct.
        int year = date.getYear();
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;

        LocalDate easterSunday = LocalDate.of(year, month, day);
        return date.equals(easterSunday);
    }

    /**
     * Updates the URL field with a new random prompt.
     */
    private void updatePrompt(List<String> textPrompts) {
        if (!textPrompts.isEmpty()) {
            String newPrompt = getRandomPrompt(textPrompts);
            urlField.setPromptText(newPrompt);
        }
    }

    /**
     * Adjusts the text field width dynamically based on the prompt text length.
     */
    private void adjustTextFieldWidth(String promptText) {
        if (promptText == null || promptText.isEmpty()) return;

        Text text = new Text(promptText);
        text.setFont(urlField.getFont());

        double estimatedWidth = text.getLayoutBounds().getWidth() + 20;
        double minWidth = 250;
        double maxWidth = 600;

        urlField.setPrefWidth(Math.max(minWidth, Math.min(estimatedWidth, maxWidth)));
    }

    private Button createButton(String text) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 8px; -fx-border-color: #c8c8c8; "
                + "-fx-border-radius: 8px; -fx-padding: 5px 10px; -fx-font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; "
                + "-fx-font-size: 12px; -fx-max-width: 60px;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 8px; "
                + "-fx-border-color: #b0b0b0; -fx-border-radius: 8px; -fx-padding: 5px 10px; -fx-font-family: 'Helvetica Neue', "
                + "Helvetica, Arial, sans-serif; -fx-font-size: 12px;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 8px; "
                + "-fx-border-color: #c8c8c8; -fx-border-radius: 8px; -fx-padding: 5px 10px; -fx-font-family: 'Helvetica Neue', "
                + "Helvetica, Arial, sans-serif; -fx-font-size: 12px;"));

        return button;
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
