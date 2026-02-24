package org.example.AI;

/**
 * <p>
 * 3 niveaux de difficulté (profondeur de recherche de base).
 * </p>
 */
public enum Niveau {
    FACILE(3),
    MOYEN(5),
    DIFFICILE(7);

    private final int profondeur;

    Niveau(int profondeur) {
        this.profondeur = profondeur;
    }

    public int profondeur() {
        return profondeur;
    }
}
