package fr.moteurechecs.jeu;

import fr.moteurechecs.ia.OrchestrateurIA;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Plateau;
import fr.moteurechecs.ia.Niveau;

/**
 * Joueur IA : choisit un coup automatiquement via Minimax + Alpha-Bêta.
 */
public final class JoueurIA {

    private final Niveau niveau;

    public JoueurIA(Niveau niveau) {
        if (niveau == null) throw new IllegalArgumentException("niveau null");
        this.niveau = niveau;
    }

    public Coup choisirCoup(Plateau plateau) {
        return OrchestrateurIA.meilleurCoup(plateau, niveau);
    }

    public Niveau niveau() {
        return niveau;
    }
}
