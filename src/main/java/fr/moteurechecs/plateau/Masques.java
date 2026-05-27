package fr.moteurechecs.plateau;

/**
 * Constantes et tables d'attaques pré-calculées.
 *
 * Pré-calcul = très important pour la performance, car l’IA appelle
 * ces fonctions des milliers de fois.
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
     *
     * Cases attaquées par un roi placé à l’indice donné.
     *
     * @param indice 0..63
     * @return bitboard des cases attaquées
     */
    public static long attaquesRoi(int indice) {
        return ATTAQUES_ROI[indice];
    }

    /**
     *
     * Cases attaquées par un cavalier placé à l’indice donné.
     *
     * @param indice 0..63
     * @return bitboard des cases attaquées
     */
    public static long attaquesCavalier(int indice) {
        return ATTAQUES_CAVALIER[indice];
    }

    private static long calculAttaquesRoi(int indice) {
        int rangee  = indice / 8;
        int colonne = indice % 8;
        long attaques = 0L;

        // Parcourir les 8 directions autour du roi
        for (int deltaRangee = -1; deltaRangee <= 1; deltaRangee++) {
            for (int deltaColonne = -1; deltaColonne <= 1; deltaColonne++) {
                if (deltaRangee == 0 && deltaColonne == 0) continue;
                int rangeeVoisine  = rangee  + deltaRangee;
                int colonneVoisine = colonne + deltaColonne;
                if (rangeeVoisine >= 0 && rangeeVoisine < 8 && colonneVoisine >= 0 && colonneVoisine < 8) {
                    attaques |= 1L << (rangeeVoisine * 8 + colonneVoisine);
                }
            }
        }
        return attaques;
    }

    private static long calculAttaquesCavalier(int indice) {
        int rangee  = indice / 8;
        int colonne = indice % 8;

        // 8 sauts en L possibles pour un cavalier
        int[][] sauts = {
                {+2, +1}, {+2, -1}, {-2, +1}, {-2, -1},
                {+1, +2}, {+1, -2}, {-1, +2}, {-1, -2}
        };

        long attaques = 0L;
        for (int[] saut : sauts) {
            int rangeeVoisine  = rangee  + saut[0];
            int colonneVoisine = colonne + saut[1];
            if (rangeeVoisine >= 0 && rangeeVoisine < 8 && colonneVoisine >= 0 && colonneVoisine < 8) {
                attaques |= 1L << (rangeeVoisine * 8 + colonneVoisine);
            }
        }
        return attaques;
    }
}
