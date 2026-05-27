package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

/**
 * Évalue la structure de pions en phase de finale.
 *
 * Les pions avancés vers leur promotion reçoivent un bonus proportionnel
 * à leur rang d'avancement.
 */
public final class StructurePions {

    private static final int BONUS_PION_AVANCE = 6;

    private StructurePions() {
    }

    /**
     * Calcule le score de structure de pions pour la finale.
     *
     * @param plateau la position à évaluer
     * @return score en centipions (positif = avantage Blancs)
     */
    public static int scorePionsFinale(Plateau plateau) {
        int bonusBlanc = calculerBonusPionsAvances(plateau.bitboard(Piece.PION_BLANC), true);
        int bonusNoir  = calculerBonusPionsAvances(plateau.bitboard(Piece.PION_NOIR),  false);
        return bonusBlanc - bonusNoir;
    }

    // calcul du bonus pour les pions avancés d'une couleur
    private static int calculerBonusPionsAvances(long bitboardPions, boolean estBlanc) {
        int bonus = 0;
        long curseur = bitboardPions;
        while (curseur != 0) {
            int indice = Long.numberOfTrailingZeros(curseur);
            curseur &= (curseur - 1);
            int rangee = indice / 8;
            int avance = estBlanc ? (7 - rangee) : rangee;
            bonus += avance * BONUS_PION_AVANCE;
        }
        return bonus;
    }
}
