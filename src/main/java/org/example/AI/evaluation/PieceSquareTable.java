package org.example.AI.evaluation;

import org.example.chess.Piece;
import org.example.chess.Plateau;

public final class PieceSquareTable {

    private static final int[] PST_PION = {
            0, 0, 0, 0, 0, 0, 0, 0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
            5, 5, 10, 25, 25, 10, 5, 5,
            0, 0, 0, 20, 20, 0, 0, 0,
            5, -5, -10, 0, 0, -10, -5, 5,
            5, 10, 10, -20, -20, 10, 10, 5,
            0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] PST_CAVALIER = {
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 0, 0, 0, -20, -40,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
    };

    private static final int[] PST_FOU = {
            -20, -10, -10, -10, -10, -10, -10, -20,
            -10, 0, 0, 0, 0, 0, 0, -10,
            -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 5, 5, 10, 10, 5, 5, -10,
            -10, 0, 10, 10, 10, 10, 0, -10,
            -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 5, 0, 0, 0, 0, 5, -10,
            -20, -10, -10, -10, -10, -10, -10, -20
    };

    private static final int[] PST_ROI_MILIEU = {
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30,
            -20, -30, -30, -40, -40, -30, -30, -20,
            -10, -20, -20, -20, -20, -20, -20, -10,
            20, 20, 0, 0, 0, 0, 20, 20,
            20, 30, 10, 0, 0, 10, 30, 20
    };

    private static final int[] PST_ROI_FINALE = {
            -50, -40, -30, -20, -20, -30, -40, -50,
            -30, -20, -10, 0, 0, -10, -20, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 30, 40, 40, 30, -10, -30,
            -30, -10, 20, 30, 30, 20, -10, -30,
            -30, -30, 0, 0, 0, 0, -30, -30,
            -50, -30, -30, -30, -30, -30, -30, -50
    };

    private PieceSquareTable() {
    }

    private static int evaluerPST(long bitboard, int[] pst, boolean isBlanc) {
        int score = 0;
        long tmp = bitboard;
        while (tmp != 0) {
            int idx = Long.numberOfTrailingZeros(tmp);
            tmp &= (tmp - 1);
            int sq = isBlanc ? idx : (idx ^ 56);
            score += pst[sq];
        }
        return score;
    }

    public static int scorePositionnelPieceSquare(Plateau p, double phase) {
        int score = 0;

        score += evaluerPST(p.bitboard(Piece.PION_BLANC), PST_PION, true);
        score -= evaluerPST(p.bitboard(Piece.PION_NOIR), PST_PION, false);

        score += evaluerPST(p.bitboard(Piece.CAVALIER_BLANC), PST_CAVALIER, true);
        score -= evaluerPST(p.bitboard(Piece.CAVALIER_NOIR), PST_CAVALIER, false);

        score += evaluerPST(p.bitboard(Piece.FOU_BLANC), PST_FOU, true);
        score -= evaluerPST(p.bitboard(Piece.FOU_NOIR), PST_FOU, false);

        int roiBlancMilieu = evaluerPST(p.bitboard(Piece.ROI_BLANC), PST_ROI_MILIEU, true);
        int roiBlancFinale = evaluerPST(p.bitboard(Piece.ROI_BLANC), PST_ROI_FINALE, true);
        score += (int) (roiBlancMilieu * phase + roiBlancFinale * (1.0 - phase));

        int roiNoirMilieu = evaluerPST(p.bitboard(Piece.ROI_NOIR), PST_ROI_MILIEU, false);
        int roiNoirFinale = evaluerPST(p.bitboard(Piece.ROI_NOIR), PST_ROI_FINALE, false);
        score -= (int) (roiNoirMilieu * phase + roiNoirFinale * (1.0 - phase));

        return score;
    }
}
