package org.example.chess;

/**
 * <p>
 * La classe {@code Case} représente une seule case de l’échiquier d’échecs.
 * </p>
 *
 * <p>
 * Un échiquier contient 64 cases organisées en :
 * </p>
 * <ul>
 *     <li>8 colonnes : de <b>a</b> à <b>h</b></li>
 *     <li>8 lignes : de <b>8</b> (en haut) à <b>1</b> (en bas)</li>
 * </ul>
 *
 * <p>
 * Chaque case est représentée par un indice unique entre {@code 0} et {@code 63} :
 * </p>
 * <ul>
 *     <li>{@code 0} correspond à la case <b>a8</b></li>
 *     <li>{@code 7} correspond à la case <b>h8</b></li>
 *     <li>{@code 56} correspond à la case <b>a1</b></li>
 *     <li>{@code 63} correspond à la case <b>h1</b></li>
 * </ul>
 *
 * <p>
 * Cette classe est <b>immuable</b> et utilise un <b>cache</b> :
 * il n’existe qu’une seule instance par case.
 * </p>
 *
 * <p>
 * Cela permet d’éviter de créer des milliers d’objets pendant la recherche
 * de coups de l’IA, ce qui est essentiel à cause du <b>grand facteur de branchement</b>.
 * </p>
 */
public final class Case {

    /** Indice minimal d’une case */
    public static final int INDICE_MIN = 0;

    /** Indice maximal d’une case */
    public static final int INDICE_MAX = 63;

    /** Nombre de colonnes de l’échiquier */
    public static final int NB_COLONNES = 8;

    /** Nombre de lignes de l’échiquier */
    public static final int NB_LIGNES = 8;

    /**
     * Cache contenant les 64 cases de l’échiquier.
     * <p>
     * Ce cache garantit qu’il n’existe qu’un seul objet {@code Case}
     * par indice.
     * </p>
     */
    private static final Case[] CACHE = new Case[64];

    static {
        for (int i = 0; i < 64; i++) {
            CACHE[i] = new Case(i);
        }
    }

    /**
     * Indice de la case (entre 0 et 63).
     */
    private final int indice;

    /**
     * <p>
     * Constructeur privé.
     * </p>
     *
     * <p>
     * Il est volontairement inaccessible depuis l’extérieur afin de forcer
     * l’utilisation du cache et d’éviter des créations inutiles d’objets.
     * </p>
     *
     * @param indice indice de la case
     */
    private Case(int indice) {
        this.indice = indice;
    }

    /**
     * Retourne la case correspondant à l’indice donné.
     *
     * @param indice valeur entre 0 et 63
     * @return la case correspondante
     * @throws IllegalArgumentException si l’indice est invalide
     */
    public static Case depuisIndice(int indice) {
        if (indice < INDICE_MIN || indice > INDICE_MAX) {
            throw new IllegalArgumentException("Indice invalide : " + indice);
        }
        return CACHE[indice];
    }

    /**
     * Crée une case à partir d’une colonne et d’une ligne.
     *
     * <p>
     * Exemple :
     * </p>
     * <ul>
     *     <li>colonne = 4, ligne = 4 → e4</li>
     * </ul>
     *
     * @param colonne colonne entre 0 (a) et 7 (h)
     * @param ligne ligne entre 1 et 8
     * @return la case correspondante
     */
    public static Case depuisColonneLigne(int colonne, int ligne) {
        if (colonne < 0 || colonne >= NB_COLONNES || ligne < 1 || ligne > NB_LIGNES) {
            throw new IllegalArgumentException("Coordonnées invalides");
        }
        int indice = (8 - ligne) * 8 + colonne;
        return depuisIndice(indice);
    }

    /**
     * Crée une case à partir d’une notation algébrique.
     *
     * <p>
     * Exemple :
     * </p>
     * <ul>
     *     <li>"e4"</li>
     *     <li>"a8"</li>
     * </ul>
     *
     * @param coord notation algébrique
     * @return la case correspondante
     */
    public static Case depuisAlgebrique(String coord) {
        if (coord == null || coord.length() != 2) {
            throw new IllegalArgumentException("Notation algébrique invalide");
        }
        int colonne = coord.charAt(0) - 'a';
        int ligne = coord.charAt(1) - '0';
        return depuisColonneLigne(colonne, ligne);
    }

    /**
     * Retourne l’indice de la case.
     *
     * @return indice entre 0 et 63
     */
    public int indice() {
        return indice;
    }

    /**
     * Retourne la colonne de la case.
     *
     * @return colonne entre 0 (a) et 7 (h)
     */
    public int colonne() {
        return indice % 8;
    }

    /**
     * Retourne la ligne de la case.
     *
     * @return ligne entre 1 et 8
     */
    public int ligne() {
        return 8 - (indice / 8);
    }

    /**
     * Retourne la notation algébrique de la case.
     *
     * @return chaîne comme "e4"
     */
    public String versAlgebrique() {
        char colonne = (char) ('a' + colonne());
        char ligne = (char) ('0' + ligne());
        return "" + colonne + ligne;
    }

    /**
     * Retourne le bitboard correspondant à cette case.
     *
     * <p>
     * Le bit retourné contient un seul bit à 1 à la position
     * correspondant à l’indice de la case.
     * </p>
     *
     * @return bitboard de la case
     */
    public long bit() {
        return 1L << indice;
    }

    @Override
    public String toString() {
        return versAlgebrique();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Case)) return false;
        Case autre = (Case) obj;
        return this.indice == autre.indice;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(indice);
    }
}
