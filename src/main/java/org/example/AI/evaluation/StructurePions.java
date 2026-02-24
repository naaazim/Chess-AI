package org.example.AI.evaluation;

import org.example.chess.Piece;
import org.example.chess.Plateau;

public final class StructurePions {
    private static final int BONUS_PION_AVANCE = 6;

    private StructurePions() {
    }

    public static int scorePionsFinale(Plateau p) {
        long wp = p.bitboard(Piece.PION_BLANC);
        long bp = p.bitboard(Piece.PION_NOIR);

        int bonusBlanc = 0;
        int bonusNoir = 0;

        long tmp = wp;
        while (tmp != 0) {
            int idx = Long.numberOfTrailingZeros(tmp);
            tmp &= (tmp - 1);

            int ligne = idx / 8;
            int avance = (7 - ligne);
            bonusBlanc += avance * BONUS_PION_AVANCE;
        }

        tmp = bp;
        while (tmp != 0) {
            int idx = Long.numberOfTrailingZeros(tmp);
            tmp &= (tmp - 1);

            int ligne = idx / 8;
            int avance = ligne;
            bonusNoir += avance * BONUS_PION_AVANCE;
        }

        return bonusBlanc - bonusNoir;
    }
}
