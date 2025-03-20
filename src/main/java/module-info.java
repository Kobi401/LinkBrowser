module com.kobi401.browser {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires jdk.jsobject;
    requires java.logging;
    requires jdk.management;

    opens com.kobi401.browser to javafx.fxml;
    exports com.kobi401.browser;
    exports com.kobi401.browser.download;
    opens com.kobi401.browser.download to javafx.fxml;
    exports com.kobi401.browser.ui;
    opens com.kobi401.browser.ui to javafx.fxml;
    exports com.kobi401.browser.memory;
    opens com.kobi401.browser.memory to javafx.fxml;
    exports com.kobi401.browser.engine;
    opens com.kobi401.browser.engine to javafx.fxml;
}