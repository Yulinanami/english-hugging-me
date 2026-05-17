package me.englishhugging.desktop.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public final class DesktopUi {
    private DesktopUi() {}

    public static GridPane settingsGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        return grid;
    }

    public static Button compactButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(32);
        button.setPrefHeight(32);
        button.setStyle("-fx-background-color: #EEF2FF; -fx-text-fill: #52699A; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #D8E0F3; -fx-padding: 5 14 5 14;");
        return button;
    }

    public static TextField compactTextField() {
        TextField textField = new TextField();
        textField.setPrefWidth(330);
        textField.setPrefHeight(32);
        styleModernControl(textField);
        return textField;
    }

    public static VBox groupBox(String title, Node content) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #52699A; -fx-font-weight: bold; -fx-padding: 0 0 2 0;");
        VBox box = new VBox(8, label, content);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #E4E8F2; -fx-border-radius: 14; -fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 12, 0, 0, 3);");
        return box;
    }

    public static void styleModernControl(Node node) {
        node.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #D8E0F3; -fx-padding: 3 8 3 8;");
    }

    public static Tab settingsTab(String title, Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }
}
