package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.ia.Evaluation;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

/**
 * Calcule le score matériel brut d'une position.
 *
 * Le score est positif quand les Blancs ont l'avantage,
 * négatif quand les Noirs ont l'avantage.
 */
public final class Materiel {

    private Materiel() {
    }

    /**
     * Calcule la balance matérielle (Blancs − Noirs) en centipions.
     *
     * @param plateau la position à évaluer
     * @return différence de matériel (positif = avantage Blancs)
     */
    public static int scoreMateriel(Plateau plateau) {
        int scoreBlancs = compterMaterielBlanc(plateau);
        int scoreNoirs  = compterMaterielNoir(plateau);
        return scoreBlancs - scoreNoirs;
    }

    // somme du matériel blanc
    private static int compterMaterielBlanc(Plateau plateau) {
        return UtilitairesEvaluation.compter(plateau.bitboard(Piece.PION_BLANC))     * Evaluation.VALEUR_PION
             + UtilitairesEvaluation.compter(plateau.bitboard(Piece.CAVALIER_BLANC)) * Evaluation.VALEUR_CAVALIER
             + UtilitairesEvaluation.compter(plateau.bitboard(Piece.FOU_BLANC))      * Evaluation.VALEUR_FOU
             + UtilitairesEvaluation.compter(plateau.bitboard(Piece.TOUR_BLANC))     * Evaluation.VALEUR_TOUR
             + UtilitairesEvaluation.compter(plateau.bitboard(Piece.DAME_BLANCHE))   * Evaluation.VALEUR_DAME;
    }

    // somme du matériel noir
    private static int compterMaterielNoir(Plateau plateau) {
        return UtilitairesEvaluation.compter(plateau.bitboard(Piece.PION_NOIR))     * Evaluation.VALEUR_PION
             + UtilitairesEvaluation.compter(plateau.bitboard(Piece.CAVALIER_NOIR)) * Evaluation.VALEUR_CAVALIER
             + UtilitairesEvaluation.compter(plateau.bitboard(Piece.FOU_NOIR))      * Evaluation.VALEUR_FOU
             + UtilitairesEvaluation.compter(plateau.bitboard(Piece.TOUR_NOIRE))    * Evaluation.VALEUR_TOUR
             + UtilitairesEvaluation.compter(plateau.bitboard(Piece.DAME_NOIRE))    * Evaluation.VALEUR_DAME;
    }
}
