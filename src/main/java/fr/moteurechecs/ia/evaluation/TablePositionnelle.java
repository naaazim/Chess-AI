package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

/**
 * Tables de bonus positionnel (Piece-Square Tables).
 *
 * Chaque pièce reçoit un bonus ou une pénalité selon sa case,
 * favorisant les cases centrales et actives. Le roi utilise deux tables
 * interpolées selon la phase de jeu : milieu de partie (sécurité) et
 * finale (centralisation).
 */
public final class TablePositionnelle {

    // ===== Tables PST (du point de vue des Blancs, indices 0=a8..63=h1) =====

    private static final int[] PST_PION = {
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] PST_CAVALIER = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] PST_FOU = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] PST_ROI_MILIEU = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };

    private static final int[] PST_ROI_FINALE = {
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    };

    private TablePositionnelle() {
    }

    /**
     * Calcule le score positionnel total interpolé selon la phase.
     *
     * @param plateau la position à évaluer
     * @param phase   valeur entre 0.0 (finale) et 1.0 (ouverture)
     * @return score positionnel en centipions
     */
    public static int scorePositionnelPieceSquare(Plateau plateau, double phase) {
        int score = 0;
        score += evaluerPST(plateau.bitboard(Piece.PION_BLANC),    PST_PION,     true);
        score -= evaluerPST(plateau.bitboard(Piece.PION_NOIR),     PST_PION,     false);
        score += evaluerPST(plateau.bitboard(Piece.CAVALIER_BLANC),PST_CAVALIER, true);
        score -= evaluerPST(plateau.bitboard(Piece.CAVALIER_NOIR), PST_CAVALIER, false);
        score += evaluerPST(plateau.bitboard(Piece.FOU_BLANC),     PST_FOU,      true);
        score -= evaluerPST(plateau.bitboard(Piece.FOU_NOIR),      PST_FOU,      false);
        score += evaluerScoreRoiInterpolee(plateau, phase);
        return score;
    }

    // interpolation du score du roi entre milieu de jeu et finale
    private static int evaluerScoreRoiInterpolee(Plateau plateau, double phase) {
        int roiBlancMilieu = evaluerPST(plateau.bitboard(Piece.ROI_BLANC), PST_ROI_MILIEU, true);
        int roiBlancFinale = evaluerPST(plateau.bitboard(Piece.ROI_BLANC), PST_ROI_FINALE, true);
        int roiNoirMilieu  = evaluerPST(plateau.bitboard(Piece.ROI_NOIR),  PST_ROI_MILIEU, false);
        int roiNoirFinale  = evaluerPST(plateau.bitboard(Piece.ROI_NOIR),  PST_ROI_FINALE, false);

        int scoreBlanc = (int)(roiBlancMilieu * phase + roiBlancFinale * (1.0 - phase));
        int scoreNoir  = (int)(roiNoirMilieu  * phase + roiNoirFinale  * (1.0 - phase));
        return scoreBlanc - scoreNoir;
    }

    // somme des bonus PST pour toutes les pièces d'un bitboard
    private static int evaluerPST(long bitboard, int[] table, boolean estBlanc) {
        int score = 0;
        long curseur = bitboard;
        while (curseur != 0) {
            int indice = Long.numberOfTrailingZeros(curseur);
            curseur &= (curseur - 1);
            int caseTable = estBlanc ? indice : (indice ^ 56);
            score += table[caseTable];
        }
        return score;
    }
}
