package org.example.AI.evaluation;

import org.example.AI.Evaluation;
import org.example.chess.Piece;
import org.example.chess.Plateau;

public final class Materiel {
    private Materiel() {
    }

    public static int scoreMateriel(Plateau p) {
        int blancs = 0;
        int noirs = 0;

        blancs += EvaluationUtils.compter(p.bitboard(Piece.PION_BLANC)) * Evaluation.VALEUR_PION;
        blancs += EvaluationUtils.compter(p.bitboard(Piece.CAVALIER_BLANC)) * Evaluation.VALEUR_CAVALIER;
        blancs += EvaluationUtils.compter(p.bitboard(Piece.FOU_BLANC)) * Evaluation.VALEUR_FOU;
        blancs += EvaluationUtils.compter(p.bitboard(Piece.TOUR_BLANC)) * Evaluation.VALEUR_TOUR;
        blancs += EvaluationUtils.compter(p.bitboard(Piece.DAME_BLANCHE)) * Evaluation.VALEUR_DAME;

        noirs += EvaluationUtils.compter(p.bitboard(Piece.PION_NOIR)) * Evaluation.VALEUR_PION;
        noirs += EvaluationUtils.compter(p.bitboard(Piece.CAVALIER_NOIR)) * Evaluation.VALEUR_CAVALIER;
        noirs += EvaluationUtils.compter(p.bitboard(Piece.FOU_NOIR)) * Evaluation.VALEUR_FOU;
        noirs += EvaluationUtils.compter(p.bitboard(Piece.TOUR_NOIR)) * Evaluation.VALEUR_TOUR;
        noirs += EvaluationUtils.compter(p.bitboard(Piece.DAME_NOIRE)) * Evaluation.VALEUR_DAME;

        return blancs - noirs;
    }
}
