package org.example.AI.evaluation;

import org.example.chess.Piece;
import org.example.chess.Plateau;

public final class GamePhase {
    private static final int POIDS_PHASE_DAME = 4;
    private static final int POIDS_PHASE_TOUR = 2;
    private static final int POIDS_PHASE_FOU = 1;
    private static final int POIDS_PHASE_CAVALIER = 1;

    private static final int PHASE_MAX = 24;

    private GamePhase() {
    }

    public static double calculerPhase(Plateau p) {
        int phasePoints = 0;

        int dames = EvaluationUtils.compter(p.bitboard(Piece.DAME_BLANCHE))
                + EvaluationUtils.compter(p.bitboard(Piece.DAME_NOIRE));
        int tours = EvaluationUtils.compter(p.bitboard(Piece.TOUR_BLANC))
                + EvaluationUtils.compter(p.bitboard(Piece.TOUR_NOIRE));
        int fous = EvaluationUtils.compter(p.bitboard(Piece.FOU_BLANC))
                + EvaluationUtils.compter(p.bitboard(Piece.FOU_NOIR));
        int cavaliers = EvaluationUtils.compter(p.bitboard(Piece.CAVALIER_BLANC))
                + EvaluationUtils.compter(p.bitboard(Piece.CAVALIER_NOIR));

        phasePoints += dames * POIDS_PHASE_DAME;
        phasePoints += tours * POIDS_PHASE_TOUR;
        phasePoints += fous * POIDS_PHASE_FOU;
        phasePoints += cavaliers * POIDS_PHASE_CAVALIER;

        if (phasePoints < 0)
            phasePoints = 0;
        if (phasePoints > PHASE_MAX)
            phasePoints = PHASE_MAX;

        return phasePoints / (double) PHASE_MAX;
    }
}
