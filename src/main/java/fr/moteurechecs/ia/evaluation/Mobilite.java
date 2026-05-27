package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Plateau;

/**
 * Évalue la mobilité du joueur au trait.
 *
 * Un coup disponible supplémentaire vaut {@code BONUS_MOBILITE_PAR_COUP}
 * centipions. Le score est positif si les Blancs ont plus de mobilité,
 * négatif sinon.
 */
public final class Mobilite {

    private static final int BONUS_MOBILITE_PAR_COUP = 2;

    private Mobilite() {
    }

    /**
     * Calcule le score de mobilité du joueur au trait.
     *
     * @param plateau la position à évaluer
     * @return score en centipions (positif = avantage Blancs)
     */
    public static int scoreMobilite(Plateau plateau) {
        int nombreCoups = GenerateurCoups.genererLegaux(plateau).size();
        int bonus = nombreCoups * BONUS_MOBILITE_PAR_COUP;
        return (plateau.trait() == Couleur.BLANC) ? bonus : -bonus;
    }
}
