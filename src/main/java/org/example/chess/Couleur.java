package org.example.chess;

/**
 * <p>Représente la couleur du joueur.</p>
 */
public enum Couleur {
    BLANC,
    NOIR;

    /**
     * <p>Retourne la couleur opposée.</p>
     *
     * @return l'autre couleur
     */
    public Couleur inverse() {
        return this == BLANC ? NOIR : BLANC;
    }
}
