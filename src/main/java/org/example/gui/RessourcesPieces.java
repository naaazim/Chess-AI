package org.example.gui;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.example.chess.Piece;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire de ressources pour l'affichage des pièces.
 * <p>
 * Charge les images PNG depuis les ressources.
 * </p>
 */
public class RessourcesPieces {

    private static final Map<Piece, Image> IMAGES = new HashMap<>();

    static {
        // Chargement des images au démarrage de la classe (ou à la demande)
        chargerImages();
    }

    private static void chargerImages() {
        // Correspondance Piece -> Nom fichier
        // Ex: roi_blanc.png, roi_noir.png, etc.
        // Noms supposés basés sur le list_dir :
        // cavalier_blanc.png, cavalier_noir.png, fou_blanc.png, etc.

        chargerImage(Piece.ROI_BLANC, "roi_blanc.png");
        chargerImage(Piece.DAME_BLANCHE, "reine_blanche.png");
        chargerImage(Piece.TOUR_BLANC, "tour_blanche.png");
        chargerImage(Piece.FOU_BLANC, "fou_blanc.png");
        chargerImage(Piece.CAVALIER_BLANC, "cavalier_blanc.png");
        chargerImage(Piece.PION_BLANC, "pion_blanc.png");
        chargerImage(Piece.ROI_NOIR, "roi_noir.png");
        chargerImage(Piece.DAME_NOIRE, "reine_noire.png");
        chargerImage(Piece.TOUR_NOIR, "tour_noire.png");
        chargerImage(Piece.FOU_NOIR, "fou_noir.png");
        chargerImage(Piece.CAVALIER_NOIR, "cavalier_noir.png");
        chargerImage(Piece.PION_NOIR, "pion_noir.png");
    }

    private static void chargerImage(Piece piece, String nomFichier) {
        // Le dossier 'resources' est à la racine du classpath en standard Maven
        // Donc on charge "/nomFichier"
        String path = "/" + nomFichier;
        try (InputStream is = RessourcesPieces.class.getResourceAsStream(path)) {
            if (is != null) {
                IMAGES.put(piece, new Image(is));
            } else {
                System.err.println("Image introuvable : " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Node getRepresentation(Piece piece, double tailleCase) {
        if (piece == null)
            return null;

        Image img = IMAGES.get(piece);
        if (img == null) {
            // Fallback si image manquante
            return new javafx.scene.text.Text("?");
        }

        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(tailleCase);
        imageView.setFitHeight(tailleCase);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        return new StackPane(imageView);
    }
}
