package fr.moteurechecs.plateau;

/**
 * Attaques des pièces glissantes (tour, fou, dame).
 *
 * On “glisse” dans une direction jusqu’à tomber sur une pièce ou sortir du plateau.
 * C’est simple et correct. On optimisera plus tard si besoin.
 */
public final class Rayons {

    private Rayons() {}

    /**
     * Calcule les cases attaquées en diagonale depuis une case donnée.
     *
     * @param indice  indice de la pièce (0..63)
     * @param occupes bitboard des cases occupées (bloquants)
     * @return bitboard des cases attaquées en diagonale
     */
    public static long attaquesDiagonales(int indice, long occupes) {
        return attaquesDirection(indice, occupes, -9)
                | attaquesDirection(indice, occupes, -7)
                | attaquesDirection(indice, occupes, +7)
                | attaquesDirection(indice, occupes, +9);
    }

    /**
     * Calcule les cases attaquées en ligne droite (horizontales et verticales) depuis une case donnée.
     *
     * @param indice  indice de la pièce (0..63)
     * @param occupes bitboard des cases occupées (bloquants)
     * @return bitboard des cases attaquées en ligne droite
     */
    public static long attaquesHorizontalesEtVerticales(int indice, long occupes) {
        return attaquesDirection(indice, occupes, -8)
                | attaquesDirection(indice, occupes, +8)
                | attaquesDirection(indice, occupes, -1)
                | attaquesDirection(indice, occupes, +1);
    }

    private static long attaquesDirection(int indice, long occupes, int delta) {
        long attaques = 0L;
        int  curseur  = indice;

        while (true) {
            int suivant = curseur + delta;
            if (suivant < 0 || suivant >= 64) break;

            // Éviter les "wrap" gauche/droite sur les mouvements horizontaux et diagonaux
            int colonneActuelle = curseur  % 8;
            int colonneSuivante = suivant  % 8;

            boolean estMouvementLateral = (delta == -1 || delta == +1 || delta == -9 || delta == -7 || delta == +7 || delta == +9);
            if (estMouvementLateral && Math.abs(colonneSuivante - colonneActuelle) > 1) {
                break;
            }

            long masque = 1L << suivant;
            attaques |= masque;

            if ((occupes & masque) != 0L) break; // case bloquée par une pièce
            curseur = suivant;
        }

        return attaques;
    }
}
