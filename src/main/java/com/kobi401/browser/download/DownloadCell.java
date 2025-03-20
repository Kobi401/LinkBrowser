package com.kobi401.browser.download;

import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Label;

public class DownloadCell extends ListCell<Download> {
    @Override
    protected void updateItem(Download item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            HBox hbox = new HBox();
            ProgressBar progressBar = new ProgressBar(item.getProgress());
            Label progressLabel = new Label(String.format("%.1f%%", item.getProgress() * 100));
            Label timeLabel = new Label(item.getEstimatedTime());

            hbox.getChildren().addAll(progressBar, progressLabel, timeLabel);
            setGraphic(hbox);
        }
    }
}


