package org.example.chess;

/**
 * <p>
 * Constantes et tables d'attaques pré-calculées.
 * </p>
 *
 * <p>
 * Pré-calcul = très important pour la performance, car l’IA appelle
 * ces fonctions des milliers de fois.
 * </p>
 */
public final class Masques {

    private Masques() {}

    /** Colonne A (a8, a7, ... a1) */
    public static final long FILE_A = 0x0101010101010101L;

    /** Colonne H (h8, h7, ... h1) */
    public static final long FILE_H = 0x8080808080808080L;

    private static final long[] ATTAQUES_ROI = new long[64];
    private static final long[] ATTAQUES_CAVALIER = new long[64];

    static {
        for (int i = 0; i < 64; i++) {
            ATTAQUES_ROI[i] = calculAttaquesRoi(i);
            ATTAQUES_CAVALIER[i] = calculAttaquesCavalier(i);
        }
    }

    /**
     * <p>Cases attaquées par un roi placé à l’indice donné.</p>
     *
     * @param indice 0..63
     * @return bitboard des cases attaquées
     */
    public static long attaquesRoi(int indice) {
        return ATTAQUES_ROI[indice];
    }

    /**
     * <p>Cases attaquées par un cavalier placé à l’indice donné.</p>
     *
     * @param indice 0..63
     * @return bitboard des cases attaquées
     */
    public static long attaquesCavalier(int indice) {
        return ATTAQUES_CAVALIER[indice];
    }

    private static long calculAttaquesRoi(int idx) {
        int r = idx / 8;
        int c = idx % 8;
        long a = 0L;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int rr = r + dr;
                int cc = c + dc;
                if (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
                    a |= 1L << (rr * 8 + cc);
                }
            }
        }
        return a;
    }

    private static long calculAttaquesCavalier(int idx) {
        int r = idx / 8;
        int c = idx % 8;

        int[][] deltas = {
                {+2, +1}, {+2, -1}, {-2, +1}, {-2, -1},
                {+1, +2}, {+1, -2}, {-1, +2}, {-1, -2}
        };

        long a = 0L;
        for (int[] d : deltas) {
            int rr = r + d[0];
            int cc = c + d[1];
            if (rr >= 0 && rr < 8 && cc >= 0 && cc < 8) {
                a |= 1L << (rr * 8 + cc);
            }
        }
        return a;
    }
}
