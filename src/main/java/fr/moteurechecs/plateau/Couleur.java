package fr.moteurechecs.plateau;

/**
 *
 * Représente la couleur du joueur.
 */
public enum Couleur {
    BLANC,
    NOIR;

    /**
     *
     * Retourne la couleur opposée.
     *
     * @return l'autre couleur
     */
    public Couleur inverse() {
        return this == BLANC ? NOIR : BLANC;
    }
}
