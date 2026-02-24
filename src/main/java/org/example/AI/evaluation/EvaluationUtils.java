package org.example.AI.evaluation;

import org.example.chess.Case;

public final class EvaluationUtils {
    private EvaluationUtils() {
    }

    public static int compter(long bitboard) {
        return Long.bitCount(bitboard);
    }

    public static Case trouverCaseUnique(long bitboardUnique) {
        if (bitboardUnique == 0)
            return null;
        int idx = Long.numberOfTrailingZeros(bitboardUnique);
        return Case.depuisIndice(idx);
    }
}
