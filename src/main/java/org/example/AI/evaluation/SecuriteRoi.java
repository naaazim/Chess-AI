package org.example.AI.evaluation;

import org.example.chess.Case;
import org.example.chess.Piece;
import org.example.chess.Plateau;

public final class SecuriteRoi {
    private static final int BONUS_ROI_CENTRE_FINALE = 15;

    private SecuriteRoi() {
    }

    public static int scoreRoiActifFinale(Plateau p) {
        Case roiBlanc = EvaluationUtils.trouverCaseUnique(p.bitboard(Piece.ROI_BLANC));
        Case roiNoir = EvaluationUtils.trouverCaseUnique(p.bitboard(Piece.ROI_NOIR));
        if (roiBlanc == null || roiNoir == null)
            return 0;

        int distBlanc = distanceCentre(roiBlanc);
        int distNoir = distanceCentre(roiNoir);

        int bonusBlanc = (6 - distBlanc) * BONUS_ROI_CENTRE_FINALE;
        int bonusNoir = (6 - distNoir) * BONUS_ROI_CENTRE_FINALE;

        return bonusBlanc - bonusNoir;
    }

    private static int distanceCentre(Case c) {
        int col = c.colonne();
        int lig = c.ligne();

        int[][] centres = new int[][] {
                { 3, 3 }, { 4, 3 }, { 3, 4 }, { 4, 4 }
        };

        int best = Integer.MAX_VALUE;
        for (int[] centre : centres) {
            int dc = Math.abs(col - centre[0]);
            int dl = Math.abs(lig - centre[1]);
            best = Math.min(best, dc + dl);
        }
        return best;
    }
}
