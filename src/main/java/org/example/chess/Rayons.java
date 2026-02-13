package org.example.chess;

/**
 * <p>
 * Attaques des pièces glissantes (tour, fou, dame).
 * </p>
 *
 * <p>
 * On “glisse” dans une direction jusqu’à tomber sur une pièce ou sortir du plateau.
 * C’est simple et correct. On optimisera plus tard si besoin.
 * </p>
 */
public final class Rayons {

    private Rayons() {}

    public static long attaquesDiagonales(int idx, long occupes) {
        return attaquesDirection(idx, occupes, -9)
                | attaquesDirection(idx, occupes, -7)
                | attaquesDirection(idx, occupes, +7)
                | attaquesDirection(idx, occupes, +9);
    }

    public static long attaquesHorizontalesEtVerticales(int idx, long occupes) {
        return attaquesDirection(idx, occupes, -8)
                | attaquesDirection(idx, occupes, +8)
                | attaquesDirection(idx, occupes, -1)
                | attaquesDirection(idx, occupes, +1);
    }

    private static long attaquesDirection(int idx, long occupes, int delta) {
        long attaques = 0L;
        int cur = idx;

        while (true) {
            int next = cur + delta;
            if (next < 0 || next >= 64) break;

            // éviter les "wrap" gauche/droite sur les mouvements horizontaux/diagonaux
            int curCol = cur % 8;
            int nextCol = next % 8;

            boolean mouvementHorizontalOuDiagonal = (delta == -1 || delta == +1 || delta == -9 || delta == -7 || delta == +7 || delta == +9);
            if (mouvementHorizontalOuDiagonal && Math.abs(nextCol - curCol) > 1) {
                break;
            }

            long b = 1L << next;
            attaques |= b;

            if ((occupes & b) != 0L) break; // bloqué
            cur = next;
        }

        return attaques;
    }
}
