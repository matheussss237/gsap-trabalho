package br.com.projeto.view;

import br.com.projeto.view.components.FormField;
import br.com.projeto.view.components.LoadingButton;
import br.com.projeto.view.components.StatusBanner;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Tela de configurações: aparência, conta (troca de senha) e status da integração. */
public class SettingsView {

    private final BorderPane root = new BorderPane();
    private final ToggleGroup themeGroup = new ToggleGroup();
    private final RadioButton lightOption = new RadioButton("Tema claro");
    private final RadioButton darkOption = new RadioButton("Tema escuro");

    private final Label accountName = new Label();
    private final Label accountEmail = new Label();
    private final FormField currentPassword = FormField.password("Senha atual", "");
    private final FormField newPassword = FormField.password("Nova senha", "Mínimo de 8 caracteres, com letras e números");
    private final FormField confirmation = FormField.password("Confirmar nova senha", "");
    private final LoadingButton changePasswordButton = new LoadingButton("Alterar senha");
    private final StatusBanner passwordBanner = new StatusBanner();

    private final GridPane statusGrid = new GridPane();
    private final Button closeButton = new Button("Fechar");

    public SettingsView() {
        root.getStyleClass().add("settings-root");

        lightOption.setToggleGroup(themeGroup);
        darkOption.setToggleGroup(themeGroup);
        lightOption.getStyleClass().add("theme-option");
        darkOption.getStyleClass().add("theme-option");
        HBox themes = new HBox(12, lightOption, darkOption);

        accountName.getStyleClass().add("settings-strong");
        accountEmail.getStyleClass().add("muted");
        changePasswordButton.setMaxWidth(200);
        VBox account = new VBox(12, new VBox(2, accountName, accountEmail), passwordBanner,
                currentPassword, newPassword, confirmation, changePasswordButton);

        statusGrid.setHgap(18);
        statusGrid.setVgap(10);

        VBox content = new VBox(18,
                section("Aparência", "A preferência fica salva neste computador.", themes),
                section("Conta", "Altere a sua senha de acesso.", account),
                section("Sistema", "Configurações lidas das variáveis de ambiente.", statusGrid));
        content.setPadding(new Insets(24));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("transparent-scroll");

        Label title = new Label("Configurações");
        title.getStyleClass().add("dialog-title");
        HBox header = new HBox(title);
        header.getStyleClass().add("dialog-header");

        closeButton.getStyleClass().add("secondary-button");
        closeButton.setCancelButton(true);
        HBox footer = new HBox(closeButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("dialog-footer");

        root.setTop(header);
        root.setCenter(scroll);
        root.setBottom(footer);
    }

    private Node section(String title, String description, Node body) {
        Label t = new Label(title);
        t.getStyleClass().add("section-heading");
        Label d = new Label(description);
        d.getStyleClass().add("muted");
        VBox box = new VBox(12, new VBox(2, t, d), body);
        box.getStyleClass().add("settings-section");
        return box;
    }

    /** Adiciona uma linha "nome — valor" com indicador ok/erro. */
    public void addStatus(String name, String value, boolean ok) {
        int row = statusGrid.getRowCount();
        Label n = new Label(name);
        n.getStyleClass().add("settings-strong");
        Region dot = new Region();
        dot.getStyleClass().addAll("status-dot", ok ? "status-ok" : "status-warn");
        Label v = new Label(value);
        v.getStyleClass().add("muted");
        v.setWrapText(true);
        HBox valueBox = new HBox(8, dot, v);
        valueBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setHgrow(valueBox, Priority.ALWAYS);
        statusGrid.addRow(row, n, valueBox);
    }

    public Parent root() { return root; }
    public ToggleGroup themeGroup() { return themeGroup; }
    public RadioButton lightOption() { return lightOption; }
    public RadioButton darkOption() { return darkOption; }
    public Label accountName() { return accountName; }
    public Label accountEmail() { return accountEmail; }
    public FormField currentPassword() { return currentPassword; }
    public FormField newPassword() { return newPassword; }
    public FormField confirmation() { return confirmation; }
    public LoadingButton changePasswordButton() { return changePasswordButton; }
    public StatusBanner passwordBanner() { return passwordBanner; }
    public Button closeButton() { return closeButton; }
}
