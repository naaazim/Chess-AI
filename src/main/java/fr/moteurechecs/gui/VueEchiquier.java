package fr.moteurechecs.gui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import fr.moteurechecs.plateau.Case;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

/**
 * Composant graphique représentant l'échiquier.
 */
public class VueEchiquier extends GridPane {

    private static final int TAILLE_CASE = 60;
    private static final Color COULEUR_CLAIR = Color.web("#eceed1");
    private static final Color COULEUR_FONCE = Color.web("#779556");
    private static final Color COULEUR_SELECTION = Color.web("#baca44", 0.7);
    private static final Color COULEUR_DERNIER_COUP = Color.web("#f5f682", 0.6);

    private final RessourcesPieces ressources;
    private final ControleurPartieGUI controleur;

    // Stockage des nodes pour mise à jour rapide (optionnel, ici on peut tout
    // redessiner pour simplifier)
    // Pour l'instant : on redessine tout à chaque rafraichissement (simple et
    // robuste pour < 100 éléments)

    public VueEchiquier(ControleurPartieGUI controleur) {
        this.controleur = controleur;
        this.ressources = new RessourcesPieces();
        this.setAlignment(Pos.CENTER);

        // Initialiser la grille vide
        construireGrilleVide();
    }

    private void construireGrilleVide() {
        this.getChildren().clear();
        for (int ligneVue = 0; ligneVue < 8; ligneVue++) {
            for (int colonneVue = 0; colonneVue < 8; colonneVue++) {
                // Fond de la case
                Rectangle rect = new Rectangle(TAILLE_CASE, TAILLE_CASE);
                boolean estClair = (ligneVue + colonneVue) % 2 == 0;
                rect.setFill(estClair ? COULEUR_CLAIR : COULEUR_FONCE);

                // Capturer les indices pour le listener (variables effectivement finales)
                final int ligneCapturee    = ligneVue;
                final int colonneCapturee  = colonneVue;

                // StackPane pour empiler : Fond + Surbrillance + Pièce
                StackPane stack = new StackPane(rect);
                stack.setOnMouseClicked(e -> {
                    // Conversion vue (0,0 en haut gauche) → modèle (ligne 1..8, 1 en bas)
                    int ligneDuModele   = 8 - ligneCapturee;
                    int colonneDuModele = colonneCapturee;
                    try {
                        Case caseCliquee = Case.depuisColonneLigne(colonneDuModele, ligneDuModele);
                        controleur.traiterClic(caseCliquee);
                    } catch (IllegalArgumentException ex) {
                        // Clic hors limite : ignoré
                    }
                });

                this.add(stack, colonneVue, ligneVue);
            }
        }
    }

    /**
     * Rafraîchit entièrement l'affichage de l'échiquier.
     *
     * @param plateau       position courante à afficher
     * @param selection     case sélectionnée par le joueur (peut être {@code null})
     * @param dernierCoup   dernier coup joué pour la surbrillance (peut être {@code null})
     * @param coupsPossibles liste des coups légaux depuis la case sélectionnée
     */
    public void rafraichir(Plateau plateau, Case selection, Coup dernierCoup, java.util.List<Coup> coupsPossibles) {
        this.getChildren().clear();

        for (int ligneVue = 0; ligneVue < 8; ligneVue++) {
            for (int colonneVue = 0; colonneVue < 8; colonneVue++) {
                // Conversion coordonnées vue → modèle (ligne 1=bas, 8=haut)
                int ligneDuModele   = 8 - ligneVue;
                int colonneDuModele = colonneVue;
                Case caseCourante = Case.depuisColonneLigne(colonneDuModele, ligneDuModele);
                Piece piece = plateau.pieceEn(caseCourante);

                // Fond de la case
                Rectangle rect = new Rectangle(TAILLE_CASE, TAILLE_CASE);
                boolean estClair = (ligneVue + colonneVue) % 2 == 0;
                rect.setFill(estClair ? COULEUR_CLAIR : COULEUR_FONCE);

                // Surbrillance (sélection ou dernier coup)
                Rectangle highlight = new Rectangle(TAILLE_CASE, TAILLE_CASE);
                highlight.setFill(Color.TRANSPARENT);

                if (selection != null && selection.equals(caseCourante)) {
                    highlight.setFill(COULEUR_SELECTION);
                } else if (dernierCoup != null
                        && (dernierCoup.depart().equals(caseCourante) || dernierCoup.arrivee().equals(caseCourante))) {
                    highlight.setFill(COULEUR_DERNIER_COUP);
                }

                // Représentation graphique de la pièce (légèrement réduite pour l'esthétique)
                Node pieceNode = ressources.getRepresentation(piece, TAILLE_CASE * 0.85);
                if (pieceNode != null) {
                    pieceNode.setMouseTransparent(true);
                    // Ombre portée pour l'effet visuel des pièces
                    javafx.scene.effect.DropShadow ombre = new javafx.scene.effect.DropShadow();
                    ombre.setRadius(4.0);
                    ombre.setOffsetX(2.0);
                    ombre.setOffsetY(3.0);
                    ombre.setColor(Color.color(0, 0, 0, 0.5));
                    pieceNode.setEffect(ombre);
                }

                StackPane stack = new StackPane(rect, highlight);
                if (pieceNode != null) {
                    stack.getChildren().add(pieceNode);
                }

                // Afficher un point pour chaque coup possible depuis la case sélectionnée
                if (coupsPossibles != null) {
                    for (Coup coup : coupsPossibles) {
                        if (coup.arrivee().equals(caseCourante)) {
                            javafx.scene.shape.Circle cercle = new javafx.scene.shape.Circle(TAILLE_CASE / 6.0);
                            cercle.setFill(Color.rgb(0, 0, 0, 0.2));
                            cercle.setMouseTransparent(true);
                            stack.getChildren().add(cercle);
                            break;
                        }
                    }
                }

                // Clic sur la case
                stack.setOnMouseClicked(e -> controleur.traiterClic(caseCourante));

                // Effet de survol
                Rectangle surbrillanceSurvol = new Rectangle(TAILLE_CASE, TAILLE_CASE);
                surbrillanceSurvol.setFill(Color.rgb(255, 255, 255, 0.2));
                surbrillanceSurvol.setVisible(false);
                stack.getChildren().add(surbrillanceSurvol);

                stack.setOnMouseEntered(e -> surbrillanceSurvol.setVisible(true));
                stack.setOnMouseExited(e -> surbrillanceSurvol.setVisible(false));

                this.add(stack, colonneVue, ligneVue);

                // Coordonnées algébriques sur les bords du plateau
                if (colonneVue == 0) {
                    Text texteRangee = new Text("" + ligneDuModele);
                    texteRangee.getStyleClass().addAll("coord-text", estClair ? "coord-dark" : "coord-light");
                    StackPane.setAlignment(texteRangee, Pos.TOP_LEFT);
                    texteRangee.setTranslateX(2);
                    texteRangee.setTranslateY(2);
                    stack.getChildren().add(texteRangee);
                }
                if (ligneVue == 7) {
                    char lettreColonne = (char) ('a' + colonneVue);
                    Text texteColonne = new Text("" + lettreColonne);
                    texteColonne.getStyleClass().addAll("coord-text", estClair ? "coord-dark" : "coord-light");
                    StackPane.setAlignment(texteColonne, Pos.BOTTOM_RIGHT);
                    texteColonne.setTranslateX(-2);
                    texteColonne.setTranslateY(-2);
                    stack.getChildren().add(texteColonne);
                }
            }
        }
    }
}
