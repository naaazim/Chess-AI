package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Case;

/**
 * Utilitaires partagés entre les composants d'évaluation.
 *
 * Fournit des raccourcis pour les opérations bitboard courantes :
 * comptage de bits et conversion bitboard → case.
 */
public final class UtilitairesEvaluation {

    private UtilitairesEvaluation() {
    }

    /**
     * Compte le nombre de bits à 1 dans le bitboard donné.
     *
     * @param bitboard le bitboard à analyser
     * @return nombre de pièces sur le bitboard
     */
    public static int compter(long bitboard) {
        return Long.bitCount(bitboard);
    }

    /**
     * Retourne la case correspondant à un bitboard contenant exactement un bit.
     *
     * @param bitboardUnique bitboard avec un seul bit à 1
     * @return la case correspondante, ou {@code null} si le bitboard est vide
     */
    public static Case trouverCaseUnique(long bitboardUnique) {
        if (bitboardUnique == 0)
            return null;
        int indice = Long.numberOfTrailingZeros(bitboardUnique);
        return Case.depuisIndice(indice);
    }
}
