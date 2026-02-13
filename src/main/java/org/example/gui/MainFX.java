package org.example.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.AI.Niveau;

public class MainFX extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Jeu d'Échecs - MiniMax AlphaBeta");

        montrerMenuPrincipal();
    }

    private void montrerMenuPrincipal() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label lblTitre = new Label("Configuration de la partie");
        lblTitre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Mode de jeu
        Label lblMode = new Label("Mode de jeu :");
        RadioButton rbHvsH = new RadioButton("Humain vs Humain");
        RadioButton rbHvsIA = new RadioButton("Humain vs IA (Humain = Blancs)");
        RadioButton rbIAvsH = new RadioButton("IA vs Humain (IA = Blancs)");
        RadioButton rbIAvsIA = new RadioButton("IA vs IA"); // Spectateur

        rbHvsIA.setSelected(true); // Défaut

        ToggleGroup groupMode = new ToggleGroup();
        rbHvsH.setToggleGroup(groupMode);
        rbHvsIA.setToggleGroup(groupMode);
        rbIAvsH.setToggleGroup(groupMode);
        rbIAvsIA.setToggleGroup(groupMode);

        // Niveau IA Blanc
        Label lblNiveauBlanc = new Label("Niveau IA (Blancs) :");
        ComboBox<Niveau> comboNiveauBlanc = new ComboBox<>();
        comboNiveauBlanc.getItems().addAll(Niveau.values());
        comboNiveauBlanc.setValue(Niveau.MOYEN);

        // Niveau IA Noir
        Label lblNiveauNoir = new Label("Niveau IA (Noirs) :");
        ComboBox<Niveau> comboNiveauNoir = new ComboBox<>();
        comboNiveauNoir.getItems().addAll(Niveau.values());
        comboNiveauNoir.setValue(Niveau.MOYEN);

        // Gestion de la visibilité/activation des niveaux selon le mode
        // Au démarrage
        updateVisibility(rbHvsIA, comboNiveauBlanc, comboNiveauNoir);

        // Listeners
        groupMode.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            updateVisibility((RadioButton) newVal, comboNiveauBlanc, comboNiveauNoir);
        });

        Button btnLancer = new Button("Démarrer");
        btnLancer.setStyle("-fx-font-size: 14px; -fx-padding: 10 20;");
        btnLancer.setOnAction(e -> {
            boolean iaBlanc = false;
            boolean iaNoir = false;

            if (rbHvsH.isSelected()) {
                iaBlanc = false;
                iaNoir = false;
            } else if (rbHvsIA.isSelected()) {
                iaBlanc = false;
                iaNoir = true;
            } else if (rbIAvsH.isSelected()) {
                iaBlanc = true;
                iaNoir = false;
            } else if (rbIAvsIA.isSelected()) {
                iaBlanc = true;
                iaNoir = true;
            }

            lancerPartie(iaBlanc, iaNoir, comboNiveauBlanc.getValue(), comboNiveauNoir.getValue());
        });

        root.getChildren().addAll(lblTitre, lblMode, rbHvsH, rbHvsIA, rbIAvsH, rbIAvsIA,
                lblNiveauBlanc, comboNiveauBlanc, lblNiveauNoir, comboNiveauNoir,
                btnLancer);

        Scene scene = new Scene(root, 400, 500);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void updateVisibility(RadioButton selected, ComboBox<Niveau> blanc, ComboBox<Niveau> noir) {
        String text = selected.getText();
        if (text.startsWith("Humain vs Humain")) {
            blanc.setDisable(true);
            noir.setDisable(true);
        } else if (text.startsWith("Humain vs IA")) {
            blanc.setDisable(true);
            noir.setDisable(false);
        } else if (text.startsWith("IA vs Humain")) {
            blanc.setDisable(false);
            noir.setDisable(true);
        } else { // IA vs IA
            blanc.setDisable(false);
            noir.setDisable(false);
        }
    }

    private void lancerPartie(boolean iaBlanc, boolean iaNoir, Niveau niveauBlanc, Niveau niveauNoir) {
        ControleurPartieGUI controleur = new ControleurPartieGUI(iaBlanc, iaNoir, niveauBlanc, niveauNoir);

        // Layout principal de la partie
        javafx.scene.layout.BorderPane borderPane = new javafx.scene.layout.BorderPane();

        // Centre : Echiquier
        borderPane.setCenter(controleur.getVue());

        // Bas : Barre d'outils (Undo, etc.)
        javafx.scene.layout.HBox toolbar = new javafx.scene.layout.HBox(10);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setPadding(new Insets(10));

        Button btnAnnuler = new Button("Annuler coup");
        btnAnnuler.setOnAction(e -> controleur.annulerCoup());

        // On n'affiche le bouton annuler que si au moins un humain joue ?
        // Ou même en IA vs IA pour revenir en arrière ?
        // Le user a dit : "fais en sorte que je puisse retourner en arriere quand il
        // s'agit d'affronter une ia seulement"
        // Donc HvIA ou IAvH.
        boolean modeHvIA = (iaBlanc && !iaNoir) || (!iaBlanc && iaNoir);
        if (modeHvIA) {
            toolbar.getChildren().add(btnAnnuler);
        } else {
            // on peut le laisser tout le temps pour debug ou spectateur,
            // mais respectons la demande stricte : "quand il s'agit d'affronter une ia
            // seulement"
            // Interprétation : Humain vs IA.
            // Mais bon, "IA vs IA" c'est affronter une IA vs une IA...
            // La demande est "affronter une ia", sous entendu "Moi (joueur) affronte une
            // IA".
        }

        // Ajoutons un bouton "Menu Principal" pour revenir
        Button btnMenu = new Button("Menu Principal");
        btnMenu.setOnAction(e -> montrerMenuPrincipal());
        toolbar.getChildren().add(btnMenu);

        borderPane.setBottom(toolbar);

        Scene scene = new Scene(borderPane, 600, 650);

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();

        // Démarrer la logique du contrôleur (ex: calcul premier coup si IA commence)
        controleur.demarrer();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
