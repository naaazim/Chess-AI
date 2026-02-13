package org.example.jeu;

import org.example.AI.RechercheMinimaxAlphaBeta;
import org.example.chess.Coup;
import org.example.chess.Plateau;
import org.example.AI.Niveau;

/**
 * <p>
 * Joueur IA : choisit un coup automatiquement via Minimax + Alpha-Bêta.
 * </p>
 */
public final class JoueurIA {

    private final Niveau niveau;

    public JoueurIA(Niveau niveau) {
        if (niveau == null) throw new IllegalArgumentException("niveau null");
        this.niveau = niveau;
    }

    public Coup choisirCoup(Plateau plateau) {
        return RechercheMinimaxAlphaBeta.meilleurCoup(plateau, niveau);
    }

    public Niveau niveau() {
        return niveau;
    }
}
