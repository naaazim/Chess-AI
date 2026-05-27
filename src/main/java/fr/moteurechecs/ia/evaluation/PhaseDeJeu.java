package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

/**
 * Détecte la phase de jeu en cours à partir du matériel restant sur le plateau.
 *
 * Retourne une valeur entre 0.0 (finale) et 1.0 (ouverture) basée sur
 * le nombre pondéré de pièces majeures et mineures encore en jeu.
 */
public final class PhaseDeJeu {

    private static final int POIDS_PHASE_DAME     = 4;
    private static final int POIDS_PHASE_TOUR     = 2;
    private static final int POIDS_PHASE_FOU      = 1;
    private static final int POIDS_PHASE_CAVALIER = 1;

    /** Points totaux en début de partie (2 dames + 4 tours + 4 fous + 4 cavaliers). */
    private static final int PHASE_MAX = 24;

    private PhaseDeJeu() {
    }

    /**
     * Calcule la phase de jeu courante.
     *
     * @param plateau la position à analyser
     * @return valeur entre 0.0 (finale) et 1.0 (ouverture)
     */
    public static double calculerPhase(Plateau plateau) {
        int pointsPhase = compterPointsPhase(plateau);
        pointsPhase = Math.max(0, Math.min(PHASE_MAX, pointsPhase));
        return pointsPhase / (double) PHASE_MAX;
    }

    // calcul du score de phase selon le matériel restant
    private static int compterPointsPhase(Plateau plateau) {
        int dames = UtilitairesEvaluation.compter(plateau.bitboard(Piece.DAME_BLANCHE))
                  + UtilitairesEvaluation.compter(plateau.bitboard(Piece.DAME_NOIRE));
        int tours = UtilitairesEvaluation.compter(plateau.bitboard(Piece.TOUR_BLANC))
                  + UtilitairesEvaluation.compter(plateau.bitboard(Piece.TOUR_NOIRE));
        int fous = UtilitairesEvaluation.compter(plateau.bitboard(Piece.FOU_BLANC))
                 + UtilitairesEvaluation.compter(plateau.bitboard(Piece.FOU_NOIR));
        int cavaliers = UtilitairesEvaluation.compter(plateau.bitboard(Piece.CAVALIER_BLANC))
                      + UtilitairesEvaluation.compter(plateau.bitboard(Piece.CAVALIER_NOIR));

        return dames     * POIDS_PHASE_DAME
             + tours     * POIDS_PHASE_TOUR
             + fous      * POIDS_PHASE_FOU
             + cavaliers * POIDS_PHASE_CAVALIER;
    }
}
