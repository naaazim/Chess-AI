package fr.moteurechecs.ia.recherche;

import fr.moteurechecs.ia.Evaluation;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

import java.util.List;

/**
 * Tri des coups pour améliorer l'efficacité de l'élagage alpha-bêta.
 *
 * Les captures avec gain de matériel (MVV-LVA) et les promotions
 * sont placées en premier ; les coups calmes suivent.
 */
public final class TrieurDeCoups {

    private TrieurDeCoups() {
    }

    /**
     * Trie la liste de coups en place, du plus prometteur au moins prometteur.
     *
     * @param coups   liste de coups à trier (modifiée en place)
     * @param plateau position courante (pour identifier la couleur adverse)
     */
    public static void trierCoups(List<Coup> coups, Plateau plateau) {
        coups.sort((c1, c2) -> {
            int note1 = evaluerCoupPourTri(c1, plateau);
            int note2 = evaluerCoupPourTri(c2, plateau);
            return Integer.compare(note2, note1);
        });
    }

    /**
     * Déplace le coup indiqué en première position dans la liste.
     * Utilisé pour placer le coup de la table de transposition en tête.
     *
     * @param coups                liste de coups
     * @param meilleurCoupPrecedent coup à promouvoir en première position
     */
    public static void placerEnPremier(List<Coup> coups, Coup meilleurCoupPrecedent) {
        if (meilleurCoupPrecedent == null) return;

        int indice = trouverIndiceCoup(coups, meilleurCoupPrecedent);
        if (indice > 0) {
            Coup coup = coups.remove(indice);
            coups.add(0, coup);
        }
    }

    // calcul du score heuristique d'un coup pour le tri (MVV-LVA + promotion)
    private static int evaluerCoupPourTri(Coup coup, Plateau plateau) {
        int note = 0;

        if (coup.estCapture()) {
            note += calculerGainCapture(coup, plateau);
        }
        if (coup.estPromotion()) {
            note += valeurPiece(coup.piecePromotion());
        }

        return note;
    }

    // score MVV-LVA : 10 × valeur victime − valeur attaquant
    private static int calculerGainCapture(Coup coup, Plateau plateau) {
        Piece victime    = coup.pieceCapturee();
        Piece attaquant  = coup.pieceDeplacee();

        if (victime == null && coup.estEnPassant()) {
            victime = (plateau.trait() == Couleur.BLANC) ? Piece.PION_NOIR : Piece.PION_BLANC;
        }

        return 10 * valeurPiece(victime) - valeurPiece(attaquant);
    }

    // valeur centipions d'une pièce (0 pour null ou roi)
    private static int valeurPiece(Piece piece) {
        if (piece == null) return 0;
        return switch (piece) {
            case PION_BLANC,     PION_NOIR     -> Evaluation.VALEUR_PION;
            case CAVALIER_BLANC, CAVALIER_NOIR -> Evaluation.VALEUR_CAVALIER;
            case FOU_BLANC,      FOU_NOIR      -> Evaluation.VALEUR_FOU;
            case TOUR_BLANC,     TOUR_NOIRE    -> Evaluation.VALEUR_TOUR;
            case DAME_BLANCHE,   DAME_NOIRE    -> Evaluation.VALEUR_DAME;
            case ROI_BLANC,      ROI_NOIR      -> 0;
        };
    }

    // recherche de l'indice d'un coup dans la liste par départ/arrivée/promotion
    private static int trouverIndiceCoup(List<Coup> coups, Coup cible) {
        for (int i = 0; i < coups.size(); i++) {
            Coup c = coups.get(i);
            if (c.depart().equals(cible.depart())
                    && c.arrivee().equals(cible.arrivee())
                    && c.piecePromotion() == cible.piecePromotion()) {
                return i;
            }
        }
        return -1;
    }
}
