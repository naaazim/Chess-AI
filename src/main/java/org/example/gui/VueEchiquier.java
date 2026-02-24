package org.example.gui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import org.example.chess.Case;
import org.example.chess.Coup;
import org.example.chess.Piece;
import org.example.chess.Plateau;
import org.example.chess.Couleur;

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
        for (int lig = 0; lig < 8; lig++) {
            for (int col = 0; col < 8; col++) {
                // Case de fond
                Rectangle rect = new Rectangle(TAILLE_CASE, TAILLE_CASE);
                boolean estClair = (lig + col) % 2 == 0;
                rect.setFill(estClair ? COULEUR_CLAIR : COULEUR_FONCE);

                // Interaction
                final int l = lig;
                final int c = col;

                // StackPane pour empiler : Fond + Surbrillance + Pièce
                StackPane stack = new StackPane(rect);
                stack.setOnMouseClicked(e -> {
                    // Conversion vue (0,0 en haut gauche) -> modèle (Case)
                    // Case.depuisColonneLigne attend ligne 1..8 (1 en bas) et col 0..7
                    // Lig 0 vue = Ligne 8 modèle
                    int modelLigne = 8 - l;
                    int modelCol = c;
                    try {
                        Case caseCliquee = Case.depuisColonneLigne(modelCol, modelLigne);
                        controleur.traiterClic(caseCliquee);
                    } catch (IllegalArgumentException ex) {
                        // Click hors limite ou erreur
                    }
                });

                this.add(stack, col, lig);
            }
        }
    }

    public void rafraichir(Plateau plateau, Case selection, Coup dernierCoup) {
        // Pour simplifier l'implémentation sans gérer finement les enfants du GridPane
        // :
        // On parcourt les enfants (StackPanes) et on met à jour leur contenu.
        // L'ordre d'ajout dans le GridPane est (col, row).
        // Mais attention l'ordre dans getChildren() n'est pas garanti 100% stable si
        // modifs dynamiques.
        // Mieux vaut reconstruire ou utiliser un tableau 2D de références.

        // Recréons le contenu "pièce" et "surbrillance" par dessus les fonds
        // C'est un peu brute-force mais sûr.

        this.getChildren().clear();

        for (int lig = 0; lig < 8; lig++) {
            for (int col = 0; col < 8; col++) {
                // Modèle
                int modelLigne = 8 - lig;
                int modelCol = col;
                Case caseCourante = Case.depuisColonneLigne(modelCol, modelLigne);
                Piece p = plateau.pieceEn(caseCourante);

                // Fond
                Rectangle rect = new Rectangle(TAILLE_CASE, TAILLE_CASE);
                boolean estClair = (lig + col) % 2 == 0;
                rect.setFill(estClair ? COULEUR_CLAIR : COULEUR_FONCE);

                // Surbrillance
                Rectangle highlight = new Rectangle(TAILLE_CASE, TAILLE_CASE);
                highlight.setFill(Color.TRANSPARENT);

                if (selection != null && selection.equals(caseCourante)) {
                    highlight.setFill(COULEUR_SELECTION);
                } else if (dernierCoup != null
                        && (dernierCoup.depart().equals(caseCourante) || dernierCoup.arrivee().equals(caseCourante))) {
                    highlight.setFill(COULEUR_DERNIER_COUP);
                }

                // Pièce (Node)
                Node pieceNode = ressources.getRepresentation(p, TAILLE_CASE * 0.85); // Légèrement plus petite pour un
                                                                                      // look premium
                if (pieceNode != null) {
                    pieceNode.setMouseTransparent(true);
                    // Ajout ombre sur les pièces
                    javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
                    shadow.setRadius(4.0);
                    shadow.setOffsetX(2.0);
                    shadow.setOffsetY(3.0);
                    shadow.setColor(Color.color(0, 0, 0, 0.5));
                    pieceNode.setEffect(shadow);
                }

                StackPane stack = new StackPane(rect, highlight);
                if (pieceNode != null) {
                    stack.getChildren().add(pieceNode);
                }

                // Interaction
                stack.setOnMouseClicked(e -> controleur.traiterClic(caseCourante));

                // Hover effet
                Rectangle hoverHighlight = new Rectangle(TAILLE_CASE, TAILLE_CASE);
                hoverHighlight.setFill(Color.rgb(255, 255, 255, 0.2));
                hoverHighlight.setVisible(false);
                stack.getChildren().add(hoverHighlight);

                stack.setOnMouseEntered(e -> hoverHighlight.setVisible(true));
                stack.setOnMouseExited(e -> hoverHighlight.setVisible(false));

                this.add(stack, col, lig);

                // Coordonnées sur les bords avec classes CSS
                if (col == 0) {
                    Text t = new Text("" + modelLigne);
                    t.getStyleClass().addAll("coord-text", estClair ? "coord-dark" : "coord-light");
                    StackPane.setAlignment(t, Pos.TOP_LEFT);
                    t.setTranslateX(2);
                    t.setTranslateY(2);
                    stack.getChildren().add(t);
                }
                if (lig == 7) {
                    char cNom = (char) ('a' + col);
                    Text t = new Text("" + cNom);
                    t.getStyleClass().addAll("coord-text", estClair ? "coord-dark" : "coord-light");
                    StackPane.setAlignment(t, Pos.BOTTOM_RIGHT);
                    t.setTranslateX(-2);
                    t.setTranslateY(-2);
                    stack.getChildren().add(t);
                }
            }
        }
    }
}
