package fr.moteurechecs.gui;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import fr.moteurechecs.ia.Niveau;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Piece;

import java.io.InputStream;
import java.util.List;

public class MainFX extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("DEEP BLACK");
        try {
            Image icon = new Image(getClass().getResourceAsStream("/logo.png"));
            this.primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Logo introuvable : " + e.getMessage());
        }
        montrerMenuPrincipal();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TÂCHE 1 — Écran d'accueil
    // ═══════════════════════════════════════════════════════════════════════

    private void montrerMenuPrincipal() {
        int[]     modeSelectionne = {0};           // 0=HvIA 1=IAvH 2=HvH 3=IAvIA
        Niveau[]  niveauBlanc     = {Niveau.MOYEN};
        Niveau[]  niveauNoir      = {Niveau.MOYEN};

        // ── Barre du haut ───────────────────────────────────────────────────
        HBox topBar = new HBox();
        topBar.setPrefHeight(56);
        topBar.setMinHeight(56);
        topBar.setMaxHeight(56);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 24, 0, 24));
        topBar.setStyle(
            "-fx-background-color:#0f0f0f;" +
            "-fx-border-color:transparent transparent #1f1f1f transparent;" +
            "-fx-border-width:0 0 1 0;"
        );

        Label lblTopLeft = new Label("DEEP BLACK");
        lblTopLeft.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#f0f0f0;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Label lblTopRight = new Label("Java 17 \u00B7 Maven \u00B7 JavaFX");
        lblTopRight.setStyle("-fx-font-size:11px;-fx-text-fill:#555555;");

        topBar.getChildren().addAll(lblTopLeft, topSpacer, lblTopRight);

        // ── Barre du bas ────────────────────────────────────────────────────
        HBox bottomBar = new HBox();
        bottomBar.setPrefHeight(36);
        bottomBar.setMinHeight(36);
        bottomBar.setMaxHeight(36);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setStyle(
            "-fx-background-color:#080808;" +
            "-fx-border-color:#1a1a1a transparent transparent transparent;" +
            "-fx-border-width:1 0 0 0;"
        );
        Label lblFooter = new Label(
            "DEEP BLACK \u00B7 Intelligence Artificielle " +
            "\u00B7 Universit\u00E9 Paris Cit\u00E9 \u00B7 2025\u20132026"
        );
        lblFooter.setStyle("-fx-font-size:11px;-fx-text-fill:#333333;");
        bottomBar.getChildren().add(lblFooter);

        // ── Zone centrale ───────────────────────────────────────────────────
        StackPane stackCentre = new StackPane();
        stackCentre.setStyle("-fx-background-color:#0a0a0a;");

        Canvas canvas = new Canvas();
        stackCentre.widthProperty().addListener((obs, ov, nv) -> {
            canvas.setWidth(nv.doubleValue());
            dessinerGrille(canvas);
        });
        stackCentre.heightProperty().addListener((obs, ov, nv) -> {
            canvas.setHeight(nv.doubleValue());
            dessinerGrille(canvas);
        });
        StackPane.setAlignment(canvas, Pos.TOP_LEFT);

        // ── Contenu centré ──────────────────────────────────────────────────
        VBox contenu = new VBox(24);
        contenu.setAlignment(Pos.CENTER);
        contenu.setMaxWidth(560);

        // 1. Titres
        Label lblGrand = new Label("DEEP BLACK");
        lblGrand.setStyle("-fx-font-size:68px;-fx-font-weight:bold;-fx-text-fill:#f0f0f0;");
        Label lblSous = new Label("Moteur d\u2019\u00E9checs");
        lblSous.setStyle("-fx-font-size:18px;-fx-text-fill:#555555;");
        VBox vboxTitre = new VBox(4, lblGrand, lblSous);
        vboxTitre.setAlignment(Pos.CENTER);

        // 2. Séparateur 120 px
        Region sep = new Region();
        sep.setPrefWidth(120);
        sep.setPrefHeight(1);
        sep.setMaxWidth(120);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color:#1f1f1f;");

        // 3. Label mode
        Label lblChoix = new Label("CHOISISSEZ VOTRE MODE DE JEU");
        lblChoix.setStyle("-fx-font-size:12px;-fx-text-fill:#444444;");

        // Lignes de niveaux (créées avant les cartes pour le lambda)
        HBox rowBlancs = creerLigneNiveaux("Niveau Blancs :", niveauBlanc, Niveau.MOYEN);
        HBox rowNoirs  = creerLigneNiveaux("Niveau Noirs :",  niveauNoir,  Niveau.MOYEN);

        VBox niveauContainer = new VBox(8);
        niveauContainer.setAlignment(Pos.CENTER);
        niveauContainer.getChildren().add(rowNoirs); // défaut : HvIA

        // 4. Cartes TilePane 2×2
        String[][] cartesData = {
            {"Humain vs IA",     "Vous jouez les Blancs"},
            {"IA vs Humain",     "Vous jouez les Noirs"},
            {"Humain vs Humain", "2 joueurs locaux"},
            {"IA vs IA",         "Mode spectateur"},
        };
        VBox[] cartesArr = new VBox[4];

        TilePane tilePane = new TilePane();
        tilePane.setPrefColumns(2);
        tilePane.setHgap(10);
        tilePane.setVgap(10);
        tilePane.setAlignment(Pos.CENTER);

        Runnable actualiserNiveaux = () -> {
            niveauContainer.getChildren().clear();
            switch (modeSelectionne[0]) {
                case 0 -> {
                    niveauContainer.getChildren().add(rowNoirs);
                    niveauContainer.setVisible(true);
                    niveauContainer.setManaged(true);
                }
                case 1 -> {
                    niveauContainer.getChildren().add(rowBlancs);
                    niveauContainer.setVisible(true);
                    niveauContainer.setManaged(true);
                }
                case 2 -> {
                    niveauContainer.setVisible(false);
                    niveauContainer.setManaged(false);
                }
                default -> {
                    niveauContainer.getChildren().addAll(rowBlancs, rowNoirs);
                    niveauContainer.setVisible(true);
                    niveauContainer.setManaged(true);
                }
            }
        };

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            VBox carte = new VBox(6);
            carte.setPrefSize(160, 90);
            carte.setMaxSize(160, 90);
            carte.setAlignment(Pos.CENTER);
            carte.getStyleClass().add("mode-card");
            if (i == 0) carte.getStyleClass().add("selected");

            Label t = new Label(cartesData[i][0]);
            t.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#f0f0f0;");
            Label s = new Label(cartesData[i][1]);
            s.setStyle("-fx-font-size:11px;-fx-text-fill:#555555;");
            carte.getChildren().addAll(t, s);

            carte.setOnMouseClicked(e -> {
                for (VBox c : cartesArr) c.getStyleClass().remove("selected");
                carte.getStyleClass().add("selected");
                modeSelectionne[0] = idx;
                actualiserNiveaux.run();
            });

            cartesArr[i] = carte;
            tilePane.getChildren().add(carte);
        }

        // 6. Bouton Démarrer
        Button btnDemarrer = new Button("D\u00E9marrer la partie");
        btnDemarrer.setStyle(
            "-fx-background-color:#ffffff;-fx-text-fill:#000000;" +
            "-fx-font-size:14px;-fx-font-weight:bold;" +
            "-fx-background-radius:8px;-fx-border-radius:8px;" +
            "-fx-padding:12px 32px;-fx-cursor:hand;" +
            "-fx-border-color:transparent;"
        );
        btnDemarrer.setOnMouseEntered(e -> btnDemarrer.setOpacity(0.88));
        btnDemarrer.setOnMouseExited(e  -> btnDemarrer.setOpacity(1.0));
        btnDemarrer.setOnAction(e -> {
            boolean iaBlanc, iaNoir;
            switch (modeSelectionne[0]) {
                case 0  -> { iaBlanc = false; iaNoir = true;  }
                case 1  -> { iaBlanc = true;  iaNoir = false; }
                case 2  -> { iaBlanc = false; iaNoir = false; }
                default -> { iaBlanc = true;  iaNoir = true;  }
            }
            lancerPartie(iaBlanc, iaNoir, niveauBlanc[0], niveauNoir[0]);
        });

        contenu.getChildren().addAll(vboxTitre, sep, lblChoix, tilePane, niveauContainer, btnDemarrer);
        stackCentre.getChildren().addAll(canvas, contenu);

        // ── Root ────────────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(stackCentre);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 900, 620);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ex) {
            System.err.println("style.css introuvable.");
        }
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void dessinerGrille(Canvas canvas) {
        if (canvas.getWidth() == 0 || canvas.getHeight() == 0) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.rgb(255, 255, 255, 0.025));
        gc.setLineWidth(1.0);
        for (double x = 0; x <= canvas.getWidth(); x += 48)
            gc.strokeLine(x, 0, x, canvas.getHeight());
        for (double y = 0; y <= canvas.getHeight(); y += 48)
            gc.strokeLine(0, y, canvas.getWidth(), y);
    }

    private HBox creerLigneNiveaux(String labelTexte, Niveau[] selected, Niveau defaut) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER);

        if (labelTexte != null) {
            Label lbl = new Label(labelTexte);
            lbl.setStyle("-fx-font-size:10px;-fx-text-fill:#555555;-fx-min-width:100px;");
            box.getChildren().add(lbl);
        }

        Niveau[] vals = {Niveau.FACILE, Niveau.MOYEN, Niveau.DIFFICILE};
        String[] noms = {"FACILE", "MOYEN", "DIFFICILE"};
        Button[] btns = new Button[3];

        for (int i = 0; i < 3; i++) {
            final int fi = i;
            Button btn = new Button(noms[i]);
            btn.getStyleClass().add("level-btn");
            if (vals[i] == defaut) btn.getStyleClass().add("active");
            btn.setOnAction(e -> {
                for (Button b : btns) b.getStyleClass().remove("active");
                btn.getStyleClass().add("active");
                selected[0] = vals[fi];
            });
            btns[i] = btn;
            box.getChildren().add(btn);
        }
        return box;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TÂCHE 2 — Barre d'avantage verticale
    // ═══════════════════════════════════════════════════════════════════════

    private Pane creerBarreAvantage(ControleurPartieGUI controleur) {
        Pane pane = new Pane();
        pane.setPrefSize(18, 480);
        pane.setMinSize(18, 480);
        pane.setMaxSize(18, 480);

        Rectangle bg = new Rectangle(18, 480, Color.web("#1a1a1a"));
        bg.setArcWidth(8);
        bg.setArcHeight(8);

        Rectangle zoneNoire = new Rectangle(18, 240, Color.web("#111111"));
        Rectangle zoneBlanc = new Rectangle(18, 240, Color.web("#f0f0f0"));
        zoneBlanc.setLayoutY(240);

        Rectangle clip = new Rectangle(18, 480);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        Group innerGroup = new Group(zoneNoire, zoneBlanc);
        innerGroup.setClip(clip);

        Label lblScore = new Label();
        lblScore.setPrefWidth(18);
        lblScore.setAlignment(Pos.CENTER);
        lblScore.setMouseTransparent(true);
        lblScore.setVisible(false);

        pane.getChildren().addAll(bg, innerGroup, lblScore);

        Timeline[] tl = {null};

        Runnable update = () -> {
            int    avantage     = controleur.avantageMaterielProperty().get();
            double hauteurBlanc = Math.min(460, Math.max(20, (0.5 + avantage / 62.0) * 480));
            double hauteurNoir  = 480.0 - hauteurBlanc;

            if (tl[0] != null) tl[0].stop();
            tl[0] = new Timeline(new KeyFrame(Duration.millis(300),
                new KeyValue(zoneNoire.heightProperty(), hauteurNoir,  Interpolator.EASE_BOTH),
                new KeyValue(zoneBlanc.heightProperty(), hauteurBlanc, Interpolator.EASE_BOTH),
                new KeyValue(zoneBlanc.layoutYProperty(), hauteurNoir, Interpolator.EASE_BOTH)
            ));
            tl[0].play();

            if (avantage != 0) {
                lblScore.setText((avantage > 0 ? "+" : "") + avantage);
                lblScore.setVisible(true);
                lblScore.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:"
                    + (avantage > 0 ? "#000000" : "#ffffff") + ";");
                lblScore.setLayoutX(0);
                lblScore.setLayoutY(avantage > 0
                    ? hauteurNoir + hauteurBlanc / 2.0 - 8
                    : hauteurNoir / 2.0 - 8);
            } else {
                lblScore.setVisible(false);
            }
        };

        controleur.avantageMaterielProperty().addListener((obs, ov, nv) -> update.run());
        update.run();
        return pane;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TÂCHE 3 — Pièces capturées
    // ═══════════════════════════════════════════════════════════════════════

    private String nomFichierPiece(Piece p) {
        if (p == null) return null;
        return switch (p) {
            case PION_BLANC     -> "pion_blanc.png";
            case CAVALIER_BLANC -> "cavalier_blanc.png";
            case FOU_BLANC      -> "fou_blanc.png";
            case TOUR_BLANC     -> "tour_blanche.png";
            case DAME_BLANCHE   -> "reine_blanche.png";
            case PION_NOIR      -> "pion_noir.png";
            case CAVALIER_NOIR  -> "cavalier_noir.png";
            case FOU_NOIR       -> "fou_noir.png";
            case TOUR_NOIRE     -> "tour_noire.png";
            case DAME_NOIRE     -> "reine_noire.png";
            default             -> null;
        };
    }

    private void mettreAJourHBoxCaptures(HBox hbox, List<Piece> pieces) {
        hbox.getChildren().clear();
        for (Piece p : pieces) {
            String nom = nomFichierPiece(p);
            if (nom == null) continue;
            try (InputStream is = getClass().getResourceAsStream("/" + nom)) {
                if (is == null) continue;
                ImageView iv = new ImageView(new Image(is));
                iv.setFitWidth(18);
                iv.setFitHeight(18);
                iv.setPreserveRatio(true);
                hbox.getChildren().add(iv);
            } catch (Exception ex) {
                // image manquante : on ignore
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lance la partie
    // ═══════════════════════════════════════════════════════════════════════

    private void lancerPartie(boolean iaBlanc, boolean iaNoir, Niveau niveauBlanc, Niveau niveauNoir) {
        ControleurPartieGUI controleur = new ControleurPartieGUI(iaBlanc, iaNoir, niveauBlanc, niveauNoir);

        // Échiquier
        StackPane boardWrapper = new StackPane(controleur.getVue());
        boardWrapper.setPrefSize(480, 480);
        boardWrapper.setMaxSize(480, 480);
        boardWrapper.getStyleClass().add("board-container");

        // Barres captures
        HBox capturesParNoirs  = new HBox(2);  // blancs pris → au-dessus du plateau
        HBox capturesParBlancs = new HBox(2);  // noirs pris  → en-dessous
        capturesParNoirs.getStyleClass().add("capture-bar");
        capturesParBlancs.getStyleClass().add("capture-bar");

        controleur.getPiecesCaptureesBlanches().addListener((ListChangeListener<Piece>) c ->
            mettreAJourHBoxCaptures(capturesParNoirs,  controleur.getPiecesCaptureesBlanches()));
        controleur.getPiecesCaptureeNoires().addListener((ListChangeListener<Piece>) c ->
            mettreAJourHBoxCaptures(capturesParBlancs, controleur.getPiecesCaptureeNoires()));

        // VBox échiquier + captures
        VBox vboxEchiquier = new VBox(4, capturesParNoirs, boardWrapper, capturesParBlancs);
        vboxEchiquier.setAlignment(Pos.CENTER_LEFT);

        // Barre d'avantage
        Pane barreAvantage = creerBarreAvantage(controleur);

        // Panneau latéral
        VBox panneauInfos = creerPanneauInfos(controleur);

        HBox centralLayout = new HBox(12, vboxEchiquier, barreAvantage, panneauInfos);
        centralLayout.setAlignment(Pos.CENTER);

        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(20));
        borderPane.setCenter(centralLayout);

        // Barre d'outils
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER);
        toolbar.setPadding(new Insets(12, 0, 0, 0));

        boolean modeHvIA = (iaBlanc && !iaNoir) || (!iaBlanc && iaNoir);
        if (modeHvIA) {
            Button btnAnnuler = new Button("Annuler coup");
            btnAnnuler.setOnAction(e -> controleur.annulerCoup());
            toolbar.getChildren().add(btnAnnuler);
        }

        Button btnMenu = new Button("Menu Principal");
        btnMenu.getStyleClass().add("button-accent");
        btnMenu.setOnAction(e -> montrerMenuPrincipal());
        toolbar.getChildren().add(btnMenu);

        borderPane.setBottom(toolbar);

        Scene scene = new Scene(borderPane, 900, 660);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ex) {
            System.err.println("style.css introuvable.");
        }

        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        controleur.demarrer();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TÂCHE 4 — Panneau latéral restructuré
    // ═══════════════════════════════════════════════════════════════════════

    private VBox creerPanneauInfos(ControleurPartieGUI controleur) {
        VBox panneau = new VBox();
        panneau.setPrefWidth(280);
        panneau.setMinWidth(280);
        panneau.setPrefHeight(480);
        panneau.setStyle(
            "-fx-background-color:#0d0d0d;" +
            "-fx-border-color:#1a1a1a;" +
            "-fx-border-width:1px;" +
            "-fx-background-radius:8px;" +
            "-fx-border-radius:8px;" +
            "-fx-padding:16px;" +
            "-fx-font-family:-apple-system,\"Helvetica Neue\",sans-serif;"
        );

        // Section 1 — Ouverture
        Label lblOuvSec = new Label("OUVERTURE");
        lblOuvSec.getStyleClass().add("side-section-label");

        Label lblOuverture = new Label();
        lblOuverture.setStyle("-fx-font-size:12px;-fx-text-fill:#aaaaaa;");
        lblOuverture.textProperty().bind(controleur.nomOuvertureProperty());
        lblOuverture.setWrapText(true);
        lblOuverture.setMaxWidth(248);

        Region sep1 = mkSep();
        VBox.setMargin(sep1, new Insets(8, 0, 8, 0));

        // Section 2 — Historique SAN
        Label lblCoupsSec = new Label("COUPS");
        lblCoupsSec.getStyleClass().add("side-section-label");

        ListView<String> listeHistorique = new ListView<>(controleur.getHistoriqueAffichage());
        listeHistorique.setFocusTraversable(false);
        listeHistorique.setPrefHeight(185);
        listeHistorique.getStyleClass().addAll("history-list", "history-cell");
        VBox.setVgrow(listeHistorique, Priority.ALWAYS);

        controleur.getHistoriqueAffichage().addListener((ListChangeListener<String>) c -> {
            if (!controleur.getHistoriqueAffichage().isEmpty())
                listeHistorique.scrollTo(controleur.getHistoriqueAffichage().size() - 1);
        });

        Region sep2 = mkSep();
        VBox.setMargin(sep2, new Insets(8, 0, 8, 0));

        // Section 3 — Calculs IA
        Label lblCalcSec = new Label("CALCULS IA");
        lblCalcSec.getStyleClass().add("side-section-label");

        ListView<String> listeCalculIA = new ListView<>(controleur.getCalculAIAffichage());
        listeCalculIA.setFocusTraversable(false);
        listeCalculIA.setPrefHeight(145);
        listeCalculIA.getStyleClass().addAll("history-list", "history-cell");
        VBox.setVgrow(listeCalculIA, Priority.SOMETIMES);

        controleur.getCalculAIAffichage().addListener((ListChangeListener<String>) c -> {
            if (!controleur.getCalculAIAffichage().isEmpty())
                listeCalculIA.scrollTo(controleur.getCalculAIAffichage().size() - 1);
        });

        Region sep3 = mkSep();
        VBox.setMargin(sep3, new Insets(8, 0, 8, 0));

        // Section 4 — Trait + niveau
        Label lblTrait = new Label("\u25CF Blancs");
        lblTrait.setStyle("-fx-font-size:11px;-fx-text-fill:#f0f0f0;");

        controleur.traitProperty().addListener((obs, ov, nv) -> {
            if (nv == Couleur.BLANC) {
                lblTrait.setText("\u25CF Blancs");
                lblTrait.setStyle("-fx-font-size:11px;-fx-text-fill:#f0f0f0;");
            } else {
                lblTrait.setText("\u25CF Noirs");
                lblTrait.setStyle("-fx-font-size:11px;-fx-text-fill:#555555;");
            }
        });

        Label lblNiveau = new Label();
        if (controleur.estIABlanc() && controleur.estIANoir()) {
            lblNiveau.setText("B:" + controleur.getNiveauIABlanc() + " N:" + controleur.getNiveauIANoir());
        } else if (controleur.estIABlanc()) {
            lblNiveau.setText(controleur.getNiveauIABlanc().toString());
        } else if (controleur.estIANoir()) {
            lblNiveau.setText(controleur.getNiveauIANoir().toString());
        }
        lblNiveau.setStyle("-fx-font-size:11px;-fx-text-fill:#444444;");

        Region spacerBas = new Region();
        HBox.setHgrow(spacerBas, Priority.ALWAYS);
        HBox bas = new HBox(lblTrait, spacerBas, lblNiveau);
        bas.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(bas, new Insets(4, 0, 0, 0));

        panneau.getChildren().addAll(
            lblOuvSec, lblOuverture,
            sep1,
            lblCoupsSec, listeHistorique,
            sep2,
            lblCalcSec, listeCalculIA,
            sep3,
            bas
        );
        return panneau;
    }

    private Region mkSep() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxHeight(1);
        r.setStyle("-fx-background-color:#1a1a1a;");
        return r;
    }

    public static void main(String[] args) {
        launch(args);
    }
}